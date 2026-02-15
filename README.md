# 📰 NewsBot - AI-Powered Telegram News Aggregator

NewsBot is a microservices-based system that delivers personalized news feeds to users via Telegram. It scrapes news from various sources, uses AI to understand the content and user preferences (via vector embeddings), and delivers highly relevant updates in real-time.

## 🚀 Key Features

* ** automated Scraping**: Python-based scraper fetches RSS feeds and raw HTML news.
* **AI-Driven Personalization**: Uses HuggingFace Transformer models (`all-MiniLM-L6-v2`) to vectorize articles and user interests.
* **Vector Search**: Matches news to users based on semantic similarity (Cosine Similarity), not just keyword matching.
* **Event-Driven Architecture**: Fully decoupled services communicating via **Apache Kafka**.
* **Telegram Integration**: Interactive bot for user registration and preference management.

## 🛠️ Tech Stack

| Component | Technology | Description |
| --- | --- | --- |
| **Core Service** | Java 21, Spring Boot 3 | Orchestrator, User Management, Telegram Bot integration. |
| **AI Service** | Python, FastAPI, PyTorch | Vectorization, Semantic Matching, ML Model serving. |
| **Scraper** | Python, BeautifulSoup | Fetches raw news data. |
| **Message Broker** | Apache Kafka + Zookeeper | Handles asynchronous data flow between services. |
| **Databases** | PostgreSQL | Relational data (User profiles, state). |
|  | MongoDB | Unstructured data (News articles, Vector embeddings). |
| **Infrastructure** | Docker Compose | Local orchestration of databases and brokers. |

---

## 📂 Project Structure

```text
/newsbot-system
├── /common                # Shared configuration
│   ├── docker-compose.yml # Infrastructure (Kafka, DBs)
│   └── .env               # Environment variables (Secrets)
├── /newsbot-core          # Spring Boot Application
│   ├── src/main/java...   # Bot Logic, Command Strategies
│   └── pom.xml
├── /newsbot-ai            # Python AI Service
│   ├── ai_service_main.py # FastAPI app & Kafka Consumers
│   └── requirements.txt
└── /newsbot-scraper       # Python Scraper Service
    ├── scraper_main.py    # RSS fetching logic
    └── requirements.txt

```

---

## ⚡ Getting Started (Local Development)

This project runs in a **Hybrid Mode** for development: Infrastructure runs in Docker, while Service applications run locally on your machine for easy debugging.

### 1. Prerequisites

* Docker & Docker Compose
* Java 21 JDK & Maven
* Python 3.9+

### 2. Configuration (`.env`)

Create a `.env` file in the `common/` folder. **Do not commit this file.**

```ini
# Infrastructure Credentials
POSTGRES_DB=db
POSTGRES_USER=user
POSTGRES_PASSWORD=password

MONGO_INITDB_ROOT_USERNAME=user
MONGO_INITDB_ROOT_PASSWORD=password

# Telegram Bot (Get from @BotFather)
TG_BOT_TOKEN=123456789:ABCdefGHIjklMNOpqrsTUVwxyz
TG_BOT_USER=user
TG_BOT_PASSWORD=password

# Kafka
KAFKA_BROKER=localhost:9092

```

### 3. Start Infrastructure

Run the databases and message broker using Docker.

```bash
cd common
docker-compose up -d

```

* **Postgres**: `localhost:5433` (Mapped to avoid conflict with local DBs)
* **MongoDB**: `localhost:27017`
* **Kafka**: `localhost:9092`
* **Kafka UI**: `http://localhost:8080` (Use this to view topics/messages)

---

## 🏃 Running the Services

### A. Core Service (Spring Boot)

1. Open `/newsbot-core` in IntelliJ.
2. Ensure Maven dependencies are loaded.
3. **Run Configuration**: If `.env` is not loading automatically, add this VM Option:
```text
-Ddotenv.directory=../common

```


4. Run `NewsBotApplication`.
5. *Verification*: Send `/start` to your Telegram Bot.

### B. AI Service (Python)

1. Navigate to `/newsbot-ai`.
2. Create and activate a virtual environment:
```bash
python -m venv .venv
# Windows:
.\.venv\Scripts\activate
# Mac/Linux:
source .venv/bin/activate

```


3. Install dependencies:
```bash
pip install -r requirements.txt

```


*(Note: If on Windows and facing `OSError: [WinError 1114]`, install the CPU version of PyTorch via pip).*
4. Run the service:
```bash
uvicorn ai_service_main:app --reload --port 8000

```



### C. Scraper Service (Python)

1. Navigate to `/newsbot-scraper`.
2. Setup venv and install requirements (similar to AI service).
3. Run the scraper manually to test:
```bash
python scraper_main.py

```



---

## 🤖 Bot Commands

| Command               | Description                                               |
|-----------------------|-----------------------------------------------------------|
| `/start`              | Registers you with the system and shows the welcome menu. |
| `/myinterests [text]` | Updates your preference profile. <br>                     |

<br> *Example:* `/myinterests AI, Nvidia, Formula 1` |
| `/help` | Shows available commands and usage examples. |

---

## 🧠 Data Flow

1. **User** sends `/myinterests Crypto` to **Core Service**.
2. **Core Service** saves text to Postgres and produces event `user.interests.updated` to Kafka.
3. **AI Service** consumes event, vectorizes "Crypto", and upserts to MongoDB `users` collection.
4. **Scraper** fetches article "Bitcoin hits $100k", pushes to `news.raw`.
5. **AI Service** consumes `news.raw`, vectorizes article, and searches MongoDB for matching users (Cosine Sim > 0.35).
6. **AI Service** produces `news.notification` event.
7. **Core Service** consumes notification and sends Telegram message to User.

## 🔮 Future Roadmap

* [ ] **Containerization**: Dockerfiles for all 3 services.
* [ ] **Orchestration**: Kubernetes (K8s) deployment manifests.
* [ ] **CI/CD**: Jenkins pipeline for automated testing and building.
* [ ] **Cloud**: Terraform scripts for AWS deployment (EKS, RDS).
