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
from telethon.errors import SessionPasswordNeededError


# --- 1. CONFIGURATION ---
current_dir = Path(__file__).resolve().parent
env_path = current_dir.parent / "common" / ".env"
load_dotenv(dotenv_path=env_path)

KAFKA_BROKER = os.getenv("KAFKA_BROKER", "localhost:9092")
TOPIC_NAME = "news.raw"
REDIS_HOST = "localhost"
REDIS_PORT = 6379

# Telegram Credentials
API_ID = os.getenv("TELEGRAM_API_ID")
API_HASH = os.getenv("TELEGRAM_API_HASH")
SESSION_NAME = "newsbot_session"

# Mock Browser Headers
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
        db_sources.append(
            {
                "name": row[0],
                "url": row[1],  # Для Telegram это полный URL канала
                "type": "telegram" if "t.me" in row[1] else "rss",
                "channel": row[1].split("/")[-1] if "t.me" in row[1] else None,
                "filter_hours": 24,
            }
        )
    conn.close()
    return db_sources


# --- 2. HELPER FUNCTIONS ---
def clean_text(text):
    # For better compatibility with some AI models or databases.
    if not text:
        return ""
    replacements = {
        "ă": "a",
        "â": "a",
        "î": "i",
        "ș": "s",
        "ț": "t",
        "Ă": "A",
        "Â": "A",
        "Î": "I",
        "Ș": "S",
        "Ț": "T",
        "„": '"',
        "”": '"',
        "’": "'",
    }

    for k, v in replacements.items():
        text = text.replace(k, v)
    return text


def is_processed(link):
    return redis_client.exists(link)


def mark_processed(link):
    redis_client.set(link, "1", ex=86400)  # 24h expiration


def is_recent(date_obj, hours):
    if date_obj.tzinfo is None:
        date_obj = date_obj.replace(tzinfo=timezone.utc)
    now = datetime.now(timezone.utc)
    cutoff = now - timedelta(hours=hours)
    return date_obj > cutoff

def extract_full_text(url):
    try:
        resp = requests.get(url, headers=HEADERS, timeout=5)
        soup = BeautifulSoup(resp.content, "html.parser")
        for script in soup(["script", "style", "nav", "footer", "header", "aside"]):
            script.extract()
        paragraphs = soup.find_all("p")
        full_text = "\n\n".join(
            [p.get_text().strip() for p in paragraphs if len(p.get_text()) > 20]
        )
        return full_text if len(full_text) > 50 else ""
    except Exception as e:
        return ""


def send_to_kafka(source, title, link, summary, full_text, date_obj):
    # Determine best content
    content = full_text if len(full_text) > len(summary) else summary
    if not content:
        content = title
    clean_title = clean_text(title)
    clean_content = clean_text(content)
    clean_summary = clean_text(summary)
    article = {
        "title": clean_title,
        "link": link,
        "published": date_obj.isoformat(),
        "summary": clean_summary,
        "content": clean_content,
        "source": source,
    }
    producer.send(TOPIC_NAME, value=article)
    mark_processed(link)
    print(f"      ✅ [SENT] {clean_title[:50]}...")


# --- 3. LOGIC: RSS ---
def scrape_rss(source):
    print(f"   📡 RSS: {source['name']}...")
    try:
        resp = requests.get(source["url"], headers=HEADERS, timeout=10)
        feed = feedparser.parse(resp.content)
        if not feed.entries:
            print(f"      ⚠️ No entries found.")
            return 0
        count = 0
        for entry in feed.entries:
            link = entry.link
            if is_processed(link):
                continue
            try:
                pub_date = date_parser.parse(entry.published)
            except:
                pub_date = datetime.now(timezone.utc)
            if not is_recent(pub_date, source["filter_hours"]):
                mark_processed(link)
                continue
            full_text = extract_full_text(link)
            send_to_kafka(
                source["name"],
                entry.title,
                link,
                entry.get("summary", ""),
                full_text,
                pub_date,
            )
            count += 1
        return count
    except Exception as e:
        print(f"      ❌ RSS Error: {e}")
        return 0

# --- 4. LOGIC: TELEGRAM (API) ---
def scrape_all_telegram(client, telegram_sources):
    total_count = 0
    for source in telegram_sources:
        print(f"   ✈️ Telegram: {source['name']} ({source['channel']})...")
        try:
            entity = client.get_entity(source["channel"])
            posts = client.get_messages(entity, limit=20)  # Limit 20 to catch more news
            count = 0
            for post in posts:
                if not post.message:
                    continue
                link = f"https://t.me/{source['channel']}/{post.id}"
                if is_processed(link):
                    continue
                pub_date = post.date
                if not is_recent(pub_date, source["filter_hours"]):
                    mark_processed(link)
                    continue
                full_text = post.message
                title = full_text.split("\n")[0][:200]
                if len(title) < 10:
                    title = full_text[:60] + "..."
                send_to_kafka(
                    source["name"], title, link, full_text, full_text, pub_date
                )
                count += 1
            total_count += count
        except Exception as e:
            print(f"      ❌ TG Error ({source['name']}): {e}")
    return total_count


# --- 5. MAIN JOB ---
def job():
    print(f"\n🔄 Cycle: {datetime.now().strftime('%H:%M:%S')}")
    total = 0
    # 1. RSS Scrape
    rss_sources = [s for s in get_sources_from_db() if s["type"] == "rss"]
    for source in rss_sources:
        total += scrape_rss(source)

    # 2. Telegram Scrape
    tg_sources = [s for s in get_sources_from_db() if s["type"] == "telegram"]
    if tg_sources:
        if not API_ID or not API_HASH:
            print("      ❌ Skipped Telegram: Missing Credentials")
        else:
            try:
                with TelegramClient(SESSION_NAME, API_ID, API_HASH) as client:
                    total += scrape_all_telegram(client, tg_sources)
            except Exception as e:
                print(f"      ❌ Telegram Connection Failed: {e}")
    print(f"💤 Done. Sent {total} articles.")

schedule.every(10).minutes.do(job)


if __name__ == "__main__":
    job()
    while True:
        schedule.run_pending()
        time.sleep(1)
