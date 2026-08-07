# 004-260807-db-connection-pool-leak 오류 기록

## 개요
- **발생일**: 2026-08-07
- **환경**: Ubuntu 22.04, MariaDB 10.11, Spring Boot 3.4.4, HikariCP
- **심각도**: 🔴 Critical (DB 접속 불가)

## 1. 오류 현상
### 1.1 에러 메시지
```
SQL Error [08000]: Socket timeout when connecting to 127.0.0.1:36915
Read timed out
```

### 1.2 증상
- DBeaver에서 모든 쿼리 타임아웃 (30초 초과)
- `SELECT count(*) FROM job_postings`도 실행 안 됨
- `SHOW PROCESSLIST`에서 Sleep 연결 41개 확인

### 1.3 재현 단계
1. 크롤링 여러 번 실행
2. 시간 경과 후 DB 접속 시도
3. 타임아웃 발생

## 2. 원인 분석
### 2.1 근본 원인
HikariCP 연결 풀에서 연결을 반환하지 않고 방치 (연결 누수)

### 2.2 관련 코드
- 파일: `modules/scraper/backend/src/main/java/com/scraper/platform/service/CrawlExecutionService.java`
- 문제: `executeCrawl()` 메서드에서 `@Transactional` 미사용으로 각 Repository 호출마다 별도 연결 사용

### 2.3 발생 메커니즘
```
크롤링 시작
  ├─ for (siteConfig : siteConfigs)
  │    ├─ executeSiteCrawlJobs()     → 연결 1 점유
  │    ├─ saveJobPostings()          → 연결 1 점유 (REQUIRES_NEW)
  │    └─ saveCrawlLogComplete()     → 연결 1 점유
  └─ 연결 미반환 → Sleep 상태 유지 → 누적
```

### 2.4 데이터
- 서비스 4개 × 기본 풀 크기 10 = 최대 40개 연결
- 실제 사용량: 2~3개/서비스 → 나머지 7~8개는 불필요 대기

## 3. 해결 방법
### 3.1 즉시 조치
- Sleep 연결 모두 KILL
- 서비스 재시작으로 풀 초기화

### 3.2 코드 개선
1. **풀 설정 최적화**: maximum-pool-size: 5
2. **누수 탐지**: leak-detection-threshold: 30000
3. **트랜잭션 범위 조정**: executeCrawl()에 @Transactional 추가

### 3.3 최종 코드 변경
```yaml
# application.yml
spring:
  datasource:
    hikari:
      maximum-pool-size: 5
      minimum-idle: 2
      max-lifetime: 1800000
      idle-timeout: 600000
      leak-detection-threshold: 30000
```

## 4. 예방 방법
- 연결 풀 모니터링 (Prometheus + Grafana)
- 크롤링 시 연결 사용량 추적
- 정기적인 서비스 재시작 (연결 풀 초기화)

## 5. 참고 자료
- HikariCP 문서: https://github.com/brettwooldridge/HikariCP

---
*작성일: 2026-08-07*