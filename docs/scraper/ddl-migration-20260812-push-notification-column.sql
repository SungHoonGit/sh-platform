-- ============================================================
-- [MIGRATION] 2026-08-12: crawl_config.push_notification 컬럼 추가
-- ============================================================
ALTER TABLE crawl_config ADD COLUMN IF NOT EXISTS push_notification BOOLEAN DEFAULT FALSE COMMENT '브라우저 푸쉬 알림 활성화';
