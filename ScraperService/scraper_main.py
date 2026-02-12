import time
import json
import feedparser
import schedule
from kafka import KafkaProducer
import os
from pathlib import Path
from dotenv import load_dotenv

# 1. Find the .env file in the sibling 'common' directory
current_dir = Path(__file__).resolve().parent
env_path = current_dir.parent / 'common' / '.env'

# 2. Load it
print(f"Loading .env from: {env_path}")
load_dotenv(dotenv_path=env_path)

# 3. Verify
print(f"Kafka Broker: {os.getenv('KAFKA_BROKER', 'localhost:9092')}")

# Configuration
load_dotenv()
KAFKA_BROKER = os.getenv('KAFKA_BROKER', 'localhost:9092')
TOPIC_NAME = 'news.raw'
RSS_FEEDS = [
    "https://feeds.bbci.co.uk/news/technology/rss.xml",
    "https://techcrunch.com/feed/"
]

producer = KafkaProducer(
    bootstrap_servers=[KAFKA_BROKER],
    value_serializer=lambda x: json.dumps(x).encode('utf-8')
)

def scrape_feeds():
    print("Starting scrape cycle...")
    for feed_url in RSS_FEEDS:
        feed = feedparser.parse(feed_url)
        for entry in feed.entries:
            article = {
                "title": entry.title,
                "link": entry.link,
                "published": entry.get("published", ""),
                "summary": entry.get("summary", ""),
                "source": feed_url
            }
            # Push to Kafka
            producer.send(TOPIC_NAME, value=article)
            print(f"Sent: {article['title']}")

    producer.flush()
    print("Scrape cycle complete.")

schedule.every(10).minutes.do(scrape_feeds)

if __name__ == "__main__":
    scrape_feeds()
    while True:
        schedule.run_pending()
        time.sleep(1)