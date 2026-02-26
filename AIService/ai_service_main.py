import json
import threading
import os
import time
import psycopg2
from pathlib import Path
from huggingface_hub import login
from dotenv import load_dotenv
from fastapi import FastAPI
from kafka import KafkaConsumer, KafkaProducer
from sentence_transformers import SentenceTransformer, util

current_dir = Path(__file__).resolve().parent
env_path = current_dir.parent / 'common' / '.env'
load_dotenv(dotenv_path=env_path)

KAFKA_BROKER = os.getenv('KAFKA_BROKER', 'localhost:9092')
HF_TOKEN = os.getenv('HUGGINGFACE_TOKEN')

DB_NAME = os.getenv("POSTGRES_DB", "newsbot_db")
DB_USER = os.getenv("POSTGRES_USER", "postgres")
DB_PASS = os.getenv("POSTGRES_PASSWORD", "password")
DB_HOST = os.getenv("POSTGRES_HOST", "localhost")
DB_PORT = os.getenv("POSTGRES_PORT", "5432")

TOPIC_NEWS_RAW = "news.raw"
TOPIC_USER_INTERESTS = "user.interests.updated"
TOPIC_NOTIFICATIONS = "news.notification"

app = FastAPI()

if HF_TOKEN: login(token=HF_TOKEN)
print("Loading Model...")
model = SentenceTransformer('intfloat/multilingual-e5-large', token=HF_TOKEN)

producer = KafkaProducer(
    bootstrap_servers=[KAFKA_BROKER],
    value_serializer=lambda x: json.dumps(x, ensure_ascii=False).encode('utf-8')
)


def get_db_connection():
    return psycopg2.connect(dbname=DB_NAME, user=DB_USER, password=DB_PASS, host=DB_HOST, port=DB_PORT)


def safe_deserialize(msg_bytes):
    """
    Safely deserialize Kafka message with fallback encodings
    """
    if msg_bytes is None:
        return None

    # Try UTF-8 first (most common)
    try:
        return json.loads(msg_bytes.decode('utf-8'))
    except UnicodeDecodeError:
        pass

    # Try UTF-8 with error handling
    try:
        decoded = msg_bytes.decode('utf-8', errors='ignore')
        return json.loads(decoded)
    except Exception:
        pass

    # Try Latin-1 as fallback
    try:
        return json.loads(msg_bytes.decode('latin-1'))
    except Exception:
        pass

    # Last resort: try to parse as string
    try:
        return json.loads(str(msg_bytes))
    except Exception as e:
        print(f"❌ Failed to deserialize message: {e}")
        print(f"Raw bytes (first 100): {msg_bytes[:100]}")
        return None


def process_news_stream():
    """
    Process incoming news articles with THREE scenarios:
    1. Read-All Source (Bypass AI)
    2. Strict Mode + AI Filtering
    3. Normal AI Filtering
    """
    print("🎧 Listening for News...")
    consumer = KafkaConsumer(
        TOPIC_NEWS_RAW,
        bootstrap_servers=[KAFKA_BROKER],
        auto_offset_reset='earliest',
        group_id='ai-news-group',
        value_deserializer=safe_deserialize  # Use safe deserializer
    )

    for message in consumer:
        conn = None
        try:
            article = message.value

            if article is None:
                print("⚠️  Skipping message - failed to deserialize")
                continue

            title = article.get('title', 'Untitled')
            link = article.get('link')
            source_name = article.get('source')
            summary = article.get('summary', '')

            if not link or not source_name:
                print(f"⚠️  Skipping article - missing link or source")
                continue

            text_to_vectorize = f"passage: {title} - {summary}"
            news_vector = model.encode(text_to_vectorize, normalize_embeddings=True).tolist()
            vector_str = str(news_vector)

            conn = get_db_connection()
            cur = conn.cursor()

            # Insert article
            try:
                cur.execute("""
                            INSERT INTO articles (title, url, summary, content, source_name, vector)
                            VALUES (%s, %s, %s, %s, %s, %s::vector)
                            """, (title, link, summary, article.get('content', ''), source_name, vector_str))
                conn.commit()
                print(f"✅ Saved: {title[:50]}")
            except psycopg2.errors.UniqueViolation:
                conn.rollback()
                print(f"⏭️  Duplicate: {title[:50]}")
                continue

            # Get source ID
            cur.execute("SELECT id FROM sources WHERE name = %s", (source_name,))
            source_row = cur.fetchone()
            if not source_row:
                print(f"⚠️  Source not found: {source_name}")
                continue
            source_id = source_row[0]

            # Get matching users with their settings
            cur.execute("""
                        SELECT
                            u.id AS user_id,
                            EXISTS(SELECT 1 FROM user_read_all_sources ur WHERE ur.user_id = u.id AND ur.source_id = %s) AS is_read_all,
                            EXISTS(SELECT 1 FROM user_subscriptions us WHERE us.user_id = u.id AND us.source_id = %s) AS is_subscribed,
                            u.show_only_subscribed_sources,
                            (1 - (u.interests_vector <=> %s::vector)) AS similarity
                        FROM users u
                        WHERE u.interests_vector IS NOT NULL
                        """, (source_id, source_id, vector_str))

            users = cur.fetchall()

            for user in users:
                user_id, is_read_all, is_subscribed, strict_mode, similarity = user
                similarity = float(similarity) if similarity else 0.0

                # SCENARIO 1: Read-All Source (Bypass AI)
                if is_read_all:
                    producer.send(TOPIC_NOTIFICATIONS, value={
                        "userId": user_id,
                        "title": title,
                        "url": link,
                        "score": 1.0,
                        "reason": "read_all"
                    })
                    print(f"📢 [Scenario 1] Read-All: User {user_id} - {title[:30]}")
                    continue

                # SCENARIO 2: Strict Mode + AI Filtering
                if strict_mode:
                    if not is_subscribed:
                        continue

                    if similarity > 0.82:
                        producer.send(TOPIC_NOTIFICATIONS, value={
                            "userId": user_id,
                            "title": title,
                            "url": link,
                            "score": similarity,
                            "reason": "strict_ai"
                        })
                        print(f"✨ [Scenario 2] Strict+AI: User {user_id} ({similarity:.4f})")
                    continue

                # SCENARIO 3: Normal AI Filtering
                if similarity > 0.80:
                    producer.send(TOPIC_NOTIFICATIONS, value={
                        "userId": user_id,
                        "title": title,
                        "url": link,
                        "score": similarity,
                        "reason": "normal_ai"
                    })
                    print(f"✨ [Scenario 3] Normal AI: User {user_id} ({similarity:.4f})")

        except Exception as e:
            print(f"❌ Error processing news: {e}")
            import traceback
            traceback.print_exc()
        finally:
            if conn:
                conn.close()


def listen_for_user_updates():
    """Listen for interest updates and save vector to Postgres"""
    print("🎧 Listening for User Interests...")
    consumer = KafkaConsumer(
        TOPIC_USER_INTERESTS,
        bootstrap_servers=[KAFKA_BROKER],
        group_id='ai-user-group',
        value_deserializer=safe_deserialize  # Use safe deserializer
    )

    for message in consumer:
        conn = None
        try:
            data = message.value

            if data is None:
                print("⚠️  Skipping message - failed to deserialize")
                continue

            user_id = data.get('userId')
            interests_text = data.get('interests')

            if not user_id or not interests_text:
                print(f"⚠️  Skipping - missing userId or interests")
                continue

            # Generate vector
            text_for_model = f"query: {interests_text}"
            vector = model.encode(text_for_model, normalize_embeddings=True).tolist()

            # Save to PostgreSQL
            conn = get_db_connection()
            cur = conn.cursor()
            cur.execute("""
                        UPDATE users
                        SET interests_vector = %s::vector
                        WHERE id = %s
                        """, (str(vector), user_id))

            conn.commit()
            print(f"✅ Updated User {user_id} Vector: {interests_text[:50]}")

        except Exception as e:
            print(f"❌ Error updating vector: {e}")
            import traceback
            traceback.print_exc()
        finally:
            if conn:
                conn.close()


@app.on_event("startup")
async def startup_event():
    print("🚀 Starting AI Service...")
    print("📊 Scenarios:")
    print("   1️⃣  Read-All Sources (Bypass AI)")
    print("   2️⃣  Strict Mode + AI Filtering")
    print("   3️⃣  Normal AI Filtering")

    t1 = threading.Thread(target=process_news_stream, daemon=True)
    t1.start()

    t2 = threading.Thread(target=listen_for_user_updates, daemon=True)
    t2.start()


@app.get("/health")
def health():
    return {"status": "running", "model": "multilingual-e5-large"}