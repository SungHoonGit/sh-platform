-- 채용공고 중복 제거용 테이블
-- DB 기반 중복 체크 + Viewer 데이터 소스

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
    crawled_at DATE NOT NULL COMMENT '수집 일자',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    -- 인덱스
    INDEX idx_job_postings_config (config_id),
    INDEX idx_job_postings_site (site_name),
    INDEX idx_job_postings_crawled_at (crawled_at),
    INDEX idx_job_postings_dedup_key (dedup_key),
    INDEX idx_job_postings_url (url(191)),
    UNIQUE INDEX uk_job_postings_dedup (dedup_key, crawled_at) COMMENT '같은 공고는 같은 날에만 중복 허용'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='채용공고 저장 + 중복 제거';
