# 007-260809-기업 평점 수집 기능 설계

## 개요
- **목적**: 채용공고 검색 시 기업 평점 정보를 함께 표시
- **범위**: 잡플래닛, 잡코리아, 사람인 기업 평점 수집
- **작성일**: 2026-08-09

## 1. 배경 및 이유

### 1.1 문제점
- 현재 채용공고 검색 시 기업 정보 부족
- 지원 전 기업 평판 확인을 위해 별도 사이트 방문 필요
- 사용자 경험 저하

### 1.2 목표
- 검색 결과에 기업 평점 즉시 표시
- 여러 소스의 평점을 합산하여 더 정확한 평점 제공
- 서버 부하 최소화 (온디맨드 캐싱 적용)

## 2. 요구 사항

### 2.1 기능 요구 사항
- [ ] FR-001: 잡플래닛 기업 평점 수집
- [ ] FR-002: 잡코리아 기업 평점 수집
- [ ] FR-003: 사람인 기업 평점 수집
- [ ] FR-004: 수집된 평점 DB 저장
- [ ] FR-005: 검색 결과에 평점 표시
- [ ] FR-006: 캐시 유효 기간 설정 (7일)

### 2.2 비기능 요구 사항
- 성능: 첫 조회 시 1-2초, 이후 즉시
- 보안: 공개 데이터만 수집 (로그인 불필요)
- 가용성: 특정 소스 실패 시 나머지로 대체

## 3. 설계

### 3.1 아키텍처

```
┌─────────────────────────────────────────────────────────┐
│                    Search/Viewer UI                      │
└─────────────────────────────────────────────────────────┘
                           ↓
┌─────────────────────────────────────────────────────────┐
│              CompanyRatingService (신규)                  │
│  - getRatings(List<String> companyNames)                │
│  - scrapeFromSources(String companyName)                │
└─────────────────────────────────────────────────────────┘
                           ↓
┌─────────────────────────────────────────────────────────┐
│           CompanyRatingRepository (신규)                 │
│  - findByCompanyName(String companyName)                │
│  - save(CompanyRating rating)                           │
└─────────────────────────────────────────────────────────┘
                           ↓
┌─────────────────────────────────────────────────────────┐
│              company_ratings 테이블 (신규)                │
│  - id (PK)                                              │
│  - company_name (UNIQUE)                                │
│  - jobplanet_score                                       │
│  - jobkorea_score                                        │
│  - saramin_score                                         │
│  - average_score                                         │
│  - last_updated_at                                       │
└─────────────────────────────────────────────────────────┘
```

### 3.2 데이터 모델

```sql
CREATE TABLE company_ratings (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    company_name VARCHAR(200) NOT NULL UNIQUE,
    jobplanet_score DECIMAL(2,1),
    jobkorea_score DECIMAL(2,1),
    saramin_score DECIMAL(2,1),
    average_score DECIMAL(2,1),
    last_updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    INDEX idx_company_name (company_name)
);
```

### 3.3 API 설계

#### 기업 평점 조회
- **GET** `/api/v1/company-ratings?companyNames=삼성전자,네이버`
- **응답**:
```json
{
  "ratings": [
    {
      "companyName": "삼성전자",
      "jobplanetScore": 3.2,
      "jobkoreaScore": 3.5,
      "saraminScore": 3.1,
      "averageScore": 3.3,
      "lastUpdatedAt": "2026-08-09T10:00:00"
    }
  ]
}
```

### 3.4 수집 대상 분석

| 소스 | URL 패턴 | 데이터 | 로그인 필요 |
|------|----------|--------|-------------|
| 잡플래닛 | `https://www.jobplanet.co.kr/companies/{id}/info/{name}` | 평점 점수 | ❌ |
| 잡코리아 | `https://www.jobkorea.co.kr/Company/{id}` | 평점 (있다면) | ❌ |
| 사람인 | `https://www.saramin.co.kr/zf_user/company/{id}` | 평점 (있다면) | ❌ |

### 3.5 수집 로직

```java
@Service
public class CompanyRatingService {

    private final Map<String, CompanyRating> cache = new ConcurrentHashMap<>();
    private final CompanyRatingRepository repository;
    private final SiteSearchMapper siteSearchMapper;
    
    public List<CompanyRating> getRatings(List<String> companyNames) {
        List<CompanyRating> result = new ArrayList<>();
        
        for (String companyName : companyNames) {
            // 1. 캐시 확인
            CompanyRating cached = cache.get(companyName);
            if (cached != null && !cached.isExpired()) {
                result.add(cached);
                continue;
            }
            
            // 2. DB 확인
            CompanyRating dbRating = repository.findByCompanyName(companyName);
            if (dbRating != null && !dbRating.isExpired()) {
                cache.put(companyName, dbRating);
                result.add(dbRating);
                continue;
            }
            
            // 3. Background에서 수집 (비동기)
            scrapeAndCache(companyName);
            result.add(new CompanyRating(companyName)); // 빈 객체 반환
        }
        
        return result;
    }
    
    @Async
    public void scrapeAndCache(String companyName) {
        CompanyRating rating = new CompanyRating(companyName);
        
        // 잡플래닛 평점 수집
        try {
            Double jobplanetScore = scrapeJobPlanet(companyName);
            rating.setJobplanetScore(jobplanetScore);
        } catch (Exception e) {
            log.warn("Failed to scrape JobPlanet for {}", companyName);
        }
        
        // 잡코리아 평점 수집
        try {
            Double jobkoreaScore = scrapeJobKorea(companyName);
            rating.setJobkoreaScore(jobkoreaScore);
        } catch (Exception e) {
            log.warn("Failed to scrape JobKorea for {}", companyName);
        }
        
        // 사람인 평점 수집
        try {
            Double saraminScore = scrapeSaramin(companyName);
            rating.setSaraminScore(saraminScore);
        } catch (Exception e) {
            log.warn("Failed to scrape Saramin for {}", companyName);
        }
        
        // 평균 계산
        rating.calculateAverage();
        
        // DB 저장
        repository.save(rating);
        
        // 캐시 업데이트
        cache.put(companyName, rating);
    }
}
```

## 4. 구현 계획

| 단계 | 내용 | 예상 기간 |
|------|------|-----------|
| Phase 1 | DB 스키마 설계 + Entity | 0.5일 |
| Phase 2 | CompanyRatingService 구현 | 1일 |
| Phase 3 | 잡플래닛 평점 수집기 구현 | 1일 |
| Phase 4 | 잡코리아/사람인 평점 수집기 구현 | 1일 |
| Phase 5 | Search API 연동 | 0.5일 |
| Phase 6 | UI 연동 + 테스트 | 1일 |
| **합계** | | **5일** |

## 5. 주의사항

- **Rate Limit**: 각 소스별 1초 대기
- **실패 처리**: 특정 소스 실패 시 나머지로 대체
- **데이터 정합성**: company_name 기준으로 중복 제거
- **법적 리스크**: 공개 데이터만 수집, 로그인 필요 데이터 수집 금지

## 6. 참고 자료

- [온디맨드 캐싱 가이드](../guides/006-2026-on-demand-caching-guide.md)

---

*작성일: 2026-08-09*
