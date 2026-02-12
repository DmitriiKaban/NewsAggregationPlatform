import json
import threading
import os
import time

from dotenv import load_dotenv
from fastapi import FastAPI
from kafka import KafkaConsumer, KafkaProducer
from pymongo import MongoClient
from sentence_transformers import SentenceTransformer
from pathlib import Path

# 1. Find the .env file in the sibling 'common' directory
current_dir = Path(__file__).resolve().parent
env_path = current_dir.parent / 'common' / '.env'

# 2. Load it
print(f"Loading .env from: {env_path}")
load_dotenv(dotenv_path=env_path)

# 3. Verify
print(f"Kafka Broker: {os.getenv('KAFKA_BROKER', 'localhost:9092')}")

app = FastAPI()

# Configuration
MONGO_URI = "mongodb://localhost:27017/"
KAFKA_BROKER = os.getenv('KAFKA_BROKER', 'localhost:9092')
RAW_TOPIC = "news.raw"
PROCESSED_TOPIC = "news.processed"
MONGO_USER = os.getenv("MONGO_INITDB_ROOT_USERNAME", "mongo_admin")
MONGO_PASS = os.getenv("MONGO_INITDB_ROOT_PASSWORD", "mongo_pass")
MONGO_URI = f"mongodb://{MONGO_USER}:{MONGO_PASS}@localhost:27017/"

# Initialize Models & DB
print("Loading ML Model...")
model = SentenceTransformer('all-MiniLM-L6-v2')
mongo_client = MongoClient(MONGO_URI)
db = mongo_client['newsbot_db']
articles_collection = db['articles']

producer = KafkaProducer(
    bootstrap_servers=[KAFKA_BROKER],
    value_serializer=lambda x: json.dumps(x).encode('utf-8')
)

def process_news_stream():
    print("🎧 AI Service: Listening for news...")
    consumer = KafkaConsumer(
        RAW_TOPIC,
        bootstrap_servers=[KAFKA_BROKER],
        auto_offset_reset='earliest',
        group_id='ai-service-group',
        value_deserializer=lambda x: json.loads(x.decode('utf-8'))
    )

    for message in consumer:
        article = message.value
        title = article.get('title')
        print(f"Processing: {title}")

        # 1. Simple Deduplication Check (by URL or Title)
        if articles_collection.find_one({"link": article['link']}):
            print(f"Skipping duplicate: {title}")
            continue

        # 2. Vectorization
        text_to_vectorize = f"{title} {article.get('summary', '')}"
        embedding = model.encode(text_to_vectorize).tolist()

        # 3. Store in MongoDB
        doc = {
            **article,
            "vector": embedding,
            "processed_at": time.time()
        }
        result = articles_collection.insert_one(doc)

        # 4. Notify Core Service
        notification = {
            "article_id": str(result.inserted_id),
            "title": title,
            "link": article['link'],
            "vector": embedding # Optional: send vector if Core does match
        }
        producer.send(PROCESSED_TOPIC, value=notification)
        print(f"Processed & Saved: {title}")

# Run Kafka Consumer in a background thread so FastAPI can still serve health checks
@app.on_event("startup")
async def startup_event():
    t = threading.Thread(target=process_news_stream)
    t.daemon = True
    t.start()

@app.get("/health")
def health():
    return {"status": "running"}
