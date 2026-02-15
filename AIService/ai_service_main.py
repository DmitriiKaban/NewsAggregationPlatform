import json
import threading
import os
import time
from pathlib import Path
from huggingface_hub import login
from dotenv import load_dotenv
from fastapi import FastAPI
from kafka import KafkaConsumer, KafkaProducer
from pymongo import MongoClient
from sentence_transformers import SentenceTransformer, util

# --- CONFIGURATION ---
current_dir = Path(__file__).resolve().parent
env_path = current_dir.parent / 'common' / '.env'
load_dotenv(dotenv_path=env_path)

KAFKA_BROKER = os.getenv('KAFKA_BROKER', 'localhost:9092')
MONGO_USER = os.getenv("MONGO_INITDB_ROOT_USERNAME", "mongo_admin")
MONGO_PASS = os.getenv("MONGO_INITDB_ROOT_PASSWORD", "mongo_pass")
MONGO_URI = f"mongodb://{MONGO_USER}:{MONGO_PASS}@localhost:27017/?authSource=admin"

# Topics
TOPIC_NEWS_RAW = "news.raw"
TOPIC_USER_INTERESTS = "user.interests.updated"
TOPIC_NOTIFICATIONS = "news.notification"
HF_TOKEN = os.getenv('HUGGINGFACE_TOKEN')

app = FastAPI()

if HF_TOKEN:
    print("Authenticating with Hugging Face...")
    login(token=HF_TOKEN)

# --- 🧠 MODEL UPGRADE ---
print("Loading Advanced Multilingual Model (E5-Large)...")
model = SentenceTransformer(
    'intfloat/multilingual-e5-large',
    token=HF_TOKEN
)

mongo_client = MongoClient(MONGO_URI)
db = mongo_client['newsbot_db']
articles_collection = db['articles']
users_collection = db['users']

producer = KafkaProducer(
    bootstrap_servers=[KAFKA_BROKER],
    value_serializer=lambda x: json.dumps(x).encode('utf-8')
)

def listen_for_user_updates():
    print("🎧 Listening for User Interests...")
    consumer = KafkaConsumer(
        TOPIC_USER_INTERESTS,
        bootstrap_servers=[KAFKA_BROKER],
        group_id='ai-user-group',
        value_deserializer=lambda x: json.loads(x.decode('utf-8'))
    )

    for message in consumer:
        try:
            data = message.value
            user_id = data['userId']
            interests_text = data['interests']

            text_for_model = f"query: {interests_text}"

            vector = model.encode(text_for_model, normalize_embeddings=True).tolist()

            users_collection.update_one(
                {"user_id": user_id},
                {"$set": {"interests_text": interests_text, "vector": vector}},
                upsert=True
            )
            print(f"Updated User {user_id}: {interests_text}")

        except Exception as e:
            print(f"Error updating user: {e}")


def process_news_stream():
    print("🎧 Listening for News...")
    consumer = KafkaConsumer(
        TOPIC_NEWS_RAW,
        bootstrap_servers=[KAFKA_BROKER],
        auto_offset_reset='earliest',
        group_id='ai-news-group',
        value_deserializer=lambda x: json.loads(x.decode('utf-8'))
    )

    for message in consumer:
        try:
            article = message.value
            title = article.get('title')
            link = article.get('link')

            # Deduplication
            if articles_collection.find_one({"link": link}):
                continue

            summary = article.get('summary', '')
            text_to_vectorize = f"passage: {title} - {summary}"

            # Normalize embeddings ensures Cosine Similarity works perfectly (0 to 1 scale)
            news_vector = model.encode(text_to_vectorize, normalize_embeddings=True).tolist()

            # Save Article
            article_doc = {**article, "vector": news_vector, "processed_at": time.time()}
            articles_collection.insert_one(article_doc)
            print(f"Processed News: {title}")

            # MATCHING LOGIC
            users = list(users_collection.find({}))

            for user in users:
                user_vector = user.get('vector')

                if not user_vector: continue

                # Calculate Cosine Similarity
                similarity = util.cos_sim(news_vector, user_vector).item()
                print(f"News similarity for user: {user['user_id']} is {similarity:.4f}")
                # 💡 ADJUSTED THRESHOLD
                # > 0.80 is usually a direct match.
                # > 0.75 is a strong semantic match.
                if similarity > 0.80:
                    notification = {
                        "userId": user['user_id'],
                        "title": title,
                        "url": link,
                        "score": similarity
                    }
                    producer.send(TOPIC_NOTIFICATIONS, value=notification)
                    print(f"Match! User {user['user_id']} (Score: {similarity:.4f})")

        except Exception as e:
            print(f"Error processing news: {e}")

@app.on_event("startup")
async def startup_event():
    t1 = threading.Thread(target=process_news_stream)
    t1.daemon = True
    t1.start()

    t2 = threading.Thread(target=listen_for_user_updates)
    t2.daemon = True
    t2.start()

@app.get("/health")
def health():
    return {"status": "running"}