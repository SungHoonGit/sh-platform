-- ============================================================
-- resume DDL v8 (Phase 9: 공유 링크, 설계 024)
-- DB: resume_platform (배포 시 .github/workflows에서 자동 실행)
--
-- resume_share_links: 문서별 공유 링크 (문서당 0 또는 1개)
-- 실행:
--   mysql -h 10.0.0.39 -u sh_user -p resume_platform < docs/resume/ddl-resume-v8.sql
-- ============================================================

CREATE TABLE IF NOT EXISTS resume_share_links (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    document_id   BIGINT NOT NULL,
    token         VARCHAR(64) NOT NULL,
    expires_at    DATETIME NULL,
    created_at    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uq_resume_share_links_token (token),
    UNIQUE KEY uq_resume_share_links_document (document_id),
    INDEX idx_resume_share_links_document (document_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;