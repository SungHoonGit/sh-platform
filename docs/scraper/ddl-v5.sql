-- ============================================================================
-- job_postings v5 (2026-08-26): 실시간 검색결과 스크랩 지원
-- 검색 결과는 크롤러 설정 없이 사용자가 직접 저장하므로
-- config_id NULL 허용 + 저장자 계정 컬럼 추가
-- ※ 매 배포 재실행되므로 멱등하게 작성 (MariaDB IF NOT EXISTS)
-- ============================================================================

ALTER TABLE job_postings MODIFY config_id BIGINT NULL;

ALTER TABLE job_postings
  ADD COLUMN IF NOT EXISTS saved_by_account_id BIGINT NULL AFTER crawl_log_id,
  ADD INDEX IF NOT EXISTS idx_job_postings_saved_by (saved_by_account_id);
