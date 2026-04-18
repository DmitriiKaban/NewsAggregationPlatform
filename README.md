# 📰 NOVIS: AI-Powered News Platform

An intelligent Telegram bot and Mini App platform that acts as a personal news curator. By leveraging Natural Language Processing (NLP) and vector databases, NewsBot filters through the noise of Telegram channels and RSS feeds to deliver only the articles that match a user's specific interests.

---

## 🏗 Architecture & Tech Stack

The project is built using a modern, scalable microservices architecture:

* **BotService (Java 21 / Spring Boot):** Handles Telegram Bot interactions, user state management, analytics, and serves as the primary backend API.
* **AIRecommendationsService (Java 21 / Spring Boot):** Uses Deep Java Library (DJL) and PyTorch for NLP topic classification, semantic embeddings, and user-specific news recommendations.
* **ScraperService (Python 3):** An automated web scraper utilizing `BeautifulSoup` and `Kafka` to fetch and ingest raw news feeds.
* **Newsbot UI (Node.js / React / Vite / TypeScript):** A sleek frontend dashboard integrated as a Telegram WebApp for user settings, insights, and administrative moderation.
* **Infrastructure:** PostgreSQL (with `pgvector`), Apache Kafka, Zookeeper, Redis, Docker, and Kubernetes.
* **Observability:** Prometheus, Grafana, Loki, and Promtail.

---

## ⚙️ Prerequisites

Ensure you have the following installed on your local machine before starting:
* [Java 21](https://adoptium.net/)
* [Python 3](https://www.python.org/downloads/)
* [Node.js](https://nodejs.org/) (v18+ recommended)
* [Docker & Docker Compose](https://www.docker.com/)
* *(Optional)* Kubernetes CLI (`kubectl`) and `ngrok` for K8s deployment.

---

## 🚀 Quick Start

### 1. Environment Setup
Before starting the infrastructure, configure your environment variables. 
Create a `.env` file in the `common/` directory. The required variables and structural examples are specified in the `.env.example` file.

### 2. Launching the Infrastructure
You must start the underlying databases, message brokers, and monitoring tools before running the application services.

**Option A: Docker Compose**
```bash
cd common
docker-compose up -d
```

**Option B: Kubernetes**
```bash
cd k8s
./start-k8s.sh
```

---

## 💻 Running the Services Locally

If you are using Docker Compose for infrastructure, you can run the individual services manually for active development:

### Bot Service & AI Recommendations Service (Java)
```bash
# Terminal 1
cd BotService
./mvnw spring-boot:run

# Terminal 2
cd AIRecommendationsService
./mvnw spring-boot:run
```

### Scraper Service (Python)
```bash
cd ScraperService
pip install -r requirements.txt
python scraper_main.py
```

### Newsbot UI (React Frontend)
```bash
cd newsbot-ui
npm install
npm run dev
```

---

## 🧪 Testing

Each service utilizes its respective testing frameworks. 

**Java Services:**
*(Note: `AIRecommendationsService` requires Docker to be running for Testcontainers integration).*
```bash
cd BotService
./mvnw test

cd ../AIRecommendationsService
./mvnw test
```

**Scraper Service:**
```bash
cd ScraperService
pytest
```

---

## 👨‍💻 Authors
Developed by **Bujilov Dmitrii** and **Cravcenco Dmitrii**