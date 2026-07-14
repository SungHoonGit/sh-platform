# MariaDB 파티셔닝 가이드

## 개요

MariaDB 10.11의 파티셔닝 기능을 활용하여 로그 테이블의 성능과 관리를 최적화합니다.

## 적용 대상 테이블

| 테이블 | DB | 파티셔닝 키 | 설명 |
|--------|-----|-----------|------|
| `crawl_log` | scraper_platform | `started_at` | 크롤링 실행 이력 |
| `common_schedule_log` | scraper_platform | `created_at` | 스케줄 실행 이력 |
| `common_notification_log` | scraper_platform | `created_at` | 알림 발송 이력 |

## 파티셔닝 구조

### 월별 RANGE 파티셔닝
```
p2026_07  → 2026년 7월 데이터
p2026_08  → 2026년 8월 데이터
p2026_09  → 2026년 9월 데이터
p_future  → 그 이후 (기본 파티션)
```

### 설정 이유
1. **월별 관리**: 로그 데이터는 보통 월별로 관리
2. **보관 정책**: 6개월 보관 후 삭제
3. **조회 성능**: 파티션 단위로 스캔 범위 축소
4. **유연성**: 새 파티션 추가/삭제 용이

## DDL 예시

```sql
CREATE TABLE crawl_log (
    id BIGINT AUTO_INCREMENT,
    config_id BIGINT,
    site_definition_id BIGINT,
    status ENUM('SUCCESS','FAILED','PARTIAL') NOT NULL DEFAULT 'SUCCESS',
    total_count INT DEFAULT 0,
    new_count INT DEFAULT 0,
    error_message TEXT,
    started_at DATETIME NOT NULL,
    completed_at DATETIME NULL,
    created_at DATETIME NOT NULL,
    PRIMARY KEY (id, started_at),
    KEY idx_config_id (config_id),
    KEY idx_started_at (started_at),
    KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
PARTITION BY RANGE (TO_DAYS(started_at)) (
    PARTITION p2026_07 VALUES LESS THAN (TO_DAYS('2026-08-01')),
    PARTITION p2026_08 VALUES LESS THAN (TO_DAYS('2026-09-01')),
    PARTITION p2026_09 VALUES LESS THAN (TO_DAYS('2026-10-01')),
    PARTITION p_future VALUES LESS THAN MAXVALUE
);
```

## 관리 작업

### 1. 새 파티션 추가
```sql
ALTER TABLE crawl_log REORGANIZE PARTITION p_future INTO (
    PARTITION p2026_10 VALUES LESS THAN (TO_DAYS('2026-11-01')),
    PARTITION p_future VALUES LESS THAN MAXVALUE
);
```

### 2. 오래된 파티션 삭제
```sql
ALTER TABLE crawl_log DROP PARTITION p2026_01;
```

### 3. 특정 파티션 조회
```sql
SELECT * FROM crawl_log PARTITION (p2026_07);
```

### 4. 파티션 상태 확인
```sql
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
```

## 자동화

### 스크립트 위치
- SQL 관리: `scripts/partition-maintenance.sql`
- 자동 실행: `scripts/auto-partition.sh`

### Crontab 설정
```bash
# 매월 1일 자동 실행
0 0 1 * * /home/ubuntu/sh-platform/scripts/auto-partition.sh
```

### 자동화 동작
1. 다음 달 파티션 자동 생성
2. 6개월 전 파티션 자동 삭제
3. 로그 파일 기록: `/var/log/partition-maintenance.log`

## 주의사항

### 1. 파티셔닝 키 제약
- `TIMESTAMP` 타임스탬프는 시간대 의존적 → `DATETIME` 사용
- `DEFAULT CURRENT_TIMESTAMP` 사용 불가
- PRIMARY KEY에 파티셔닝 키 포함 필수

### 2. 성능 고려사항
- 파티션 키에 대한 조건 필수 (否则 전체 스캔)
- 파티션 수过多 시 메타데이터 오버헤드
- 일반적으로 100개 이하 권장

### 3. 데이터 보관 정책
- 현재: 6개월 보관
- 필요시 정책 변경 가능
- 삭제 전 백업 권장

## 관련 문서
- [MariaDB 공식 문서](https://mariadb.com/kb/en/partitioning/)
- [DB 설계 표준](./db-design-standard.md)
