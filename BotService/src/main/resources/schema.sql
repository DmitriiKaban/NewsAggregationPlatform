-- AI Vector Extension
CREATE EXTENSION IF NOT EXISTS vector;

CREATE TABLE IF NOT EXISTS sources (
                                       id BIGSERIAL PRIMARY KEY,
                                       name VARCHAR(255) NOT NULL,
    url VARCHAR(500) NOT NULL UNIQUE,
    type VARCHAR(50) NOT NULL,
    trust_level VARCHAR(50),
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
    );

CREATE TABLE IF NOT EXISTS users (
                                         id BIGINT PRIMARY KEY, -- Telegram User ID
                                         username VARCHAR(255),
    first_name VARCHAR(255),
    last_name VARCHAR(255),
    interests_raw TEXT,
    interests_vector vector(1024),
    show_only_subscribed_sources BOOLEAN NOT NULL DEFAULT FALSE,
    registered_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    last_active_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
    );


-- 2. Create a table to track exactly which articles users actually read
CREATE TABLE IF NOT EXISTS article_clicks (
                                              id BIGSERIAL PRIMARY KEY,
                                              user_id BIGINT NOT NULL REFERENCES app_users(id) ON DELETE CASCADE,
    article_id BIGINT NOT NULL REFERENCES articles(id) ON DELETE CASCADE,
    clicked_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
    );

-- Join Table for Subscriptions
CREATE TABLE IF NOT EXISTS user_subscriptions (
                                                  user_id BIGINT NOT NULL,
                                                  source_id BIGINT NOT NULL,
                                                  PRIMARY KEY (user_id, source_id),
    FOREIGN KEY (user_id) REFERENCES app_users(id) ON DELETE CASCADE,
    FOREIGN KEY (source_id) REFERENCES sources(id) ON DELETE CASCADE
    );

-- Join Table for Read All Sources
CREATE TABLE IF NOT EXISTS user_read_all_sources (
                                                     user_id BIGINT NOT NULL,
                                                     source_id BIGINT NOT NULL,
                                                     PRIMARY KEY (user_id, source_id),
    FOREIGN KEY (user_id) REFERENCES app_users(id) ON DELETE CASCADE,
    FOREIGN KEY (source_id) REFERENCES sources(id) ON DELETE CASCADE
    );

-- Articles Table
CREATE TABLE IF NOT EXISTS articles (
                                        id BIGSERIAL PRIMARY KEY,
                                        title TEXT NOT NULL,
                                        url TEXT UNIQUE NOT NULL,
                                        summary TEXT,
                                        content TEXT,
                                        source_name VARCHAR(255),
    published_at TIMESTAMP,
    vector vector(1024),
    processed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
    );

-- HNSW algorithm for cosine similarity
CREATE INDEX IF NOT EXISTS articles_vector_idx ON articles USING hnsw (vector vector_cosine_ops);
CREATE INDEX IF NOT EXISTS users_vector_idx ON app_users USING hnsw (interests_vector vector_cosine_ops);

