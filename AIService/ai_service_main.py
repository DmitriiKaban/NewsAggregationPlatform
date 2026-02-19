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


def process_news_stream():
    """
    Process incoming news articles with THREE scenarios:

    1. SCENARIO 1: Read-All Source (Bypass AI completely)
       - User has this source in readAllPostsSources
       - Send ALL news from this source, NO AI filtering
       - Highest priority

    2. SCENARIO 2: Strict Mode + AI Filtering
       - showOnlySubscribedSources = true
       - User has interests
       - Only process news from subscribed sources
       - Apply AI filtering based on interests

    3. SCENARIO 3: Normal AI Filtering (Default)
       - showOnlySubscribedSources = false
       - Process news from ALL sources
       - Apply AI filtering based on interests
    """
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
            source_url = article.get('source')

            # Deduplication
            if articles_collection.find_one({"link": link}):
                continue

            summary = article.get('summary', '')
            text_to_vectorize = f"passage: {title} - {summary}"

            # Generate embeddings
            news_vector = model.encode(text_to_vectorize, normalize_embeddings=True).tolist()

            # Save Article
            article_doc = {**article, "vector": news_vector, "processed_at": time.time()}
            articles_collection.insert_one(article_doc)
            print(f"✅ Processed: {title}")

            # MATCHING LOGIC - Process each user
            users = list(users_collection.find({}))

            for user in users:
                user_id = user['user_id']
                subscribed_sources = user.get('subscriptions', [])
                read_all_sources = user.get('readAllPostsSources', [])
                show_only_subscribed = user.get('showOnlySubscribedSources', False)
                user_vector = user.get('vector')

                # ========================================
                # SCENARIO 1: Read-All Source (BYPASS AI)
                # ========================================
                if source_url in read_all_sources:
                    notification = {
                        "userId": user_id,
                        "title": title,
                        "url": link,
                        "score": 1.0,
                        "reason": "read_all_source"
                    }
                    producer.send(TOPIC_NOTIFICATIONS, value=notification)
                    print(f"📢 [Scenario 1] Read-All: User {user_id} - {title[:50]}")
                    continue  # Skip AI filtering for this user

                # ========================================
                # SCENARIO 2: Strict Mode + AI Filtering
                # ========================================
                if show_only_subscribed:
                    # Check if source is in subscriptions
                    if source_url not in subscribed_sources:
                        print(f"⏭️  [Scenario 2] Skipping: User {user_id} - not subscribed to {source_url}")
                        continue  # Skip this article for this user

                    # Source is subscribed, now apply AI filtering
                    if not user_vector:
                        print(f"⚠️  [Scenario 2] User {user_id} has no interests")
                        continue

                    similarity = util.cos_sim(news_vector, user_vector).item()
                    print(f"📊 [Scenario 2] Strict+AI: User {user_id}, Score: {similarity:.4f}")

                    if similarity > 0.80:
                        notification = {
                            "userId": user_id,
                            "title": title,
                            "url": link,
                            "score": similarity,
                            "reason": "strict_ai_match"
                        }
                        producer.send(TOPIC_NOTIFICATIONS, value=notification)
                        print(f"✨ [Scenario 2] Strict+AI Match: User {user_id} ({similarity:.4f})")
                    continue  # Done with this user

                # ========================================
                # SCENARIO 3: Normal AI Filtering (Default)
                # ========================================
                if not user_vector:
                    print(f"⚠️  [Scenario 3] User {user_id} has no interests")
                    continue

                similarity = util.cos_sim(news_vector, user_vector).item()
                print(f"📊 [Scenario 3] Normal AI: User {user_id}, Score: {similarity:.4f}")

                if similarity > 0.80:
                    notification = {
                        "userId": user_id,
                        "title": title,
                        "url": link,
                        "score": similarity,
                        "reason": "ai_match"
                    }
                    producer.send(TOPIC_NOTIFICATIONS, value=notification)
                    print(f"✨ [Scenario 3] Normal AI Match: User {user_id} ({similarity:.4f})")

        except Exception as e:
            print(f"❌ Error processing news: {e}")
            import traceback
            traceback.print_exc()


def listen_for_user_updates():
    """Listen for user interest updates"""
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
                {"$set": {
                    "interests_text": interests_text,
                    "vector": vector,
                    "updated_at": time.time()
                }},
                upsert=True
            )
            print(f"✅ Updated User {user_id}: {interests_text}")

        except Exception as e:
            print(f"❌ Error updating user interests: {e}")


def listen_for_source_updates():
    """Listen for user source subscription updates"""
    print("🎧 Listening for User Source Updates...")
    consumer = KafkaConsumer(
        TOPIC_USER_SOURCES,
        bootstrap_servers=[KAFKA_BROKER],
        group_id='ai-sources-group',
        value_deserializer=lambda x: json.loads(x.decode('utf-8'))
    )

    for message in consumer:
        try:
            data = message.value
            user_id = data['userId']

            subscriptions = data.get('subscriptions', [])
            read_all_sources = data.get('readAllPostsSources', [])
            show_only_subscribed = data.get('showOnlySubscribedSources', False)

            users_collection.update_one(
                {"user_id": user_id},
                {"$set": {
                    "subscriptions": subscriptions,
                    "readAllPostsSources": read_all_sources,
                    "showOnlySubscribedSources": show_only_subscribed,
                    "sources_updated_at": time.time()
                }},
                upsert=True
            )

            print(f"✅ Updated Sources for User {user_id}:")
            print(f"   - Subscriptions: {len(subscriptions)}")
            print(f"   - Read-All: {len(read_all_sources)}")
            print(f"   - Strict Mode: {show_only_subscribed}")

        except Exception as e:
            print(f"❌ Error updating user sources: {e}")


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