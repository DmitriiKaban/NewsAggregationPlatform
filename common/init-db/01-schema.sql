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
                                     id BIGINT PRIMARY KEY,
                                     username VARCHAR(255),
    first_name VARCHAR(255),
    last_name VARCHAR(255),
    interests_raw TEXT,
    interests_vector vector(1024),
    show_only_subscribed_sources BOOLEAN NOT NULL DEFAULT FALSE,
    registered_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    last_active_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    preferred_language VARCHAR(5)
    CHECK (preferred_language IN ('en', 'ro', 'ru'))
    );

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


CREATE TABLE IF NOT EXISTS user_subscriptions (
                                                  user_id BIGINT NOT NULL,
                                                  source_id BIGINT NOT NULL,
                                                  PRIMARY KEY (user_id, source_id),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (source_id) REFERENCES sources(id) ON DELETE CASCADE
    );

CREATE TABLE IF NOT EXISTS user_read_all_sources (
                                                     user_id BIGINT NOT NULL,
                                                     source_id BIGINT NOT NULL,
                                                     PRIMARY KEY (user_id, source_id),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (source_id) REFERENCES sources(id) ON DELETE CASCADE
    );

CREATE TABLE IF NOT EXISTS article_clicks (
                                              id BIGSERIAL PRIMARY KEY,
                                              user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    article_id BIGINT NOT NULL REFERENCES articles(id) ON DELETE CASCADE,
    clicked_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
    );

CREATE TABLE IF NOT EXISTS user_activity (
                               id BIGSERIAL PRIMARY KEY,
                               user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                               activity_date DATE NOT NULL DEFAULT CURRENT_DATE,
                               activity_count INT DEFAULT 1,
                               created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

                               UNIQUE(user_id, activity_date)
);

CREATE TABLE IF NOT EXISTS user_reactions (
                                              user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    article_id BIGINT NOT NULL REFERENCES articles(id) ON DELETE CASCADE,
    reaction_type VARCHAR(20) NOT NULL CHECK (reaction_type IN ('LIKE', 'DISLIKE')),
    reacted_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (user_id, article_id)
    );

CREATE TABLE analytics_events (
                                  id BIGSERIAL PRIMARY KEY,
                                  user_id BIGINT NOT NULL,
                                  event_type VARCHAR(255) NOT NULL,
                                  reference_id VARCHAR(255),
                                  metadata JSONB,
                                  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                                  CONSTRAINT fk_analytics_event_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_event_type ON analytics_events (event_type);
CREATE INDEX IF NOT EXISTS idx_user_time ON analytics_events (user_id, created_at);
CREATE INDEX IF NOT EXISTS idx_user_activity_date ON user_activity(activity_date);
CREATE INDEX IF NOT EXISTS idx_user_activity_user_date ON user_activity(user_id, activity_date);
CREATE INDEX IF NOT EXISTS idx_users_language ON users(preferred_language);
CREATE INDEX IF NOT EXISTS articles_vector_idx ON articles USING hnsw (vector vector_cosine_ops);
CREATE INDEX IF NOT EXISTS users_vector_idx ON users USING hnsw (interests_vector vector_cosine_ops);