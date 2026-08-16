-- V001__initial_schema.sql
-- Create app_user table
CREATE TABLE IF NOT EXISTS app_user (
    id UUID PRIMARY KEY,
    username VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    role_name VARCHAR(50) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Create stored_document table
CREATE TABLE IF NOT EXISTS stored_document (
    id UUID PRIMARY KEY,
    filename VARCHAR(255) NOT NULL,
    owner_user_id UUID NOT NULL REFERENCES app_user(id),
    status VARCHAR(50) NOT NULL,
    chunk_count INT DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (owner_user_id) REFERENCES app_user(id)
);

-- Create search_query_log table
CREATE TABLE IF NOT EXISTS search_query_log (
    id UUID PRIMARY KEY,
    query TEXT NOT NULL,
    username VARCHAR(255) NOT NULL,
    query_timestamp TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    latency_ms BIGINT,
    result_count INT,
    top_score DOUBLE PRECISION,
    average_score DOUBLE PRECISION,
    feedback_positive BOOLEAN,
    FOREIGN KEY (username) REFERENCES app_user(username)
);

-- Create web_crawler table for crawler sessions
CREATE TABLE IF NOT EXISTS web_crawler (
    id UUID PRIMARY KEY,
    seed_url VARCHAR(2000) NOT NULL,
    created_by_username VARCHAR(255) NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'IDLE',
    pages_crawled BIGINT DEFAULT 0,
    pages_queued BIGINT DEFAULT 0,
    max_depth INT DEFAULT 3,
    max_pages BIGINT DEFAULT 500,
    error_message VARCHAR(1000),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    started_at TIMESTAMP,
    paused_at TIMESTAMP,
    stopped_at TIMESTAMP,
    FOREIGN KEY (created_by_username) REFERENCES app_user(username)
);

-- Create web_crawled_content table for scraped pages
CREATE TABLE IF NOT EXISTS web_crawled_content (
    id UUID PRIMARY KEY,
    crawler_id UUID NOT NULL,
    url VARCHAR(2000) NOT NULL,
    title VARCHAR(500),
    raw_text TEXT,
    chunk_count INT DEFAULT 0,
    content_hash VARCHAR(64),
    scraped_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (crawler_id) REFERENCES web_crawler(id),
    UNIQUE(crawler_id, url)
);

-- Create indexes
CREATE INDEX IF NOT EXISTS idx_stored_document_owner ON stored_document(owner_user_id);
CREATE INDEX IF NOT EXISTS idx_search_query_log_username ON search_query_log(username);
CREATE INDEX IF NOT EXISTS idx_search_query_log_timestamp ON search_query_log(query_timestamp);
CREATE INDEX IF NOT EXISTS idx_web_crawler_username ON web_crawler(created_by_username);
CREATE INDEX IF NOT EXISTS idx_web_crawler_status ON web_crawler(status);
CREATE INDEX IF NOT EXISTS idx_web_crawled_content_crawler ON web_crawled_content(crawler_id);
CREATE INDEX IF NOT EXISTS idx_web_crawled_content_hash ON web_crawled_content(content_hash);
