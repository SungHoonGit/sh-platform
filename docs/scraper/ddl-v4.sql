-- scraper DDL v4: crawl_config.schedule 컬럼 확장
-- 구버전 테이블이 VARCHAR(20) 등으로 생성되어 다중 cron 라인(개행 구분) 저장 시
-- 'Data too long for column schedule' 오류 발생. 엔티티(length 500)와 정렬.

ALTER TABLE crawl_config MODIFY COLUMN schedule VARCHAR(500) DEFAULT '0 9 * * *' COMMENT '크론 스케줄 (개행=다중 표현식)';
