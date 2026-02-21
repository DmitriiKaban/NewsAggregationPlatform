import time
import json
import os
import requests
import feedparser
import schedule
import redis
import psycopg2
from bs4 import BeautifulSoup
from kafka import KafkaProducer
from pathlib import Path
from dotenv import load_dotenv
from datetime import datetime, timedelta, timezone
from dateutil import parser as date_parser
from telethon.sync import TelegramClient
import concurrent.futures

# --- 1. CONFIGURATION ---
current_dir = Path(__file__).resolve().parent
env_path = current_dir.parent / "common" / ".env"
load_dotenv(dotenv_path=env_path)

KAFKA_BROKER = os.getenv("KAFKA_BROKER", "localhost:9092")
TOPIC_NAME = "news.raw"
REDIS_HOST = "localhost"
REDIS_PORT = 6379

API_ID = os.getenv("TELEGRAM_API_ID")
API_HASH = os.getenv("TELEGRAM_API_HASH")
SESSION_NAME = "newsbot_session"

# Threading Config
MAX_RSS_THREADS = 5
MAX_ARTICLE_THREADS = 3

HEADERS = {
    "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36",
    "Accept": "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8",
}

print(f"🚀 Connecting to Kafka: {KAFKA_BROKER}")
print(f"💾 Connecting to Redis: {REDIS_HOST}:{REDIS_PORT}")

producer = KafkaProducer(
    bootstrap_servers=[KAFKA_BROKER],
    value_serializer=lambda x: json.dumps(x).encode("utf-8"),
)

redis_client = redis.Redis(host=REDIS_HOST, port=REDIS_PORT, decode_responses=True)


def get_sources_from_db():
    conn = None
    try:
        conn = psycopg2.connect(
            dbname="newsbot_db",
            user="newsbot_admin",
            password="newsbot_pass",
            host="localhost",
            port="5433",
        )
        cur = conn.cursor()
        cur.execute("SELECT name, url, type FROM sources WHERE is_active = true")
        rows = cur.fetchall()
        db_sources = []
        for row in rows:
            db_sources.append({
                "name": row[0],
                "url": row[1],
                "type": "telegram" if "t.me" in row[1] else "rss",
                "channel": row[1].split("/")[-1] if "t.me" in row[1] else None,
                "filter_hours": 24,
            })
        return db_sources
    except Exception as e:
        print(f"❌ Database error: {e}")
        return []
    finally:
        if conn:
            conn.close()

def clean_text(text):
    if not text: return ""
    replacements = {"ă": "a", "â": "a", "î": "i", "ș": "s", "ț": "t", "Ă": "A", "Â": "A", "Î": "I", "Ș": "S", "Ț": "T", "„": '"', "”": '"', "’": "'"}
    for k, v in replacements.items(): text = text.replace(k, v)
    return text

def is_processed(link): return redis_client.exists(link)
def mark_processed(link): redis_client.set(link, "1", ex=86400)

def is_recent(date_obj, hours):
    if date_obj.tzinfo is None:
        date_obj = date_obj.replace(tzinfo=timezone.utc)
    cutoff = datetime.now(timezone.utc) - timedelta(hours=hours)
    return date_obj > cutoff

def extract_full_text(url):
    try:
        resp = requests.get(url, headers=HEADERS, timeout=5)
        soup = BeautifulSoup(resp.content, "html.parser")
        for script in soup(["script", "style", "nav", "footer", "header", "aside"]):
            script.extract()
        paragraphs = soup.find_all("p")
        full_text = "\n\n".join([p.get_text().strip() for p in paragraphs if len(p.get_text()) > 20])
        return full_text if len(full_text) > 50 else ""
    except Exception:
        return ""

def send_to_kafka(source, title, link, summary, full_text, date_obj):
    content = full_text if len(full_text) > len(summary) else summary
    if not content: content = title

    article = {
        "title": clean_text(title),
        "link": link,
        "published": date_obj.isoformat(),
        "summary": clean_text(summary),
        "content": clean_text(content),
        "source": source,
    }
    producer.send(TOPIC_NAME, value=article)
    mark_processed(link)
    print(f"      ✅ [SENT] {article['title'][:50]}...")


def process_single_entry(entry, source_name):
    """Processes a single RSS entry. Used by the inner thread pool."""
    link = entry.link
    if is_processed(link):
        return 0

    try:
        pub_date = date_parser.parse(entry.published)
    except:
        pub_date = datetime.now(timezone.utc)

    if not is_recent(pub_date, 24):
        mark_processed(link)
        return 0

    full_text = extract_full_text(link)
    send_to_kafka(source_name, entry.title, link, entry.get("summary", ""), full_text, pub_date)
    return 1

def scrape_rss(source):
    print(f"   📡 [START] RSS: {source['name']}")
    try:
        resp = requests.get(source["url"], headers=HEADERS, timeout=10)
        feed = feedparser.parse(resp.content)
        if not feed.entries:
            return 0

        count = 0
        # INNER THREAD POOL: Fetch multiple articles from this feed concurrently
        with concurrent.futures.ThreadPoolExecutor(max_workers=MAX_ARTICLE_THREADS) as executor:
            futures = [executor.submit(process_single_entry, entry, source["name"]) for entry in feed.entries]
            for future in concurrent.futures.as_completed(futures):
                count += future.result()

        print(f"   ✅ [DONE] RSS: {source['name']} ({count} new)")
        return count
    except Exception as e:
        print(f"      ❌ RSS Error ({source['name']}): {e}")
        return 0


def scrape_telegram(client, source):
    """Scrapes a single Telegram channel."""
    print(f"   ✈️ [START] Telegram: {source['name']}")
    count = 0
    try:
        entity = client.get_entity(source["channel"])
        posts = client.get_messages(entity, limit=20)
        for post in posts:
            if not post.message: continue
            link = f"https://t.me/{source['channel']}/{post.id}"

            if is_processed(link): continue

            pub_date = post.date
            if not is_recent(pub_date, source["filter_hours"]):
                mark_processed(link)
                continue

            full_text = post.message
            title = full_text.split("\n")[0][:200] if len(full_text) > 10 else full_text[:60] + "..."

            send_to_kafka(source["name"], title, link, full_text, full_text, pub_date)
            count += 1

        print(f"   ✅ [DONE] Telegram: {source['name']} ({count} new)")
    except Exception as e:
        print(f"      ❌ TG Error ({source['name']}): {e}")
    return count


def job():
    start_time = time.time()
    print(f"\n🔄 Cycle Started: {datetime.now().strftime('%H:%M:%S')}")

    db_sources = get_sources_from_db()
    rss_sources = [s for s in db_sources if s["type"] == "rss"]
    tg_sources = [s for s in db_sources if s["type"] == "telegram"]

    total_rss = 0
    total_tg = 0

    # OUTER THREAD POOL: Scrape multiple RSS feeds concurrently
    if rss_sources:
        with concurrent.futures.ThreadPoolExecutor(max_workers=MAX_RSS_THREADS) as executor:
            futures = [executor.submit(scrape_rss, source) for source in rss_sources]
            for future in concurrent.futures.as_completed(futures):
                total_rss += future.result()

    # Telegram (Sequential to avoid API bans/FloodWait errors)
    if tg_sources:
        if not API_ID or not API_HASH:
            print("      ❌ Skipped Telegram: Missing Credentials")
        else:
            try:
                with TelegramClient(SESSION_NAME, API_ID, API_HASH) as client:
                    for source in tg_sources:
                        total_tg += scrape_telegram(client, source)
            except Exception as e:
                print(f"      ❌ Telegram Connection Failed: {e}")

    elapsed = time.time() - start_time
    total = total_rss + total_tg
    print(f"💤 Cycle Complete. Sent {total} articles in {elapsed:.2f} seconds.")


schedule.every(10).minutes.do(job)

if __name__ == "__main__":
    job()
    while True:
        schedule.run_pending()
        time.sleep(1)