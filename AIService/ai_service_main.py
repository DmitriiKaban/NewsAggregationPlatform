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

current_dir = Path(__file__).resolve().parent
env_path = current_dir.parent / 'common' / '.env'
load_dotenv(dotenv_path=env_path)

KAFKA_BROKER = os.getenv('KAFKA_BROKER', 'localhost:9092')
HF_TOKEN = os.getenv('HUGGINGFACE_TOKEN')

DB_NAME = os.getenv("DB_NAME", "newsbot_db")
DB_USER = os.getenv("DB_USER", "postgres")
DB_PASS = os.getenv("DB_PASS", "password")
DB_HOST = os.getenv("DB_HOST", "localhost")
DB_PORT = os.getenv("DB_PORT", "5432")

TOPIC_NEWS_RAW = "news.raw"
TOPIC_USER_INTERESTS = "user.interests.updated"
TOPIC_NOTIFICATIONS = "news.notification"

app = FastAPI()

if HF_TOKEN: login(token=HF_TOKEN)
print("Loading Model...")
model = SentenceTransformer('intfloat/multilingual-e5-large', token=HF_TOKEN)

producer = KafkaProducer(
    bootstrap_servers=[KAFKA_BROKER],
    value_serializer=lambda x: json.dumps(x).encode('utf-8')
)


def get_db_connection():
    return psycopg2.connect(dbname=DB_NAME, user=DB_USER, password=DB_PASS, host=DB_HOST, port=DB_PORT)


def process_news_stream():
    print("listening...")
    consumer = KafkaConsumer(
        TOPIC_NEWS_RAW, bootstrap_servers=[KAFKA_BROKER],
        auto_offset_reset='earliest', group_id='ai-news-group',
        value_deserializer=lambda x: json.loads(x.decode('utf-8'))
    )

    for message in consumer:
        conn = None
        try:
            article = message.value
            title = article.get('title')
            link = article.get('link')
            source_name = article.get('source')
            summary = article.get('summary', '')

            text_to_vectorize = f"passage: {title} - {summary}"
            news_vector = model.encode(text_to_vectorize, normalize_embeddings=True).tolist()
            vector_str = str(news_vector) # Format for pgvector

            conn = get_db_connection()
            cur = conn.cursor()

            try:
                cur.execute("""
                    INSERT INTO articles (title, url, summary, content, source_name, vector)
                    VALUES (%s, %s, %s, %s, %s, %s::vector)
                """, (title, link, summary, article.get('content', ''), source_name, vector_str))
                conn.commit()
            except psycopg2.errors.UniqueViolation:
                conn.rollback()
                continue

            print(f"saved: {title[:50]}")

            cur.execute("SELECT id FROM sources WHERE name = %s", (source_name,))
            source_row = cur.fetchone()
            if not source_row:
                continue
            source_id = source_row[0]

            cur.execute("""
                SELECT
                    u.id AS user_id,
                    EXISTS(SELECT 1 FROM user_read_all_sources ur WHERE ur.user_id = u.id AND ur.source_id = %s) AS is_read_all,
                    EXISTS(SELECT 1 FROM user_subscriptions us WHERE us.user_id = u.id AND us.source_id = %s) AS is_subscribed,
                    u.show_only_subscribed_sources,
                    (1 - (u.interests_vector <=> %s::vector)) AS similarity
                FROM app_users u
            """, (source_id, source_id, vector_str))

            users = cur.fetchall()

            for user in users:
                user_id, is_read_all, is_subscribed, strict_mode, similarity = user
                similarity = similarity if similarity else 0.0

                if is_read_all:
                    producer.send(TOPIC_NOTIFICATIONS, value={"userId": user_id, "title": title, "url": link, "score": 1.0, "reason": "read_all"})
                    continue

                if strict_mode:
                    if not is_subscribed: continue
                    if similarity > 0.82:
                        producer.send(TOPIC_NOTIFICATIONS, value={"userId": user_id, "title": title, "url": link, "score": similarity, "reason": "strict_ai"})
                    continue

                if similarity > 0.80:
                    producer.send(TOPIC_NOTIFICATIONS, value={"userId": user_id, "title": title, "url": link, "score": similarity, "reason": "normal_ai"})

        except Exception as e:
            print(f"Error processing news: {e}")
        finally:
            if conn: conn.close()


def listen_for_user_updates():
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
            print(f"Updated User {user_id}: {interests_text}")

        except Exception as e:
            print(f"Error updating user interests: {e}")


def listen_for_source_updates():
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

            print(f"Updated Sources for User {user_id}:")

        except Exception as e:
            print(f"Error updating user sources: {e}")


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