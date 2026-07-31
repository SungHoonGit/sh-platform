-- ============================================================
-- 7. site_search_mapping (사이트별 검색 파라미터 매핑)
-- ============================================================
CREATE TABLE IF NOT EXISTS site_search_mapping (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    site_definition_id BIGINT NOT NULL,
    standard_key VARCHAR(50) NOT NULL COMMENT '공통 표준 키 (keyword, career, location, job_type)',
    url_param_name VARCHAR(100) NOT NULL COMMENT '사이트 URL 파라미터명 (stext, loc_cd, career_level 등)',
    value_type ENUM('direct', 'mapped', 'range') DEFAULT 'direct' COMMENT '값 변환 방식',
    value_mapping JSON COMMENT '값 매핑 {"3~5년":"5","5~10년":"8"} 또는 범위 설정',
    is_enabled BOOLEAN DEFAULT TRUE,
    display_order INT DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_site_mapping (site_definition_id, standard_key),
    FOREIGN KEY (site_definition_id) REFERENCES site_definition(id) ON DELETE CASCADE,
    INDEX idx_site_mapping_standard_key (standard_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================
-- 초기 데이터: site_search_mapping (사람인 - site_definition_id=1)
-- ============================================================
INSERT INTO site_search_mapping (site_definition_id, standard_key, url_param_name, value_type, value_mapping, display_order) VALUES
(1, 'keyword',   'stext',         'direct',  NULL, 1),
(1, 'career',    'career_level',  'mapped',  '{"신입":"1","경력":"2","1~3년":"3","3~5년":"5","5~10년":"8","10년이상":"12"}', 2),
(1, 'location',  'loc_cd',        'mapped',  '{"서울":"101000","경기":"102000","인천":"230000","부산":"260000","대구":"270000","대전":"300000","광주":"290000","세종":"360000","강원":"420000","제주":"500000","충남":"440000","충북":"430000","전남":"460000","전북":"450000","경남":"480000","경북":"470000"}', 3),
(1, 'job_type',  'cat_kewd',      'mapped',  '{"개발":"235","기획":"200","디자인":"260","마케팅":"300","영업":"400","연구개발":"350"}', 4),
(1, 'employment','job_type',      'mapped',  '{"정규직":"1","계약직":"2","인턴":"3","프리랜서":"4","파견직":"5"}', 5);

-- ============================================================
-- 초기 데이터: site_search_mapping (잡코리아 - site_definition_id=2)
-- ============================================================
INSERT INTO site_search_mapping (site_definition_id, standard_key, url_param_name, value_type, value_mapping, display_order) VALUES
(2, 'keyword',   'stext',          'direct',  NULL, 1),
(2, 'career',    'careerType',     'mapped',  '{"신입":"new","경력":"career","1~3년":"career","3~5년":"career","5~10년":"career","10년이상":"career"}', 2),
(2, 'location',  'local',          'mapped',  '{"서울":"I000","경기":"B000","인천":"K000","부산":"H000","대구":"F000","대전":"G000","광주":"L000","전남":"L000","세종":"1000","강원":"A000","제주":"N000","충남":"O000","충북":"P000","전북":"M000","경남":"C000","경북":"D000","울산":"J000"}', 3),
(2, 'job_type',  'dutyCtgr',       'mapped',  '{"서버/백엔드":"1003101","프론트엔드":"1003102","풀스택":"1003103","모바일":"1003104","인프라/DBA":"1003105","데이터/AI":"1003106","보안":"1003107","게임":"1003108","기타":"1003199"}', 4);

-- ============================================================
-- 초기 데이터: site_search_mapping (원티드 - site_definition_id=3)
-- ============================================================
INSERT INTO site_search_mapping (site_definition_id, standard_key, url_param_name, value_type, value_mapping, display_order) VALUES
(3, 'keyword',   'query',          'direct',  NULL, 1),
(3, 'career',    'years',          'mapped',  '{"신입":"0","1~3년":"1","3~5년":"3","5~10년":"5","10년이상":"10"}', 2),
(3, 'location',  'locations',      'mapped',  '{"서울":"seoul","경기":"gyeonggi","인천":"incheon","부산":"busan","대구":"daegu","대전":"daejeon","광주":"gwangju","세종":"sejong","강원":"gangwon","제주":"jeju"}', 3),
(3, 'job_type',  'job_group_ids',  'mapped',  '{"백엔드":"518","프론트엔드":"660","모바일":"519","데이터":"777","인프라":"669"}', 4);

-- ============================================================
-- 초기 데이터: site_search_mapping (리멤버 - site_definition_id=6)
-- ============================================================
INSERT INTO site_search_mapping (site_definition_id, standard_key, url_param_name, value_type, value_mapping, display_order) VALUES
(6, 'keyword',   'query',          'direct',  NULL, 1),
(6, 'career',    'min_experience', 'mapped',  '{"신입":"0","1~3년":"1","3~5년":"3","5~10년":"5","10년이상":"10"}', 2),
(6, 'location',  'sido',           'direct',  NULL, 3);
