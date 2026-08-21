-- ============================================================
-- resume 모듈 DDL v1 (2026-08-21)
-- DB: resume_platform
-- 설계: docs/plans/011-260821-resume-portfolio-integration-design.md §3.2
--
-- 주의: resume_platform DB에는 users 테이블이 없으므로
--       FK 제약 없이 user_id BIGINT + INDEX만 사용 (scraper_platform 관례 동일)
--
-- 실행:
--   mysql -h 10.0.0.39 -u sh_user -p resume_platform < docs/resume/ddl-resume-v1.sql
-- ============================================================

-- ------------------------------------------------------------
-- 1. 이력서 항목 테이블 (8개)
-- ------------------------------------------------------------

-- 인적사항 (1:user 1행, upsert 대상)
CREATE TABLE IF NOT EXISTS resume_profiles (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id         BIGINT NOT NULL UNIQUE,
    name            VARCHAR(50),
    email           VARCHAR(100),
    phone           VARCHAR(30),
    address         VARCHAR(200),
    birth_date      DATE,
    photo_url       VARCHAR(300),
    headline        VARCHAR(100),
    created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- 경력
CREATE TABLE IF NOT EXISTS resume_careers (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id         BIGINT NOT NULL,
    company         VARCHAR(100) NOT NULL,
    title           VARCHAR(100),
    start_date      DATE,
    end_date        DATE,
    description     TEXT,
    display_order   INT NOT NULL DEFAULT 0,
    created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_resume_careers_user (user_id)
);

-- 학력
CREATE TABLE IF NOT EXISTS resume_educations (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id         BIGINT NOT NULL,
    school          VARCHAR(100) NOT NULL,
    major           VARCHAR(100),
    degree          VARCHAR(20),
    start_date      DATE,
    end_date        DATE,
    status          VARCHAR(20),
    display_order   INT NOT NULL DEFAULT 0,
    created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_resume_educations_user (user_id)
);

-- 스킬
CREATE TABLE IF NOT EXISTS resume_skills (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id         BIGINT NOT NULL,
    name            VARCHAR(50) NOT NULL,
    level           VARCHAR(20),
    category        VARCHAR(50),
    display_order   INT NOT NULL DEFAULT 0,
    created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_resume_skills_user (user_id)
);

-- 자격증
CREATE TABLE IF NOT EXISTS resume_certificates (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id         BIGINT NOT NULL,
    name            VARCHAR(100) NOT NULL,
    issuer          VARCHAR(100),
    acquired_at     DATE,
    display_order   INT NOT NULL DEFAULT 0,
    created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_resume_certificates_user (user_id)
);

-- 프로젝트
CREATE TABLE IF NOT EXISTS resume_projects (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id         BIGINT NOT NULL,
    name            VARCHAR(100) NOT NULL,
    role            VARCHAR(100),
    start_date      DATE,
    end_date        DATE,
    description     TEXT,
    tech_stack      VARCHAR(300),
    link_url        VARCHAR(300),
    display_order   INT NOT NULL DEFAULT 0,
    created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_resume_projects_user (user_id)
);

-- 자기소개 항목
CREATE TABLE IF NOT EXISTS resume_introductions (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id         BIGINT NOT NULL,
    title           VARCHAR(100) NOT NULL,
    content         TEXT NOT NULL,
    display_order   INT NOT NULL DEFAULT 0,
    created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_resume_introductions_user (user_id)
);

-- 포트폴리오 작업물 (파일/링크)
CREATE TABLE IF NOT EXISTS resume_portfolio_items (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id         BIGINT NOT NULL,
    title           VARCHAR(100) NOT NULL,
    item_type       VARCHAR(20) NOT NULL,
    file_path       VARCHAR(300),
    link_url        VARCHAR(300),
    description     VARCHAR(500),
    display_order   INT NOT NULL DEFAULT 0,
    created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_resume_portfolio_user (user_id)
);

-- ------------------------------------------------------------
-- 2. 공통 모듈 테이블 (common 라이브러리 스캔 대상)
--    docs/common/modules.md 참고 — 서비스별 DB마다 생성 필요
-- ------------------------------------------------------------

CREATE TABLE IF NOT EXISTS common_schedule_config (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    module_name         VARCHAR(50) NOT NULL,
    task_name           VARCHAR(100) NOT NULL,
    cron                VARCHAR(100) DEFAULT '0 9 * * *',
    is_enabled          TINYINT(1) DEFAULT 1,
    last_executed_at    DATETIME,
    next_executed_at    DATETIME,
    created_at          DATETIME,
    updated_at          DATETIME
);

CREATE TABLE IF NOT EXISTS common_schedule_log (
    id                      BIGINT AUTO_INCREMENT PRIMARY KEY,
    schedule_config_id      BIGINT NOT NULL,
    status                  VARCHAR(20) NOT NULL DEFAULT 'RUNNING',
    total_count             INT DEFAULT 0,
    success_count           INT DEFAULT 0,
    error_count             INT DEFAULT 0,
    error_message           TEXT,
    started_at              DATETIME,
    completed_at            DATETIME,
    created_at              DATETIME,
    INDEX idx_schedule_log_config (schedule_config_id)
);

CREATE TABLE IF NOT EXISTS common_notification_config (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    module_name         VARCHAR(50) NOT NULL,
    event_type          VARCHAR(50) NOT NULL,
    notification_type   VARCHAR(20) NOT NULL,
    is_enabled          TINYINT(1) DEFAULT 1,
    recipient_email     VARCHAR(200),
    recipient_phone     VARCHAR(20),
    created_at          DATETIME,
    updated_at          DATETIME
);

CREATE TABLE IF NOT EXISTS common_notification_log (
    id                      BIGINT AUTO_INCREMENT PRIMARY KEY,
    notification_config_id  BIGINT NOT NULL,
    module_name             VARCHAR(50) NOT NULL,
    event_type              VARCHAR(50) NOT NULL,
    recipient               VARCHAR(200) NOT NULL,
    content                 TEXT,
    status                  VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    error_message           TEXT,
    sent_at                 DATETIME,
    created_at              DATETIME,
    INDEX idx_notification_log_config (notification_config_id)
);

CREATE TABLE IF NOT EXISTS push_subscription (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    account_id      BIGINT NOT NULL,
    endpoint        VARCHAR(500) NOT NULL,
    p256dh          VARCHAR(255) NOT NULL,
    auth_key        VARCHAR(255) NOT NULL,
    user_agent      VARCHAR(500),
    is_active       TINYINT(1) DEFAULT 1,
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_push_subscription_endpoint (endpoint),
    INDEX idx_push_subscription_account (account_id),
    INDEX idx_push_subscription_active (is_active)
);
