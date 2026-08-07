# 003-260807-2depth-duplicate-display-error 오류 기록

## 개요
- **발생일**: 2026-08-07
- **환경**: Windows, Java 21, Spring Boot 3.4.4
- **심각도**: 🟡 Warning (기능은 동작하나 사용자 혼란 유발)

## 1. 오류 현상
### 1.1 증상
- Viewer 수집 이력 트리에서 동일한 시간대(10:06, 17:40)가 08-07과 08-06 양쪽 날짜에 동시에 표시됨
- 2depth 시간대 클릭 시 의도치 않게 다른 날짜의 데이터도 포함될 수 있음

### 1.2 재현 단계
1. Viewer에서 수집 이력 트리 확인
2. 08-06, 08-07 날짜 펼치기
3. 동일 시간대(예: 17:40)가 양쪽에 표시됨

## 2. 원인 분석
### 2.1 근본 원인
`CrawlLogService.getLogsGroupedByDate()`에서 날짜별 로그 필터링 시 **±1일 범위**를 사용한 것이 원인

### 2.2 관련 코드
- 파일: `modules/scraper/backend/src/main/java/com/scraper/platform/service/CrawlLogService.java:54-55`
- 코드:
```java
// 변경 전 (문제 코드)
LocalDate logDateMin = date.minusDays(1);
LocalDate logDateMax = date.plusDays(1);
```

### 2.3 발생 메커니즘
- `crawl_log.started_at` (TIMESTAMP)을 `DATE()`로 변환하여 필터링
- ±1일 범위 사용 시:
  - 08-06 17:40에 시작된 크롤링 → 08-05~08-07 범위 매칭 → 08-06과 08-07 양쪽에 표시
  - 08-07 10:06에 시작된 크롤링 → 08-06~08-08 범위 매칭 → 08-06과 08-07 양쪽에 표시

## 3. 해결 방법
### 3.1 해결 과정
1. 원인 분석: ±1일 범위가 불필요하다는 것 확인
2. `crawl_log.started_at`과 `job_postings.crawled_at` 관계 분석
3. 자정을 넘긴 크롤링도 `DATE(started_at) = crawled_at`으로 정확히 매칭됨 확인
4. ±1일 제거하여 정확한 날짜 매칭으로 변경

### 3.2 최종 코드 변경
```java
// 변경 후
LocalDate logDateMin = date;
LocalDate logDateMax = date;
```

## 4. 예방 방법
- DATE 컬럼 필터링 시 ± 범위 사용 전 반드시 매칭 관계 확인
- TIMESTAMP와 DATE 간 변환 시 시차 고려 필요성 검토
- 유사 패턴 코드 리뷰 시 범위 필터링 로직 주의 깊게 확인

## 5. 참고 자료
- commit: `3e8f734` (fix: crawl_log ENUM RUNNING 추가 + DDL 마이그레이션 현행화 + 2depth 날짜 필터 수정)

---
*작성일: 2026-08-07*