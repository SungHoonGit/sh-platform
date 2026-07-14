-- ============================================
-- MariaDB 파티셔닝 관리 스크립트
-- ============================================

-- 1. 현재 파티션 상태 확인
SELECT 
    TABLE_NAME,
    PARTITION_NAME,
    TABLE_ROWS,
    DATA_LENGTH,
    INDEX_LENGTH
FROM information_schema.PARTITIONS
WHERE TABLE_SCHEMA = 'scraper_platform'
  AND PARTITION_NAME IS NOT NULL
ORDER BY TABLE_NAME, PARTITION_NAME;

-- 2. 새 파티션 추가 (월별)
-- crawl_log
ALTER TABLE crawl_log REORGANIZE PARTITION p_future INTO (
    PARTITION p2026_11 VALUES LESS THAN (TO_DAYS('2026-12-01')),
    PARTITION p_future VALUES LESS THAN MAXVALUE
);

-- common_schedule_log
ALTER TABLE common_schedule_log REORGANIZE PARTITION p_future INTO (
    PARTITION p2026_11 VALUES LESS THAN (TO_DAYS('2026-12-01')),
    PARTITION p_future VALUES LESS THAN MAXVALUE
);

-- common_notification_log
ALTER TABLE common_notification_log REORGANIZE PARTITION p_future INTO (
    PARTITION p2026_11 VALUES LESS THAN (TO_DAYS('2026-12-01')),
    PARTITION p_future VALUES LESS THAN MAXVALUE
);

-- 3. 오래된 파티션 삭제 (데이터 보관 정책: 6개월)
ALTER TABLE crawl_log DROP PARTITION p2026_01;
ALTER TABLE common_schedule_log DROP PARTITION p2026_01;
ALTER TABLE common_notification_log DROP PARTITION p2026_01;

-- 4. 특정 파티션 데이터 조회
SELECT * FROM crawl_log PARTITION (p2026_07);

-- 5. 파티션별 RowCount 확인
SELECT 
    PARTITION_NAME,
    TABLE_ROWS
FROM information_schema.PARTITIONS
WHERE TABLE_SCHEMA = 'scraper_platform'
  AND TABLE_NAME = 'crawl_log'
  AND PARTITION_NAME IS NOT NULL;
