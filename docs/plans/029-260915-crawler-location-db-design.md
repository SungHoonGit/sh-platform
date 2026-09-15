# 029-260915 크롤러 지역코드·매핑시드 DB 일원화 설계

## 개요
- **목적**: 크롤러가 사용하는 사이트별 지역코드 매핑과 매핑 시드 생성을 DB(DDL) 중심으로 일원화
- **범위**: `SiteSearchMappingInitializer`(Java 시드) 제거, Saramin 지역코드 DB 우선 조회, 단위 테스트 보강
- **작성일**: 2026-09-15
- **전제**: `site_search_mapping` 마스터는 이미 `docs/scraper/ddl-v2.sql`의 멱등 시드가 원본(설계 028로 추가된 region·site_definition 시드와 동일 원칙)

## 1. 배경 및 현재 상태 (조사 결과)

| 항목 | 상태 | 위치 |
|------|------|------|
| 사이트별 파라미터 매핑 마스터 | ✅ SQL 시드 원본 (`INSERT IGNORE`, ddl-v2.sql:340~371) | `docs/scraper/ddl-v2.sql` |
| 동일 데이터를 Java로 재시드 | ❌ `SiteSearchMappingInitializer` `@PostConstruct` + switch(사람인/잡코리아/원티드/리멤버 4개 case) | `service/SiteSearchMappingInitializer.java:49~91` |
| 잡코리아 지역코드 | ✅ DB 우선(`siteSearchMapper.getOrDefault("local")`) → switch fallback | `JobkoreaCrawler.java:61, 350~366` |
| 원티드·리멤버 매핑 | ✅ 전부 DB(`toSiteParams`) | WantedCrawler:41, RememberCrawler:41 |
| **사람인 지역코드** | ❌ **switch 하드코딩이 원본** (DB 미조회) — `loc_mcd=101000(서울)` 등 | `SaraminCrawler.java:156~161, 295~318` |

### 1.1 근본 상태
- `SiteSearchMappingInitializer`는 `mappingRepository.count()>0`이면 건너뛴다. 프로덕션은 ddl-v2 시드로 이미 채워져 있어 **실질적으로 실행된 적 없는 죽은 코드**이며, 매핑 switch 하드코딩도 안에만 존재.
- 사람인 크롤러만 예외적으로 지역코드를 switch로 직접 변환(DB 마스터와 별개). ddl-v2의 사람인 `location→loc_cd` 시드값과 동일 값이므로, **DB 우선+switch fallback**으로 바꿔도 동작 불변.

## 2. 요구 사항
- [ ] FR-001: `SiteSearchMappingInitializer` 제거(시드는 DDL 단일 소스)
- [ ] FR-002: `SiteSearchMapper.mapLocationCode(siteName, location)` 신설 — DB(`standard_key=location`) 우선, 없으면 빈 문자열
- [ ] FR-003: SaraminCrawler 지역코드 변환을 DB 우선(FR-002) → switch fallback 순으로 변경
- [ ] FR-004: 단위 테스트 — DB 매핑 사용 경로, DB 없음 fallback 경로, 기존 switch 로직 보존
- [ ] FR-005: 지역 코드 매핑 표시명(사람인 loc_mcd 등)은 DB 수정으로 배포 없이 변경 가능

### 2.2 비기능
- 실수집 크롤링 로직 변화 최소화(Db 우선, 미스 시 기존 동작 그대로)
- 시드·마스터는 `docs/scraper/ddl-v{2,9}` 원본 유지(추가 조정 없음)

## 3. 설계
### 3.1 초기자 제거
- 삭제: `service/SiteSearchMappingInitializer.java` (모델/레포/DDL은 유지)
- 영향: 없음 — prod는 count>0 → 기동 영향 0, 신규 환경은 DDL 시드로 동일 데이터 제공

### 3.2 SiteSearchMapper 확장
```java
/** standard_key=location 매핑의 value_mapping에서 값을 조회 */
public String mapLocationCode(String siteName, String location) {
    // findBySiteDefinition_SiteNameAndStandardKeyAndIsEnabledTrue(siteName, "location")
    //  → value_mapping JSON 파싱 → node.get(location).asText(), 없으면 ""
}
```
- 잡코리아 `locationCodes`와 Saramin이 공용 사용할 수도 있지만, 이번 범위는 Saramin만(잡코리아는 기존 DB-first 유지, 중복 리팩터링은 후속).

### 3.3 SaraminCrawler 변경
```java
private String resolveLocationCode(String location) {
    String code = siteSearchMapper.mapLocationCode(getSiteName(), location); // DB 우선
    return code.isEmpty() ? mapLocationCode(location) : code;                 // fallback switch
}
```
- 사용처: `appendLocationParams`(loc_mcd), `search`의 `locationFiltered` 판단(line 80) 두 곳만 교체
- `mapLocationCode` switch는 fallback으로 유지(기존 단위 테스트 `CrawlerMappingTest` 보존)

### 3.4 테스트
- `CrawlerMappingTest`: 기존 switch 테스트 유지 + `resolveLocationCode` new cases
- `SiteSearchMapperTest`: `mapLocationCode(siteName, location)` — 매핑 있음/없음/마스터 미존재

## 4. 구현 계획
| 단계 | 내용 | 검증 |
|------|------|------|
| 1 | initializer 삭제 + 참조 grep | 빌드 통과 |
| 2 | `SiteSearchMapper.mapLocationCode` + repo 메서드 + 테스트 | `:modules:scraper:backend:test` |
| 3 | Saramin `resolveLocationCode` 적용 + 테스트 | 전체 빌드 + CI 배포 |
| 4 | 실서버/일반 스케줄 크롤 무회귀 확인 | 로그 확인 |

## 5. 참고 자료
- `docs/scraper/ddl-v2.sql` site_search_mapping 시드(표준 key/url 파라미터/value_mapping 원본)
- `docs/plans/027-260915-project-cleanup-design.md` B2 후속, `028-260915...` 마스터 DB화

---
*작성일: 2026-09-15*