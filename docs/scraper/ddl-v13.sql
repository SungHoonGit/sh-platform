-- ddl-v13.sql
-- Design 032: compound value_type for career filters (code + numeric range)
-- people/jobkorea career rows migrate from legacy flat mapping to compound.
-- Re-runnable (idempotent UPDATE with same values).

USE scraper_platform;

ALTER TABLE site_search_mapping
    MODIFY value_type ENUM('direct', 'mapped', 'range', 'compound') DEFAULT 'direct'
    COMMENT '값 변환 방식 (direct, mapped, range, compound)';

-- 사람인 career: exp_cd + exp_min/exp_max (site_definition_id=1)
UPDATE site_search_mapping
SET value_type     = 'compound',
    url_param_name = 'exp_cd',
    value_mapping  = '{"신입":{"exp_cd":"1"},"경력":{"exp_cd":"2"},"1~3년":{"exp_cd":"2","exp_min":"1","exp_max":"3"},"3~5년":{"exp_cd":"2","exp_min":"3","exp_max":"5"},"5~10년":{"exp_cd":"2","exp_min":"5","exp_max":"10"},"10년이상":{"exp_cd":"2","exp_min":"10"}}',
    updated_at     = CURRENT_TIMESTAMP
WHERE site_definition_id = 1
  AND standard_key = 'career';

-- 잡코리아 career: careerList + careerMin/careerMax (site_definition_id=2)
UPDATE site_search_mapping
SET value_type     = 'compound',
    url_param_name = 'careerList',
    value_mapping  = '{"신입":{"careerList":"1"},"경력":{"careerList":"2"},"1~3년":{"careerList":"2","careerMin":"1","careerMax":"3"},"3~5년":{"careerList":"2","careerMin":"3","careerMax":"5"},"5~10년":{"careerList":"2","careerMin":"5","careerMax":"10"},"10년이상":{"careerList":"2","careerMin":"10"}}',
    updated_at     = CURRENT_TIMESTAMP
WHERE site_definition_id = 2
  AND standard_key = 'career';
