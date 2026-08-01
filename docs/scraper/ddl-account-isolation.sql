-- ============================================================
-- Scraper Platform: 계정 격리(테넌트) 스키마 변경
-- Database: scraper_platform
-- 실행: mysql -h 10.0.0.39 -u sh_user -p'SHpass1234!' scraper_platform < docs/scraper/ddl-account-isolation.sql
-- 주의: scraper 백엔드가 ddl-auto:validate → 반드시 배포(서비스 재시작) 전에 먼저 실행할 것
--       (deploy-backend.yml 워크플로우가 재시작 전 자동 실행함)
-- 설계: docs/scraper-schedule-requirements.md §1.6
-- 멱등: 재실행해도 안전함 (IF NOT EXISTS / IF EXISTS 사용)
-- ============================================================

-- 1. crawl_config에 소유 계정(account_id) 추가
--    account_id = auth 서비스 users.id (JWT sub) 와 동일 값 (크로스 DB라 FK 없음, BIGINT만 저장)
ALTER TABLE crawl_config
    ADD COLUMN IF NOT EXISTS account_id BIGINT NOT NULL DEFAULT 1 COMMENT '소유 계정 ID (auth users.id)' AFTER id;

-- 2. 전역 이름 유니크 → 계정별 이름 유니크로 변경 (다중 사용자 시 동일 이름 허용)
ALTER TABLE crawl_config
    DROP INDEX IF EXISTS uk_crawl_config_name;

ALTER TABLE crawl_config
    ADD UNIQUE INDEX IF NOT EXISTS uk_crawl_config_account_name (account_id, name);

-- 3. 기존 데이터를 첫 사용자(account_id=1)로 귀속
--    ※ 실제 첫 로그인 사용자 id가 1이 아닌 경우: GET /api/v1/auth/me 의 id 값으로 아래 1을 교체 후 재실행
UPDATE crawl_config SET account_id = 1 WHERE account_id = 0;
