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

*최종 업데이트: 2026-08-06*