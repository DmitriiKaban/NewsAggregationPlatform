# 📰 NewsBot: AI-Powered News Platform

An intelligent Telegram bot and Mini App platform that acts as a personal news curator. By leveraging Natural Language Processing (NLP) and vector databases, NewsBot filters through the noise of Telegram channels and RSS feeds to deliver only the articles that match a user's specific interests.

## ✨ Key Features

* **🧠 AI Semantic Matching:** Uses Hugging Face Sentence Transformers to convert user interests and news articles into high-dimensional vectors, matching them using cosine similarity.
* **📱 Telegram Mini App Integration:** Features a modern, responsive React-based Web App accessible directly inside Telegram for managing sources, interests, and viewing analytics.
* **🌍 Multilingual Support:** Fully localized in English, Romanian, and Russian.
* **📰 Custom Source Management:** Users can subscribe to specific Telegram channels or RSS feeds, with options to apply "Strict Filters" (only show subscribed sources).
* **📊 Smart Recommendations:** Analyzes user interest vectors to recommend new sources based on similar peer profiles (Cosine Distance < 0.25).
* **📈 Insights Dashboard:** Tracks Daily Active Users (DAU) and article read metrics.

## 🏗️ Architecture & Tech Stack

The platform is built using a microservices-inspired architecture, separating the Telegram interface from the heavy AI processing.

* **Bot Service (Backend):** Java 21, Spring Boot 3, Telegram Bot API
* **AI Engine:** Python, FastAPI, Hugging Face (`sentence-transformers`)
* **Database:** PostgreSQL with the `pgvector` extension
* **Message Broker:** Apache Kafka (for asynchronous news stream processing)
* **Frontend (Mini App):** React.js

## ⚙️ Prerequisites

To run this project locally, you will need:
* [Docker Desktop](https://www.docker.com/products/docker-desktop) (for PostgreSQL + pgvector and Kafka)
* [Java 21+](https://adoptium.net/)
* [Python 3.10+](https://www.python.org/downloads/)
* A Telegram Bot Token (obtained from [@BotFather](https://t.me/botfather))

## 🚀 Local Setup & Installation

### 1. Database & Infrastructure
Start the required infrastructure using Docker Compose.

```bash
docker-compose up -d

```

### 2. Configure Environment Variables

Create an `application.properties` (or `.env`) file in your Spring Boot `src/main/resources` directory:

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/newsbot
spring.datasource.username=your_db_user
spring.datasource.password=your_db_password
telegram.bot.username=YourBotName
telegram.bot.token=YOUR_TELEGRAM_BOT_TOKEN
spring.kafka.bootstrap-servers=localhost:9092

```

### 3. Start the AI Engine (Python)

Navigate to the AI service directory, install dependencies, and run the FastAPI server:

```bash
cd aiservice
pip install -r requirements.txt
uvicorn ai_service_main:app --reload --port 8000 

```

### 4. Start the Spring Boot Bot Service

Run the Spring Boot application via your IDE or using Maven/Gradle:

```bash
./mvnw spring-boot:run

```

### 5. Start Scraper (Python)

Navigate to the scraper directory, install dependencies, and run script:

```bash
cd scraperservice
pip install -r requirements.txt
python scraper_main.py

```
## 💬 Bot Commands

* `/start` - Initialize the bot and select your preferred language.
* `/help` - View the help menu and quick actions.
* `/myinterests [topics]` - Update your interests (e.g., `/myinterests AI, Space, F1`).
* `/addsource [link]` - Subscribe to a new Telegram channel.
* `/removesource [link]` - Unsubscribe from a source.
* `/sources` - View your active subscriptions.

## 📂 Project Structure

```text
NewsAggregatorPlatform/
├── BotService/                 # Spring Boot Java Application
│   ├── src/main/java/.../commands/    # Telegram Command Strategies
│   ├── src/main/java/.../models/      # JPA Entities
│   ├── src/main/java/.../service/     # Core Business Logic & Kafka Producers
│   └── src/main/resources/            # i18n Localization & schema.sql
├── AIService/                  # Python AI Engine
│   ├── ai_service_main.py             # FastAPI & Kafka Consumers
│   └── requirements.txt               # Python Dependencies
├── ScraperService/             # Python Scraper
│   ├── scraper_main.py                # Scraper
│   └── requirements.txt               # Python Dependencies
└── newsbot-ui/                 # React Mini App (Web App)
└── common/                     # Infrastructure 
│   ├── docker-compose.py            
│   ├── .env             

```

## 🎓 Academic Context

This project was developed as a university thesis, demonstrating the practical application of Vector Databases (`pgvector`), asynchronous event-driven architecture (Kafka), and seamless integration between traditional backend frameworks (Spring Boot) and modern AI tools (Python/Hugging Face).

---

*Developed by Cravcenco Dmitrii and Bujilov Dmitrii
