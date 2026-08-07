# DDL/DCL 마이그레이션 이력

## 개요
- **목적**: 스크래퍼 플랫폼 DB 변경 이력 관리
- **규칙**: 서버 SQL 실행 → `ddl-v2.sql` 현행화 → 이 문서에 이력 기록
- **Database**: `scraper_platform` (10.0.0.39)

---

## 2026-08-06

### 1. crawl_log.status ENUM에 RUNNING 추가
- **SQL**: `ALTER TABLE crawl_log MODIFY COLUMN status ENUM('RUNNING','SUCCESS','FAILED','PARTIAL') NOT NULL DEFAULT 'SUCCESS';`
- **이유**: 크롤링 시작 시 RUNNING 상태를 저장하기 위해
- **변경 전**: `ENUM('success','failed','partial')`
- **변경 후**: `ENUM('RUNNING','SUCCESS','FAILED','PARTIAL')`
- **적용 방법**: DBeaver 수동 실행

### 2. job_postings.crawl_log_id 컬럼 추가
- **SQL**:
  ```sql
  ALTER TABLE job_postings ADD COLUMN IF NOT EXISTS crawl_log_id BIGINT NULL COMMENT '수집 실행 ID (crawl_log 연결)';
  ALTER TABLE job_postings ADD INDEX IF NOT EXISTS idx_job_postings_crawl_log_id (crawl_log_id);
  ```
- **이유**: 크롤링 실행별 공고 필터링 지원
- **백필 SQL**: 기존 데이터를 crawled_at + site_name 기준으로 crawl_log 매칭
- **적용 방법**: deploy-backend.yml 자동 실행 (CI/CD)

### 3. crawl_config.schedule_icon 컬럼 추가
- **SQL**: `ALTER TABLE crawl_config ADD COLUMN IF NOT EXISTS schedule_icon VARCHAR(10) DEFAULT '🤖' COMMENT '스케줄 아이콘';`
- **이유**: 크롤러별 아이콘 커스터마이징 지원
- **적용 방법**: deploy-backend.yml 자동 실행 (CI/CD)

---

## 2026-08-07

### 4. crawl_log.search_criteria 컬럼 추가
- **SQL**: `ALTER TABLE crawl_log ADD COLUMN IF NOT EXISTS search_criteria JSON NULL COMMENT '실행 시점 검색 조건 {"keyword":"Java","career":"3~5년","location":"서울"}';`
- **이유**: 크롤링 시작 시 실행 시점의 검색 조건 저장하여 이력 추적 지원
- **적용 방법**: DBeaver 수동 실행 필요

### 5. crawl_stats 테이블 생성
- **SQL**:
  ```sql
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
  ```
- **이유**: 검색 조건별 통계 집계 및 분석 지원
- **적용 방법**: DBeaver 수동 실행 필요

---

*최종 업데이트: 2026-08-07*