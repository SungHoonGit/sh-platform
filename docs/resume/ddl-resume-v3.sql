-- resume DDL v3 (Phase 6: 다중 이력서 + 지원 관리)
-- 문서 = 뷰 정의 (마스터 데이터는 기존 8테이블 그대로, 012/013 설계 참조)

CREATE TABLE IF NOT EXISTS resume_documents (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id        BIGINT NOT NULL,
    title          VARCHAR(100) NOT NULL,
    template_code  VARCHAR(20) NOT NULL DEFAULT 'CLASSIC',
    is_primary     BOOLEAN NOT NULL DEFAULT FALSE,
    section_config LONGTEXT NOT NULL,
    created_at     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_resume_documents_user (user_id)
);

CREATE TABLE IF NOT EXISTS resume_applications (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id         BIGINT NOT NULL,
    posting_id      BIGINT NULL,
    company_name    VARCHAR(100) NOT NULL,
    posting_title   VARCHAR(200) NOT NULL,
    posting_url     VARCHAR(500) NULL,
    apply_channel   VARCHAR(20) NOT NULL DEFAULT 'LINK',
    applied_at      DATE NULL,
    status          VARCHAR(20) NOT NULL DEFAULT 'PREPARING',
    document_id     BIGINT NULL,
    memo            TEXT NULL,
    created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_resume_applications_user_status (user_id, status),
    INDEX idx_resume_applications_user_date (user_id, applied_at)
);
