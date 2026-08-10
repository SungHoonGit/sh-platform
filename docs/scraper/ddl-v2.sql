-- ============================================================
-- Scraper Platform DDL v2 (통합 버전)
-- Database: scraper_platform
-- 실행: mysql -h 10.0.0.39 -u sh_user -p'SHpass1234!' scraper_platform < docs/scraper/ddl-v2.sql
-- ============================================================

-- ============================================================
-- [MIGRATION] 2026-08-06: crawl_log.status ENUM에 RUNNING 추가
-- ============================================================
-- 기존 DB: ENUM('success','failed','partial') → ENUM('RUNNING','SUCCESS','FAILED','PARTIAL')
ALTER TABLE crawl_log MODIFY COLUMN status ENUM('RUNNING','SUCCESS','FAILED','PARTIAL') NOT NULL DEFAULT 'SUCCESS';

-- ============================================================
-- [MIGRATION] 2026-08-06: job_postings.crawl_log_id 추가
-- ============================================================
-- 신규 테이블 생성 시 위 CREATE TABLE에 포함됨
-- 기존 테이블에 추가 시 아래 쿼리 실행:

-- 1) 컬럼 추가
ALTER TABLE job_postings ADD COLUMN IF NOT EXISTS crawl_log_id BIGINT NULL COMMENT '수집 실행 ID (crawl_log 연결)';

-- 2) 인덱스 추가
ALTER TABLE job_postings ADD INDEX IF NOT EXISTS idx_job_postings_crawl_log_id (crawl_log_id);

-- 3) 기존 데이터 백필: crawled_at 날짜 + site_name으로 crawl_log 매칭
--    (같은 날짜+사이트의 가장 최근 crawl_log 1건으로 연결)
--    collation 충돌 방지를 위해 COLLATE 명시
UPDATE job_postings jp
INNER JOIN (
    SELECT jp2.id AS posting_id, cl.id AS log_id
    FROM job_postings jp2
    INNER JOIN crawl_log cl
        ON cl.config_id = jp2.config_id
        AND DATE(cl.started_at) = jp2.crawled_at
    INNER JOIN site_definition sd
        ON sd.id = cl.site_definition_id
        AND sd.site_name COLLATE utf8mb4_unicode_ci = jp2.site_name COLLATE utf8mb4_unicode_ci
    WHERE jp2.crawl_log_id IS NULL
    GROUP BY jp2.id, cl.id
) matched ON jp.id = matched.posting_id
SET jp.crawl_log_id = matched.log_id;

-- ============================================================
-- [MIGRATION] 2026-08-06: crawl_config.schedule_icon 추가
-- ============================================================
ALTER TABLE crawl_config ADD COLUMN IF NOT EXISTS schedule_icon VARCHAR(10) DEFAULT '🤖' COMMENT '스케줄 아이콘';

-- ============================================================
-- [MIGRATION] 2026-08-07: crawl_log.search_criteria 추가
-- ============================================================
-- 크롤링 시작 시 실행 시점의 검색 조건 저장
ALTER TABLE crawl_log ADD COLUMN IF NOT EXISTS search_criteria JSON NULL COMMENT '실행 시점 검색 조건 {"keyword":"Java","career":"3~5년","location":"서울"}';

-- ============================================================
-- [MIGRATION] 2026-08-08: crawl_log.source 추가
-- ============================================================
-- 실행 출처 (수동 실행 / 스케줄 실행)
ALTER TABLE crawl_log ADD COLUMN IF NOT EXISTS source ENUM('MANUAL','SCHEDULE') NOT NULL DEFAULT 'MANUAL' COMMENT '실행 출처 (MANUAL: 수동, SCHEDULE: 스케줄)';

-- ============================================================
-- [MIGRATION] 2026-08-08: crawl_log.batch_id 추가
-- ============================================================
-- 한 번의 크롤링 실행(수동/스케줄)에 속한 사이트별 로그를 묶기 위한 실행 배치 식별자
ALTER TABLE crawl_log ADD COLUMN IF NOT EXISTS batch_id VARCHAR(36) NULL COMMENT '크롤링 실행 배치 ID (한 실행 내 사이트별 로그 그룹핑)';
ALTER TABLE crawl_log ADD INDEX IF NOT EXISTS idx_crawl_log_batch_id (batch_id);

-- 기존 데이터 백필: 같은 config에서 2분 이내 연속 실행은 같은 배치로 묶는다
SET @batch_num := 0;
SET @prev_config := NULL;
SET @prev_time := NULL;
CREATE TEMPORARY TABLE tmp_batch_assign AS
SELECT id,
       IF(@prev_config = config_id AND TIMESTAMPDIFF(SECOND, @prev_time, started_at) <= 120,
          @batch_num, @batch_num := @batch_num + 1) AS batch_num,
       @prev_config := config_id AS cur_config,
       @prev_time := started_at AS cur_time
FROM (SELECT id, config_id, started_at FROM crawl_log WHERE batch_id IS NULL ORDER BY config_id, started_at) t;

CREATE TEMPORARY TABLE tmp_batch_uuid AS
SELECT batch_num, UUID() AS batch_uuid FROM (SELECT DISTINCT batch_num FROM tmp_batch_assign) d;

UPDATE crawl_log cl
JOIN tmp_batch_assign ta ON cl.id = ta.id
JOIN tmp_batch_uuid tu ON ta.batch_num = tu.batch_num
SET cl.batch_id = tu.batch_uuid;

DROP TEMPORARY TABLE IF EXISTS tmp_batch_assign;
DROP TEMPORARY TABLE IF EXISTS tmp_batch_uuid;

-- ============================================================
-- [MIGRATION] 2026-08-07: crawl_stats 테이블 생성
-- ============================================================
-- 크롤링 통계 집계용 테이블
CREATE TABLE IF NOT EXISTS crawl_stats (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    config_id BIGINT NOT NULL,
    crawl_date DATE NOT NULL COMMENT '크롤링 날짜',
    keyword VARCHAR(100) COMMENT '검색 키워드',
    career VARCHAR(50) COMMENT '경력 조건',
    location VARCHAR(50) COMMENT '지역 조건',
    total_jobs INT DEFAULT 0 COMMENT '전체 수집 건수',
    new_jobs INT DEFAULT 0 COMMENT '신규 수집 건수',
    dup_jobs INT DEFAULT 0 COMMENT '중복 제외 건수',
    success_sites INT DEFAULT 0 COMMENT '성공 사이트 수',
    failed_sites INT DEFAULT 0 COMMENT '실패 사이트 수',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_crawl_stats_config_date (config_id, crawl_date),
    INDEX idx_crawl_stats_keyword (keyword),
    FOREIGN KEY (config_id) REFERENCES crawl_config(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='크롤링 통계';

-- ============================================================
-- 1. site_definition (사이트 정의)
-- ============================================================
CREATE TABLE IF NOT EXISTS site_definition (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    site_name VARCHAR(50) NOT NULL COMMENT '사이트 영문명 (saramin, jobkorea)',
    display_name VARCHAR(100) NOT NULL COMMENT '사이트 한글명 (사람인, 잡코리아)',
    base_url VARCHAR(200) COMMENT '사이트 기본 URL',
    is_enabled BOOLEAN DEFAULT TRUE COMMENT '활성화 여부',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_site_definition_name (site_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================
-- 2. site_parameter_definition (사이트별 파라미터 정의)
-- ============================================================
CREATE TABLE IF NOT EXISTS site_parameter_definition (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    site_definition_id BIGINT NOT NULL,
    param_key VARCHAR(50) NOT NULL COMMENT '파라미터 키 (keyword, career, education)',
    param_name VARCHAR(100) NOT NULL COMMENT '파라미터 한글명 (검색어, 경력, 학력)',
    param_type ENUM('text', 'select', 'hidden', 'tags') DEFAULT 'text' COMMENT '입력 방식',
    is_required BOOLEAN DEFAULT FALSE COMMENT '필수 여부',
    options JSON COMMENT 'select 타입인 경우 옵션 목록',
    default_value VARCHAR(200) COMMENT '기본값',
    display_order INT DEFAULT 0 COMMENT '표시 순서',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_site_param (site_definition_id, param_key),
    FOREIGN KEY (site_definition_id) REFERENCES site_definition(id) ON DELETE CASCADE,
    INDEX idx_param_key (param_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================
-- 3. crawl_config (크롤링 설정 - 메인)
-- ============================================================
CREATE TABLE IF NOT EXISTS crawl_config (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    account_id BIGINT NOT NULL DEFAULT 1 COMMENT '소유 계정 ID (auth users.id)',
    name VARCHAR(100) NOT NULL COMMENT '설정명 (예: Java 시니어 개발자)',
    description TEXT COMMENT '설정 설명',
    schedule VARCHAR(100) DEFAULT '0 9 * * *' COMMENT '크론 스케줄',
    schedule_icon VARCHAR(50) DEFAULT 'calendar' COMMENT '스케줄 아이콘 (calendar, clock, bell, rocket, star)',
    retention_days INT DEFAULT 30 COMMENT '데이터 보존 기간 (일)',
    is_active BOOLEAN DEFAULT TRUE COMMENT '활성화 여부',
    local_path VARCHAR(500) COMMENT '로컬 저장 경로',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_crawl_config_account_name (account_id, name),
    INDEX idx_crawl_config_active (is_active),
    INDEX idx_crawl_config_account (account_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================
-- 4. crawl_site_config (사이트별 크롤링 설정)
-- ============================================================
CREATE TABLE IF NOT EXISTS crawl_site_config (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    config_id BIGINT NOT NULL,
    site_definition_id BIGINT NOT NULL,
    is_enabled BOOLEAN DEFAULT TRUE COMMENT '사이트 활성화 여부',
    param_values JSON COMMENT '파라미터 값 {"keyword":"Java","career":"3~5년"}',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_config_site (config_id, site_definition_id),
    FOREIGN KEY (config_id) REFERENCES crawl_config(id) ON DELETE CASCADE,
    FOREIGN KEY (site_definition_id) REFERENCES site_definition(id) ON DELETE RESTRICT,
    INDEX idx_crawl_site_config_enabled (is_enabled)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================
-- 5. job_postings (채용공고 - 중복 제거용)
-- ============================================================
CREATE TABLE IF NOT EXISTS job_postings (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    config_id BIGINT NOT NULL,
    site_name VARCHAR(50) NOT NULL COMMENT 'saramin, jobkorea, wanted, remember',
    
    -- 공고 정보
    url VARCHAR(500) NOT NULL COMMENT '원본 URL',
    company VARCHAR(200) NOT NULL COMMENT '회사명',
    position VARCHAR(300) NOT NULL COMMENT '포지션명',
    career VARCHAR(100) NULL COMMENT '경력 요구사항',
    tech VARCHAR(500) NULL COMMENT '기술 스택',
    location VARCHAR(200) NULL COMMENT '근무지역',
    deadline VARCHAR(100) NULL COMMENT '마감일',
    
    -- 중복 체크용 해시
    dedup_key VARCHAR(64) NOT NULL COMMENT 'SHA256(company + position + location + site_name)',
    
    -- 메타데이터
    crawl_log_id BIGINT NULL COMMENT '수집 실행 ID (crawl_log 연결)',
    crawled_at DATE NOT NULL COMMENT '수집 일자',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    -- 인덱스
    INDEX idx_job_postings_config (config_id),
    INDEX idx_job_postings_site (site_name),
    INDEX idx_job_postings_crawled_at (crawled_at),
    INDEX idx_job_postings_crawl_log_id (crawl_log_id),
    INDEX idx_job_postings_dedup_key (dedup_key),
    INDEX idx_job_postings_url (url(191)),
    UNIQUE INDEX uk_job_postings_dedup (dedup_key, crawled_at) COMMENT '같은 공고는 같은 날에만 중복 허용'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='채용공고 저장 + 중복 제거';

-- ============================================================
-- 6. crawl_data (크롤링 메타데이터 - MD 파일 기반旧 방식, deprecated)
-- ============================================================
CREATE TABLE IF NOT EXISTS crawl_data (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    config_id BIGINT COMMENT '크롤링 설정 ID',
    category VARCHAR(100) COMMENT '카테고리명 (deprecated)',
    file_path VARCHAR(500) NOT NULL COMMENT 'MD 파일 경로',
    file_name VARCHAR(255) NOT NULL COMMENT '파일명',
    title VARCHAR(255) COMMENT '제목',
    source_url VARCHAR(500) COMMENT '원본 URL',
    source_site VARCHAR(100) COMMENT '소스 사이트',
    author VARCHAR(100) COMMENT '작성자',
    tags JSON COMMENT '태그 목록',
    file_size BIGINT COMMENT '파일 크기',
    crawled_at TIMESTAMP COMMENT '크롤링 시간',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (config_id) REFERENCES crawl_config(id) ON DELETE SET NULL,
    INDEX idx_crawl_data_config (config_id),
    INDEX idx_crawl_data_source_url (source_url(191)),
    INDEX idx_crawl_data_crawled_at (crawled_at),
    FULLTEXT INDEX ft_crawl_data_title (title)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='크롤링 메타데이터 (deprecated: job_postings 사용)';

-- ============================================================
-- 7. crawl_log (크롤링 로그)
-- ============================================================
CREATE TABLE IF NOT EXISTS crawl_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    config_id BIGINT COMMENT '크롤링 설정 ID',
    site_definition_id BIGINT COMMENT '사이트 ID',
    status ENUM('RUNNING', 'SUCCESS', 'FAILED', 'PARTIAL') NOT NULL DEFAULT 'SUCCESS' COMMENT '실행 결과',
    total_count INT DEFAULT 0 COMMENT '전체 수집 건수',
    new_count INT DEFAULT 0 COMMENT '신규 수집 건수',
    search_criteria JSON NULL COMMENT '실행 시점 검색 조건 {"keyword":"Java","career":"3~5년","location":"서울"}',
    batch_id VARCHAR(36) NULL COMMENT '크롤링 실행 배치 ID (한 실행 내 사이트별 로그 그룹핑)',
    source ENUM('MANUAL','SCHEDULE') NOT NULL DEFAULT 'MANUAL' COMMENT '실행 출처 (MANUAL: 수동, SCHEDULE: 스케줄)',
    error_message TEXT COMMENT '에러 메시지',
    started_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '시작 시간',
    completed_at TIMESTAMP NULL COMMENT '완료 시간',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (config_id) REFERENCES crawl_config(id) ON DELETE SET NULL,
    FOREIGN KEY (site_definition_id) REFERENCES site_definition(id) ON DELETE SET NULL,
    INDEX idx_crawl_log_config (config_id),
    INDEX idx_crawl_log_site (site_definition_id),
    INDEX idx_crawl_log_started_at (started_at),
    INDEX idx_crawl_log_status (status),
    INDEX idx_crawl_log_batch_id (batch_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================
-- 8. site_search_mapping (사이트별 검색 파라미터 매핑)
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
-- 초기 데이터: site_definition (면등: 이미 존재하면 무시)
-- ============================================================
INSERT IGNORE INTO site_definition (site_name, display_name, base_url) VALUES
('saramin', '사람인', 'https://www.saramin.co.kr'),
('jobkorea', '잡코리아', 'https://www.jobkorea.co.kr'),
('wanted', '원티드', 'https://www.wanted.co.kr'),
('jumpit', '점핏', 'https://www.jumpit.co.kr'),
('incruit', '인크루트', 'https://www.incruit.com'),
('remember', '리멤버', 'https://rememberapp.co.kr');

-- ============================================================
-- 초기 데이터: site_parameter_definition (사람인)
-- ============================================================
INSERT IGNORE INTO site_parameter_definition (site_definition_id, param_key, param_name, param_type, is_required, options, display_order) VALUES
(1, 'keyword', '검색어', 'text', TRUE, NULL, 1),
(1, 'career', '경력', 'select', TRUE, '["신입","1~3년","3~5년","5~10년","10년↑"]', 2),
(1, 'education', '학력', 'select', TRUE, '["고졸","대졸","석사","박사"]', 3),
(1, 'job_type', '직무', 'select', TRUE, '["개발","기획","디자인","마케팅","영업","경영지원","연구개발"]', 4),
(1, 'location', '지역', 'select', TRUE, '["서울","경기","인천","부산","대구","대전","광주","세종","강원","제주","전남","전북","경남","경북","충남","충북","기타"]', 5),
(1, 'employment', '고용형태', 'select', FALSE, '["정규직","계약직","인턴","프리랜서","파견직"]', 6),
(1, 'salary', '연봉', 'select', FALSE, '["2,000만원이하","2,000~3,000만원","3,000~4,000만원","4,000~5,000만원","5,000만원이상"]', 7);

-- ============================================================
-- 초기 데이터: site_parameter_definition (잡코리아)
-- ============================================================
INSERT IGNORE INTO site_parameter_definition (site_definition_id, param_key, param_name, param_type, is_required, options, display_order) VALUES
(2, 'keyword', '검색어', 'text', TRUE, NULL, 1),
(2, 'career', '경력', 'select', TRUE, '["신입","1~3년","3~5년","5~10년","10년↑"]', 2),
(2, 'education', '학력', 'select', TRUE, '["고졸","대졸","석사","박사"]', 3),
(2, 'company_type', '기업형태', 'select', TRUE, '["대기업","중견기업","소기업","스타트업","외국계","공공기관","기타"]', 4),
(2, 'employment_type', '고용형태', 'select', TRUE, '["정규직","계약직","인턴","프리랜서","파견직"]', 5),
(2, 'job_function', '직무', 'select', TRUE, '["서버/백엔드","프론트엔드","풀스택","모바일","인프라/DBA","데이터/AI","보안","게임","기타"]', 6),
(2, 'location', '지역', 'select', TRUE, '["서울","경기","인천","부산","대구","대전","광주","세종","강원","제주","전남","전북","경남","경북","충남","충북","기타"]', 7),
(2, 'salary', '연봉', 'select', FALSE, '["2,000만원이하","2,000~3,000만원","3,000~4,000만원","4,000~5,000만원","5,000만원이상"]', 8);

-- ============================================================
-- 초기 데이터: site_parameter_definition (원티드)
-- ============================================================
INSERT IGNORE INTO site_parameter_definition (site_definition_id, param_key, param_name, param_type, is_required, options, display_order) VALUES
(3, 'keyword', '검색어', 'text', TRUE, NULL, 1),
(3, 'career', '경력', 'select', TRUE, '["신입","1~3년","3~5년","5~10년","10년↑"]', 2),
(3, 'education', '학력', 'select', FALSE, '["고졸","대졸","석사","박사"]', 3),
(3, 'tech_stack', '기술 스택', 'tags', TRUE, NULL, 4),
(3, 'job_type', '직무', 'select', TRUE, '["개발","기획","디자인","마케팅","영업","경영지원"]', 5),
(3, 'location', '지역', 'select', TRUE, '["서울","경기","인천","부산","대구","대전","광주","세종","강원","제주","전남","전북","경남","경북","충남","충북","기타"]', 6),
(3, 'employment_type', '고용형태', 'select', FALSE, '["정규직","계약직","인턴","프리랜서"]', 7);

-- ============================================================
-- 초기 데이터: site_search_mapping (사람인 - site_definition_id=1)
-- ============================================================
INSERT IGNORE INTO site_search_mapping (site_definition_id, standard_key, url_param_name, value_type, value_mapping, display_order) VALUES
(1, 'keyword',   'stext',         'direct',  NULL, 1),
(1, 'career',    'career_level',  'mapped',  '{"신입":"1","경력":"2","1~3년":"3","3~5년":"5","5~10년":"8","10년이상":"12"}', 2),
(1, 'location',  'loc_cd',        'mapped',  '{"서울":"101000","경기":"102000","인천":"230000","부산":"260000","대구":"270000","대전":"300000","광주":"290000","세종":"360000","강원":"420000","제주":"500000","충남":"440000","충북":"430000","전남":"460000","전북":"450000","경남":"480000","경북":"470000"}', 3),
(1, 'job_type',  'cat_kewd',      'mapped',  '{"개발":"235","기획":"200","디자인":"260","마케팅":"300","영업":"400","연구개발":"350"}', 4),
(1, 'employment','job_type',      'mapped',  '{"정규직":"1","계약직":"2","인턴":"3","프리랜서":"4","파견직":"5"}', 5);

-- ============================================================
-- 초기 데이터: site_search_mapping (잡코리아 - site_definition_id=2)
-- ============================================================
INSERT IGNORE INTO site_search_mapping (site_definition_id, standard_key, url_param_name, value_type, value_mapping, display_order) VALUES
(2, 'keyword',   'stext',          'direct',  NULL, 1),
(2, 'career',    'careerType',     'mapped',  '{"신입":"new","경력":"career","1~3년":"career","3~5년":"career","5~10년":"career","10년이상":"career"}', 2),
(2, 'location',  'local',          'mapped',  '{"서울":"I000","경기":"B000","인천":"K000","부산":"H000","대구":"F000","대전":"G000","광주":"L000","전남":"L000","세종":"1000","강원":"A000","제주":"N000","충남":"O000","충북":"P000","전북":"M000","경남":"C000","경북":"D000","울산":"J000"}', 3),
(2, 'job_type',  'dutyCtgr',       'mapped',  '{"서버/백엔드":"1003101","프론트엔드":"1003102","풀스택":"1003103","모바일":"1003104","인프라/DBA":"1003105","데이터/AI":"1003106","보안":"1003107","게임":"1003108","기타":"1003199"}', 4);

-- ============================================================
-- 초기 데이터: site_search_mapping (원티드 - site_definition_id=3)
-- ============================================================
INSERT IGNORE INTO site_search_mapping (site_definition_id, standard_key, url_param_name, value_type, value_mapping, display_order) VALUES
(3, 'keyword',   'query',          'direct',  NULL, 1),
(3, 'career',    'years',          'mapped',  '{"신입":"0","1~3년":"1","3~5년":"3","5~10년":"5","10년이상":"10"}', 2),
(3, 'location',  'locations',      'mapped',  '{"서울":"seoul","경기":"gyeonggi","인천":"incheon","부산":"busan","대구":"daegu","대전":"daejeon","광주":"gwangju","세종":"sejong","강원":"gangwon","제주":"jeju"}', 3),
(3, 'job_type',  'job_group_ids',  'mapped',  '{"백엔드":"518","프론트엔드":"660","모바일":"519","데이터":"777","인프라":"669"}', 4);

-- ============================================================
-- 초기 데이터: site_search_mapping (리멤버 - site_definition_id=6)
-- ============================================================
INSERT IGNORE INTO site_search_mapping (site_definition_id, standard_key, url_param_name, value_type, value_mapping, display_order) VALUES
(6, 'keyword',   'query',          'direct',  NULL, 1),
(6, 'career',    'min_experience', 'mapped',  '{"신입":"0","1~3년":"1","3~5년":"3","5~10년":"5","10년이상":"10"}', 2),
(6, 'location',  'sido',           'direct',  NULL, 3);

-- ============================================================
-- 리멤버 비활성화 (키워드 검색 미지원)
-- ============================================================
UPDATE site_definition SET is_enabled = FALSE WHERE site_name = 'remember';

-- ============================================================
-- 원티드 비활성화 (서버 IP 차단 - 데이터센터에서 API 접근 불가)
-- ============================================================
UPDATE site_definition SET is_enabled = FALSE WHERE site_name = 'wanted';

-- ============================================================
-- [MIGRATION] 2026-08-09: company_ratings 테이블 생성
-- ============================================================
-- 기업 평점 수집용 테이블 (잡플래닛, 잡코리아, 사람인)
CREATE TABLE IF NOT EXISTS company_ratings (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    company_name VARCHAR(200) NOT NULL COMMENT '회사명',
    jobplanet_score DOUBLE NULL COMMENT '잡플래닛 평점 (1.0~5.0)',
    jobkorea_score DOUBLE NULL COMMENT '잡코리아 평점 (1.0~5.0)',
    saramin_score DOUBLE NULL COMMENT '사람인 평점 (1.0~5.0)',
    average_score DOUBLE NULL COMMENT '평균 평점',
    last_updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '마지막 업데이트 시간',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_company_ratings_name (company_name),
    INDEX idx_company_ratings_average (average_score),
    INDEX idx_company_ratings_updated (last_updated_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='기업 평점 (잡플래닛, 잡코리아, 사람인)';
