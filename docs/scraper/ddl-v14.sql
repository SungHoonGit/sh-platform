-- ddl-v14.sql
-- Design 036: 차단 키워드 부분일치 옵션 (match_type ENUM)
-- exact=정확일치(기존 동작), contains=부분일치. 기존 행은 DEFAULT 'exact'로 보존된다.
-- Re-runnable (ADD COLUMN IF NOT EXISTS).

USE scraper_platform;

ALTER TABLE company_blacklist
    ADD COLUMN IF NOT EXISTS match_type ENUM('exact', 'contains') NOT NULL DEFAULT 'exact'
    AFTER company_name_normalized
    COMMENT '키워드 매칭 방식 (exact=정확일치, contains=부분일치)';
