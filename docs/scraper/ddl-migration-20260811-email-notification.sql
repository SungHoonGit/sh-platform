-- ============================================================
-- [MIGRATION] 2026-08-11: crawl_config 이메일 알림 설정 추가
-- ============================================================
ALTER TABLE crawl_config ADD COLUMN IF NOT EXISTS email_notification BOOLEAN DEFAULT FALSE COMMENT '이메일 알림 활성화';
ALTER TABLE crawl_config ADD COLUMN IF NOT EXISTS recipient_email VARCHAR(200) NULL COMMENT '알림 수신 이메일';

-- ============================================================
-- [MIGRATION] 2026-08-11: job_postings.tech 컬럼 확장
-- ============================================================
ALTER TABLE job_postings MODIFY COLUMN tech TEXT NULL COMMENT '기술 스택';
