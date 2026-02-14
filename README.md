# 📰 NewsBot: AI-Powered Personalized News Aggregator

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![Java](https://img.shields.io/badge/AI%20%26%20Backend-Spring%20Boot-green)](https://spring.io/)
[![Python](https://img.shields.io/badge/Scraper-Python-blue)](https://python.org/)
[![React](https://img.shields.io/badge/Frontend-Telegram%20Mini%20App-blueviolet)](https://react.dev/)
[![Docker](https://img.shields.io/badge/Infrastructure-Docker-2496ED)](https://www.docker.com/)

## 📖 Overview
**NewsBot** is a thesis project designed to solve information overload. It is a smart news aggregation platform that uses **Artificial Intelligence** to curate a personalized news feed for users directly inside Telegram.

Unlike standard RSS readers, NewsBot uses **Semantic Analysis (NLP)** to understand the *meaning* of news articles and matches them with user interests, filtering out noise and irrelevant content.

## 🚀 Key Features
* **🧠 AI-Driven Personalization:** Uses Vector Embeddings (Hugging Face models) to match news articles with user interests semantically, not just by keywords.
* **📱 Telegram Mini App:** A full-featured React UI embedded inside Telegram for managing sources and interests.
* **🔄 Multi-Source Aggregation:** Scrapes news from RSS feeds and Telegram Channels in real-time.
* **🔔 Smart Notifications:** Delivers news alerts directly to the user's chat.
* **⚡ Event-Driven Architecture:** Uses **Apache Kafka** to decouple the Scraper, AI Engine, and Bot Service.

## 🏗️ Architecture

The system is built as a microservices-style architecture containerized with Docker:

| Service | Tech Stack | Description |
| :--- | :--- | :--- |
| **Bot Service** | Java 21, Spring Boot | Core backend. Manages users, preferences, and Telegram API interactions. |
| **AI Service** | Java 21, Spring Boot | Consumes raw news from Kafka, generates vector embeddings, and filters relevance. |
| **Scraper Service** | Python, BeautifulSoup, Telethon | Fetches data from RSS feeds and Telegram channels periodically. |
| **Frontend** | React, TypeScript, Vite | Telegram Mini App (Webview) for a rich user interface. |
| **Database** | PostgreSQL & MongoDB | **Postgres:** User data & Relational logic.<br>**Mongo:** News articles & Vector storage. |
| **Broker** | Apache Kafka | Handles high-throughput message passing between services. |

## 🛠️ Prerequisites
* Docker & Docker Compose
* Java 21 JDK
* Python 3.10+
* Node.js 18+ (for Frontend)
* Telegram Bot Token (from @BotFather)
* Telegram API ID & Hash (from my.telegram.org)

## 📦 Installation & Setup

### 1. Clone the Repository
```bash
git clone https://github.com/yourusername/NewsAggregatorPlatform.git
cd NewsAggregatorPlatform
```
2. Configure Environment Variables
Create a .env file in the common/ folder (or root, depending on your setup):
```
# Database
POSTGRES_USER=admin
POSTGRES_PASSWORD=password
MONGO_INITDB_ROOT_USERNAME=mongo_admin
MONGO_INITDB_ROOT_PASSWORD=mongo_pass

# Telegram
TG_BOT_TOKEN=your_bot_token_here
TELEGRAM_API_ID=your_api_id
TELEGRAM_API_HASH=your_api_hash

# AI & Kafka
HUGGINGFACE_TOKEN=your_hf_token
KAFKA_BROKER=kafka:9092
```
3. Run with Docker Compose
The easiest way to start the entire infrastructure (DBs, Kafka, Zookeeper) is:

```Bash
docker-compose up -d
```
4. Run Services (Development Mode)

Backend (Spring Boot):

```Bash
cd BotService
./mvnw spring-boot:run
```
AI Service:

```Bash
cd AIService
pip install -r requirements.txt
python ai_service_main.py
```
Scraper:

```Bash
cd Scraper
pip install -r requirements.txt
python scraper_main.py
```
Frontend (Mini App):

```Bash
cd newsbot-ui
npm install
npm run dev
```
📱 How to Use
Open the bot in Telegram.

Press /start to register.

Click the "📱 Open News App" button to launch the Mini App.

Add your interests (e.g., "Artificial Intelligence", "Formula 1") and Sources.

Wait for the Scraper to fetch news; the AI will filter and send you relevant updates!

🤝 Contributing
This is a thesis project developed by Bujilov Dmitrii and Cravcenco Dmitrii.
Contributions, issues, and feature requests are welcome!

📄 License
Distributed under the MIT License. See LICENSE for more information.
