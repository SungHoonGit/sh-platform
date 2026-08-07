# 006-260807-crawl-log-search-criteria-stats 설계 문서

## 개요
- **목적**: crawl_log에 실행 시점 검색 조건 저장 + 통계 테이블 확장 설계
- **범위**: DDL, 백엔드, 프론트 전체
- **작성일**: 2026-08-07
- **작성자**: AI Assistant

## 1. 배경 및 이유
- 현재 Viewer 헤더에 표시되는 검색 조건은 **현재 스케줄 설정** 기반
- 스케줄 중간 수정 시 이전 이력과 조건이 달라져 정확한 추적 불가
- **실행 시점의 검색 조건**을 히스토리에 저장하여 정확한 이력 관리 필요
- 향후 통계/분석을 위한 데이터 구조 확장 고려

## 2. 요구 사항
### 2.1 기능 요구 사항
- [ ] FR-001: crawl_log에 search_criteria JSON 컬럼 추가
- [ ] FR-002: 크롤링 시작 시 현재 paramValues를 search_criteria로 저장
- [ ] FR-003: Viewer에서 실행 시점 검색 조건 표시
- [ ] FR-004: 스케줄 변경 감지 시 "new!" 뱃지 표시
- [ ] FR-005: 검색 조건별 통계 집계 가능

### 2.2 비기능 요구 사항
- 성능: JSON 컬럼 인덱싱 불가 → 별도 통계 테이블 고려
- 보안: 기존 인증 체계 유지

## 3. 설계

### 3.1 데이터 모델 변경

#### 3.1.1 crawl_log 테이블 변경
```sql
ALTER TABLE crawl_log ADD COLUMN search_criteria JSON NULL COMMENT '실행 시점 검색 조건 {"keyword":"Java","career":"3~5년","location":"서울"}';
```

#### 3.1.2 crawl_stats 테이블 (신규)
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

### 3.2 데이터 흐름
```
크롤링 시작
  │
  ├─ CrawlExecutionService.executeCrawl()
  │    └─ crawlLogRepository.save(crawlLog)
  │         └─ search_criteria = siteConfigs의 paramValues 병합
  │
  ├─ 크롤링 완료
  │    └─ crawlLogRepository.completeLog()
  │
  └─ 통계 집계 (선택적)
       └─ crawlStatsRepository.save(stats)
```

### 3.3 API 변경

#### CrawlLogGroupResponse 변경
```java
public class CrawlLogGroupResponse {
    private LocalDate date;
    private int totalNewCount;
    private int totalRunCount;
    private List<CrawlRunGroup> runs;

    public static class CrawlRunGroup {
        private Long logId;
        private List<Long> logIds;
        private LocalDateTime startedAt;
        private String status;
        private int totalCount;
        private int newCount;
        private int siteCount;
        private List<String> siteNames;
        private SearchCriteria searchCriteria;  // 신규
    }
}

public class SearchCriteria {
    private String keyword;
    private String career;
    private String location;
}
```

### 3.4 프론트 변경

#### Viewer 헤더
```
[🤖 Java 시니어] | 2026-08-07 17:40
  ├─ [keyword: Java] [career: 3~5년] [location: 서울]  ← 실행 시점 조건
  └─ [전체] [사람인] [잡코리아] [원티드] [리멤버] 120건 [📥 Excel]
```

#### 2depth 시간대 표시
```
▼ 2026-08-07
   ✓ 17:40 (3개 사이트) 120건 [keyword: Java, career: 3~5년]
   ✓ 10:06 (2개 사이트) 85건 [keyword: Java, career: 신입] ← new!
```

### 3.5 변경 감지 로직
```java
// 이전 실행의 search_criteria와 비교
private boolean hasSearchCriteriaChanged(SearchCriteria current, SearchCriteria previous) {
    if (previous == null) return false;
    return !Objects.equals(current.getKeyword(), previous.getKeyword())
        || !Objects.equals(current.getCareer(), previous.getCareer())
        || !Objects.equals(current.getLocation(), previous.getLocation());
}
```

## 4. 구현 계획

| 단계 | 내용 | 상태 |
|------|------|------|
| Phase 1 | DDL: crawl_log.search_criteria 컬럼 추가 | 대기 |
| Phase 2 | DDL: crawl_stats 테이블 생성 (선택) | 대기 |
| Phase 3 | 백엔드: CrawlExecutionService에서 paramValues 저장 | 대기 |
| Phase 4 | 백엔드: CrawlLogGroupResponse에 searchCriteria 필드 추가 | 대기 |
| Phase 5 | 프론트: Viewer에서 실행 시점 조건 표시 | 대기 |
| Phase 6 | 프론트: new! 뱃지 표시 | 대기 |

## 5. 참고 자료
- 기존 코드: `CrawlExecutionService.java`, `CrawlLogGroupResponse.java`
- DDL: `docs/scraper/ddl-v2.sql`

---
*작성일: 2026-08-07*