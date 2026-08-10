# 006-2026-온디맨드 캐싱 가이드

## 개요
- **주제**: 온디맨드 캐싱 (On-Demand Caching) / Lazy Loading Cache
- **학습일**: 2026-08-09
- **수준**: 중급

## 1. 개념 설명

### 1.1 정의

온디맨드 캐싱(On-Demand Caching)은 데이터가 **필요한 시점에만** 캐시를 채우는 전략입니다. 다른 말로 **Lazy Loading Cache** 또는 **Cache-aside Pattern**이라고도 합니다.

**비유:**
- **Eager Loading (선로딩)**: 모든 메뉴를 미리 준비해놓는 식당
- **On-Demand Caching (지연로딩)**: 손님이 주문할 때마다 요리하는 식당 (하지만 한 번 요리한 것은 다음에 바로 제공)

### 1.2 왜 필요한가

| 문제 | 해결 |
|------|------|
| 모든 데이터를 미리 캐싱하면 시간·메모리 낭비 | 필요한 것만 캐싱 |
| 실시간 API 호출은 느림 | 캐시된 데이터로 즉시 응답 |
| 데이터가 자주 바뀜 | 요청 시점에 최신 데이터로 갱신 |

### 1.3 관련 개념

| 개념 | 설명 | 차이점 |
|------|------|--------|
| **Write-through** | 데이터 저장 시 캐시도 동시에 갱신 | 쓰기 성능 저하 |
| **Write-behind** | 캐시에 먼저 저장 후 비동기로 DB 갱신 | 데이터 유실 위험 |
| **Read-through** | 캐시 미스 시 DB에서 자동으로 가져옴 | 구현 복잡 |
| **Cache-aside (On-Demand)** | 애플리케이션이 직접 캐시 관리 | 가장 유연하고 간단 |
| **Eager Loading** | 미리 모든 데이터를 캐싱 | 초기 로딩 시간 김 |

## 2. 동작 원리

### 2.1 기본 흐름

```
1. 데이터 요청
   ↓
2. 캐시에서 확인 (Cache Hit?)
   ├── Yes → 캐시된 데이터 반환 (즉시)
   └── No  → DB/API에서 수집 (1-2초)
              ↓
            3. 캐시에 저장
              ↓
            4. 데이터 반환
```

### 2.2 코드 예시 (Java)

```java
@Service
public class CompanyRatingService {

    private final Map<String, CompanyRating> cache = new ConcurrentHashMap<>();
    private final CompanyRatingRepository repository;
    
    public CompanyRating getRating(String companyName) {
        // 1. 메모리 캐시 확인
        CompanyRating cached = cache.get(companyName);
        if (cached != null && !cached.isExpired()) {
            return cached;  // Cache Hit
        }
        
        // 2. DB 확인
        CompanyRating dbRating = repository.findByCompanyName(companyName);
        if (dbRating != null && !dbRating.isExpired()) {
            cache.put(companyName, dbRating);
            return dbRating;
        }
        
        // 3. 외부 API에서 수집 (On-Demand)
        CompanyRating newRating = scrapeFromJobPlanet(companyName);
        if (newRating != null) {
            repository.save(newRating);
            cache.put(companyName, newRating);
        }
        
        return newRating;
    }
}
```

### 2.3 코드 예시 (TypeScript)

```typescript
class CompanyRatingService {
  private cache = new Map<string, CompanyRating>();
  
  async getRating(companyName: string): Promise<CompanyRating | null> {
    // 1. 메모리 캐시 확인
    const cached = this.cache.get(companyName);
    if (cached && !this.isExpired(cached)) {
      return cached;  // Cache Hit
    }
    
    // 2. DB 확인
    const dbRating = await this.db.findByCompanyName(companyName);
    if (dbRating && !this.isExpired(dbRating)) {
      this.cache.set(companyName, dbRating);
      return dbRating;
    }
    
    // 3. 외부 API에서 수집 (On-Demand)
    const newRating = await this.scrapeFromJobPlanet(companyName);
    if (newRating) {
      await this.db.save(newRating);
      this.cache.set(companyName, newRating);
    }
    
    return newRating;
  }
}
```

## 3. 캐싱 전략 비교

### 3.1 시나리오별 추천

| 시나리오 | 추천 전략 | 이유 |
|----------|-----------|------|
| 데이터 변경 빈도 낮음 | Eager Loading | 미리 로딩해도 괜찮음 |
| 데이터 변경 빈도 높음 | On-Demand | 항상 최신 데이터 필요 |
| 대량 데이터 | On-Demand | 전체 로딩 시 메모리 부족 |
| 실시간성 중요 | On-Demand + TTL | 최신성과 성능 동시 만족 |

### 3.2 성능 비교

| 전략 | 첫 번째 요청 | 이후 요청 | 메모리 사용 |
|------|-------------|-----------|-------------|
| Eager Loading | 느림 (전체 로딩) | 빠름 | 높음 |
| On-Demand | 빠름 (필요할 때만) | 빠름 | 낮음 |
| No Cache | 느림 (매번 수집) | 느림 | 낮음 |

## 4. 구현 시 고려사항

### 4.1 TTL (Time To Live)

캐시된 데이터의 유효 기간 설정:

```java
@Data
public class CompanyRating {
    private String companyName;
    private Double score;
    private LocalDateTime lastUpdatedAt;
    
    public boolean isExpired() {
        // 7일 이후 만료
        return lastUpdatedAt.plusDays(7).isBefore(LocalDateTime.now());
    }
}
```

### 4.2 캐시 크기 제한

```java
// LRU 캐시 사용 예시
private final Map<String, CompanyRating> cache = 
    new LinkedHashMap<>(1000, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry eldest) {
            return size() > 1000;  // 최대 1000개
        }
    };
```

### 4.3 동시성 처리

```java
// ConcurrentHashMap 사용
private final Map<String, CompanyRating> cache = new ConcurrentHashMap<>();

// 또는 synchronized 사용
public synchronized CompanyRating getRating(String companyName) {
    // ...
}
```

## 5. 이 프로젝트에서의 적용

### 5.1 배경

- 채용공고 검색 시 기업 평점도 함께 표시하고 싶음
- 잡플래닛, 잡코리아, 사람인에서 기업 평점 수집 필요
- 매번 실시간으로 수집하면 검색 속도 느림

### 5.2 해결책: 온디맨드 캐싱

```
사용자가 "React 개발자" 검색
→ 채용공고 수집 (3초)
→ 각 기업명으로 DB 평점 확인 (즉시)
→ 삼성전자: 3.2점 (DB에 있음)
→ 스타트업A: 없음 → Background에서 수집 (1-2초 후 표시)
```

### 5.3 예상 효과

| 상황 | 성능 |
|------|------|
| 첫 검색 (새 기업) | 1-2초 추가 |
| 2회차 이후 | 즉시 (0초) |
| 서버 부하 | 낮음 (필요할 때만) |

## 6. 참고 자료

- [AWS Caching Strategies](https://docs.aws.amazon.com/elasticache/latest/red-ug/Strategies.html)
- [Redis Caching Patterns](https://redis.io/docs/manual/patterns/)
- [Java ConcurrentHashMap](https://docs.oracle.com/javase/8/docs/api/java/util/concurrent/ConcurrentHashMap.html)

---

*작성일: 2026-08-09*
