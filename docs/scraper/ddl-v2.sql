-- ============================================================
-- Scraper Platform DDL v2 (표준 적용)
-- Database: scraper_platform
-- 표준: docs/architecture/db-standards/db-design-standard.md
-- ============================================================

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
    name VARCHAR(100) NOT NULL COMMENT '설정명 (예: Java 시니어 개발자)',
    description TEXT COMMENT '설정 설명',
    schedule VARCHAR(100) DEFAULT '0 9 * * *' COMMENT '크론 스케줄',
    retention_days INT DEFAULT 30 COMMENT '데이터 보존 기간 (일)',
    is_active BOOLEAN DEFAULT TRUE COMMENT '활성화 여부',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_crawl_config_name (name),
    INDEX idx_crawl_config_active (is_active)
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
-- 5. crawl_data (크롤링 데이터)
-- ============================================================
CREATE TABLE IF NOT EXISTS crawl_data (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    config_id BIGINT COMMENT '크롤링 설정 ID',
    category VARCHAR(100) COMMENT '카테고리명 (deprecated: config_id 사용)',
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
    INDEX idx_crawl_data_category (category),
    INDEX idx_crawl_data_source_url (source_url(191)),
    INDEX idx_crawl_data_crawled_at (crawled_at),
    INDEX idx_crawl_data_file_name (file_name),
    FULLTEXT INDEX ft_crawl_data_title (title)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================
-- 6. crawl_log (크롤링 로그)
-- ============================================================
CREATE TABLE IF NOT EXISTS crawl_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    config_id BIGINT COMMENT '크롤링 설정 ID',
    site_definition_id BIGINT COMMENT '사이트 ID',
    status ENUM('success', 'failed', 'partial') NOT NULL DEFAULT 'success' COMMENT '실행 결과',
    total_count INT DEFAULT 0 COMMENT '전체 수집 건수',
    new_count INT DEFAULT 0 COMMENT '신규 수집 건수',
    error_message TEXT COMMENT '에러 메시지',
    started_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '시작 시간',
    completed_at TIMESTAMP NULL COMMENT '완료 시간',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (config_id) REFERENCES crawl_config(id) ON DELETE SET NULL,
    FOREIGN KEY (site_definition_id) REFERENCES site_definition(id) ON DELETE SET NULL,
    INDEX idx_crawl_log_config (config_id),
    INDEX idx_crawl_log_site (site_definition_id),
    INDEX idx_crawl_log_started_at (started_at),
    INDEX idx_crawl_log_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================
-- 초기 데이터: site_definition
-- ============================================================
INSERT INTO site_definition (site_name, display_name, base_url) VALUES
('saramin', '사람인', 'https://www.saramin.co.kr'),
('jobkorea', '잡코리아', 'https://www.jobkorea.co.kr'),
('wanted', '원티드', 'https://www.wanted.co.kr'),
('jumpit', '점핏', 'https://www.jumpit.co.kr'),
('incruit', '인크루트', 'https://www.incruit.com'),
('remember', '리멤버', 'https://rememberapp.co.kr');

-- ============================================================
-- 초기 데이터: site_parameter_definition (사람인)
-- ============================================================
INSERT INTO site_parameter_definition (site_definition_id, param_key, param_name, param_type, is_required, options, display_order) VALUES
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
INSERT INTO site_parameter_definition (site_definition_id, param_key, param_name, param_type, is_required, options, display_order) VALUES
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
INSERT INTO site_parameter_definition (site_definition_id, param_key, param_name, param_type, is_required, options, display_order) VALUES
(3, 'keyword', '검색어', 'text', TRUE, NULL, 1),
(3, 'career', '경력', 'select', TRUE, '["신입","1~3년","3~5년","5~10년","10년↑"]', 2),
(3, 'education', '학력', 'select', FALSE, '["고졸","대졸","석사","박사"]', 3),
(3, 'tech_stack', '기술 스택', 'tags', TRUE, NULL, 4),
(3, 'job_type', '직무', 'select', TRUE, '["개발","기획","디자인","마케팅","영업","경영지원"]', 5),
(3, 'location', '지역', 'select', TRUE, '["서울","경기","인천","부산","대구","대전","광주","세종","강원","제주","전남","전북","경남","경북","충남","충북","기타"]', 6),
(3, 'employment_type', '고용형태', 'select', FALSE, '["정규직","계약직","인턴","프리랜서"]', 7);

-- ============================================================
-- 기존 카테고리 데이터 마이그레이션
-- ============================================================
INSERT INTO category (name, slug, description) VALUES
('Java', 'java', 'Java 관련 기술 문서'),
('React', 'react', 'React 관련 기술 문서'),
('Spring', 'spring', 'Spring Framework 관련 기술 문서'),
('DevOps', 'devops', 'DevOps 관련 기술 문서')
ON DUPLICATE KEY UPDATE name = name;
