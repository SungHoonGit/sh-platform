-- ============================================================
-- scraper 모듈 DDL v8 (2026-08-30)
-- DB: scraper_platform  — 차단 사유 카테고리화: 다대다 연결 + 회사유형 카테고리
--
-- 실행:
--   mysql -h 10.0.0.39 -u sh_user -p scraper_platform < docs/scraper/ddl-v8.sql
--
-- 배경: 한 회사 차단 시 여러 카테고리(회사유형 + 사유)를 동시에 선택/저장할 수 있도록
--       기존 단일 reason 문자열 → 다대다(block_reasons) 구조로 확장.
--       block_reasons 는 v7에서 이미 생성됨. 여기서는 category 컬럼 추가 + 시드 확장 + 연결 테이블.
-- ============================================================

-- ---------- 1. block_reasons 에 category 컬럼 추가 (멱등) ----------
SET @has_category = (SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'block_reasons' AND column_name = 'category');

SET @sql = IF(@has_category = 0,
    'ALTER TABLE block_reasons ADD COLUMN category VARCHAR(20) NOT NULL DEFAULT ''reason'' AFTER name',
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ---------- 2. 기존 reason 카테고리 시드에 category='reason' 명시 (멱등) ----------
UPDATE block_reasons SET category = 'reason', sort_order = 10 WHERE name = '연봉·복지 협상 불가';
UPDATE block_reasons SET category = 'reason', sort_order = 11 WHERE name = '중복·재공고';
UPDATE block_reasons SET category = 'reason', sort_order = 12 WHERE name = '지역·근무지 불일치';
UPDATE block_reasons SET category = 'reason', sort_order = 13 WHERE name = '직무·기술스택 불일치';
UPDATE block_reasons SET category = 'reason', sort_order = 14 WHERE name = '경력·수준 불일치';
UPDATE block_reasons SET category = 'reason', sort_order = 15 WHERE name = '신뢰도·평판 이슈';
UPDATE block_reasons SET category = 'reason', sort_order = 16 WHERE name = '지원 하지 않음';

-- ---------- 3. 회사유형 카테고리 시드 (멱등) ----------
INSERT INTO block_reasons (name, category, sort_order, active)
SELECT * FROM (SELECT '스타트업 X' AS name, 'company_type' AS category, 1 AS sort_order, 1 AS active) AS t
WHERE NOT EXISTS (SELECT 1 FROM block_reasons WHERE name='스타트업 X');

INSERT INTO block_reasons (name, category, sort_order, active)
SELECT * FROM (SELECT '스타트업' AS name, 'company_type' AS category, 2 AS sort_order, 1 AS active) AS t
WHERE NOT EXISTS (SELECT 1 FROM block_reasons WHERE name='스타트업');

INSERT INTO block_reasons (name, category, sort_order, active)
SELECT * FROM (SELECT '대기업' AS name, 'company_type' AS category, 3 AS sort_order, 1 AS active) AS t
WHERE NOT EXISTS (SELECT 1 FROM block_reasons WHERE name='대기업');

INSERT INTO block_reasons (name, category, sort_order, active)
SELECT * FROM (SELECT '중견기업' AS name, 'company_type' AS category, 4 AS sort_order, 1 AS active) AS t
WHERE NOT EXISTS (SELECT 1 FROM block_reasons WHERE name='중견기업');

INSERT INTO block_reasons (name, category, sort_order, active)
SELECT * FROM (SELECT '외국계 기업' AS name, 'company_type' AS category, 5 AS sort_order, 1 AS active) AS t
WHERE NOT EXISTS (SELECT 1 FROM block_reasons WHERE name='외국계 기업');

INSERT INTO block_reasons (name, category, sort_order, active)
SELECT * FROM (SELECT '공공기관·공기업' AS name, 'company_type' AS category, 6 AS sort_order, 1 AS active) AS t
WHERE NOT EXISTS (SELECT 1 FROM block_reasons WHERE name='공공기관·공기업');

INSERT INTO block_reasons (name, category, sort_order, active)
SELECT * FROM (SELECT '아웃소싱·파견' AS name, 'company_type' AS category, 7 AS sort_order, 1 AS active) AS t
WHERE NOT EXISTS (SELECT 1 FROM block_reasons WHERE name='아웃소싱·파견');

INSERT INTO block_reasons (name, category, sort_order, active)
SELECT * FROM (SELECT '블라인드·익명 평가' AS name, 'company_type' AS category, 8 AS sort_order, 1 AS active) AS t
WHERE NOT EXISTS (SELECT 1 FROM block_reasons WHERE name='블라인드·익명 평가');

-- ---------- 4. 다대다 연결 테이블 (blacklist ↔ block_reasons) ----------
CREATE TABLE IF NOT EXISTS blacklist_block_reason (
    blacklist_id    BIGINT NOT NULL,
    block_reason_id BIGINT NOT NULL,
    PRIMARY KEY (blacklist_id, block_reason_id),
    KEY idx_blacklist_block_reason_reason (block_reason_id),
    CONSTRAINT fk_bbr_blacklist FOREIGN KEY (blacklist_id) REFERENCES company_blacklist (id) ON DELETE CASCADE,
    CONSTRAINT fk_bbr_reason FOREIGN KEY (block_reason_id) REFERENCES block_reasons (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
