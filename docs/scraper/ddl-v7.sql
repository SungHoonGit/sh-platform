-- ============================================================
-- scraper 모듈 DDL v7 (2026-08-30)
-- DB: scraper_platform  — 회사 차단 사유 마스터 테이블 + 시드
--
-- 실행:
--   mysql -h 10.0.0.39 -u sh_user -p scraper_platform < docs/scraper/ddl-v7.sql
--
-- 배경: 프론트 하드코딩 금지 원칙에 따라 차단 사유를 DB 코드화.
--       차단 사유 등록 시 자동완성/선택에 사용된다 (배포 없이 DB 추가 가능).
-- ============================================================

CREATE TABLE IF NOT EXISTS block_reasons (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    name       VARCHAR(100) NOT NULL,
    sort_order INT NOT NULL DEFAULT 0,
    active     TINYINT(1) NOT NULL DEFAULT 1,
    UNIQUE KEY uk_block_reasons_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ---------- 시드 (멱등) ----------
INSERT INTO block_reasons (name, sort_order, active)
SELECT * FROM (SELECT '연봉·복지 협상 불가', 1, 1) AS t
WHERE NOT EXISTS (SELECT 1 FROM block_reasons WHERE name='연봉·복지 협상 불가');

INSERT INTO block_reasons (name, sort_order, active)
SELECT * FROM (SELECT '중복·재공고', 2, 1) AS t
WHERE NOT EXISTS (SELECT 1 FROM block_reasons WHERE name='중복·재공고');

INSERT INTO block_reasons (name, sort_order, active)
SELECT * FROM (SELECT '지역·근무지 불일치', 3, 1) AS t
WHERE NOT EXISTS (SELECT 1 FROM block_reasons WHERE name='지역·근무지 불일치');

INSERT INTO block_reasons (name, sort_order, active)
SELECT * FROM (SELECT '직무·기술스택 불일치', 4, 1) AS t
WHERE NOT EXISTS (SELECT 1 FROM block_reasons WHERE name='직무·기술스택 불일치');

INSERT INTO block_reasons (name, sort_order, active)
SELECT * FROM (SELECT '경력·수준 불일치', 5, 1) AS t
WHERE NOT EXISTS (SELECT 1 FROM block_reasons WHERE name='경력·수준 불일치');

INSERT INTO block_reasons (name, sort_order, active)
SELECT * FROM (SELECT '신뢰도·평판 이슈', 6, 1) AS t
WHERE NOT EXISTS (SELECT 1 FROM block_reasons WHERE name='신뢰도·평판 이슈');

INSERT INTO block_reasons (name, sort_order, active)
SELECT * FROM (SELECT '지원 하지 않음', 7, 1) AS t
WHERE NOT EXISTS (SELECT 1 FROM block_reasons WHERE name='지원 하지 않음');
