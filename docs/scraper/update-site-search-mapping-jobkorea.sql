-- ============================================================
-- 잡코리아 site_search_mapping 지역 매핑코드 수정
-- 기존 {"서울":"I000","경기":"I100",...} 은 v2 API 지역 매핑코드와 다름.
-- 실행: mysql -h 10.0.0.39 -u sh_user -p'SHpass1234!' scraper_platform < docs/scraper/update-site-search-mapping-jobkorea.sql
-- ============================================================

-- site_definition_id=2 가 잡코리아인 사이트의 location(local) 매핑만 교체
UPDATE site_search_mapping
SET value_mapping = JSON_OBJECT(
    '서울', 'I000', '경기', 'B000', '인천', 'K000', '부산', 'H000',
    '대구', 'F000', '대전', 'G000', '광주', 'L000', '전남', 'L000',
    '세종', '1000', '강원', 'A000', '제주', 'N000', '충남', 'O000',
    '충북', 'P000', '전북', 'M000', '경남', 'C000', '경북', 'D000',
    '울산', 'J000'
),
    updated_at = CURRENT_TIMESTAMP
WHERE site_definition_id = (SELECT id FROM site_definition WHERE site_name = 'jobkorea')
  AND standard_key = 'location';

-- 확인 쿼리
SELECT site_definition_id, standard_key, url_param_name, value_mapping
FROM site_search_mapping
WHERE site_definition_id = (SELECT id FROM site_definition WHERE site_name = 'jobkorea')
  AND standard_key = 'location';
