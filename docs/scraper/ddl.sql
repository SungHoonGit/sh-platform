-- ============================================================
-- Scraper Platform DDL
-- Database: scraper_platform
-- ============================================================

-- 1. 카테고리 테이블
CREATE TABLE IF NOT EXISTS category (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL COMMENT 카테고리 이름,
    slug VARCHAR(100) NOT NULL UNIQUE COMMENT URL용 슬러그,
    description TEXT COMMENT 카테고리 설명,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_slug (slug)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 2. 크롤링 데이터 테이블
CREATE TABLE IF NOT EXISTS crawl_data (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    category_id BIGINT NOT NULL COMMENT 카테고리 ID,
    file_path VARCHAR(500) NOT NULL COMMENT MD 파일 경로,
    file_name VARCHAR(255) NOT NULL COMMENT 파일명,
    title VARCHAR(255) COMMENT 제목,
    source_url VARCHAR(500) COMMENT 원본 URL,
    source_site VARCHAR(100) COMMENT 소스 사이트,
    author VARCHAR(100) COMMENT 작성자,
    tags JSON COMMENT 태그 목록,
    file_size BIGINT COMMENT 파일 크기,
    crawled_at TIMESTAMP COMMENT 크롤링 시간,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (category_id) REFERENCES category(id) ON DELETE CASCADE,
    INDEX idx_category (category_id),
    INDEX idx_source_url (source_url(191)),
    INDEX idx_crawled_at (crawled_at),
    INDEX idx_file_name (file_name),
    FULLTEXT INDEX idx_title (title)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 3. 모니터링 테이블
CREATE TABLE IF NOT EXISTS crawl_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    category_id BIGINT NOT NULL,
    status ENUM(success, failed, partial) NOT NULL DEFAULT success,
    total_count INT DEFAULT 0,
    new_count INT DEFAULT 0,
    error_message TEXT,
    started_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP NULL,
    FOREIGN KEY (category_id) REFERENCES category(id) ON DELETE CASCADE,
    INDEX idx_category (category_id),
    INDEX idx_started_at (started_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 4. 테스트 데이터 삽입
INSERT INTO category (name, slug, description) VALUES
    (Java, java, Java 관련 기술 문서),
    (React, react, React 관련 기술 문서),
    (Spring, spring, Spring Framework 관련 기술 문서),
    (DevOps, devops, DevOps 관련 기술 문서);
