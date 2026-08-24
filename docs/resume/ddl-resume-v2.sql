-- ============================================================
-- resume 모듈 DDL v2 (2026-08-24)
-- DB: resume_platform  — Phase 5 (파일 업로드)
-- 설계: docs/plans/012-260824-resume-platform-roadmap-design.md §3.3
--
-- 실행:
--   mysql -h 10.0.0.39 -u sh_user -p resume_platform < docs/resume/ddl-resume-v2.sql
-- ============================================================

-- 업로드 파일 메타데이터 (실제 바이너리는 로컬 디스크: {upload-dir}/{userId}/{yyyyMM}/{uuid}.{ext})
CREATE TABLE IF NOT EXISTS resume_files (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id         BIGINT NOT NULL,
    original_name   VARCHAR(255) NOT NULL,
    stored_path     VARCHAR(300) NOT NULL,
    content_type    VARCHAR(100),
    size_bytes      BIGINT NOT NULL,
    created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_resume_files_user (user_id)
);
