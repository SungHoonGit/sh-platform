# Scraper 검색 파라미터 표준화 설계

> 사이트별 검색 파라미터를 표준 코드로 관리하고, 크롤러에 자동 적용하는 구조
> 작성일: 2026-07-28

---

## 1. 배경 및 문제점

### 1.1 현재 상황

크롤러별 검색 파라미터가 하드코딩되어 있고, 대부분의 파라미터가 실제 사이트 검색에 적용되지 않음.

| 사이트 | 키워드 | 경력 | 지역 | 직무 |
|--------|--------|------|------|------|
| 사람인 | `stext` ✅ | `career_level` ✅ | `loc_cd` ✅ (법정동코드 매핑) | `cat_kewd` ❌ (235 하드코딩) |
| 잡코리아 | `stext` ✅ | `careerType` ✅ | `local` ⚠️ (I000 형식, 부분적) | `dutyCtgr` ❌ (1003101 하드코딩) |
| 원티드 | `query` ✅ | `years` ✅ | `locations` ⚠️ (seoul, 확인 필요) | ❌ |
| 리멤버 | `query` ✅ | `min_experience` ✅ | `sido` ✅ (텍스트 전달) | ❌ |

### 1.2 핵심 문제

1. **location 파라미터 미적용** — 어떤 크롤러에서도 지역 필터가 URL에 반영 안 됨
2. **Wanted 키워드 죽은 코드** — `buildUrl()`에 keyword 블록이 비어있음
3. **Remember 키워드 미적용** — "API 미지원" 가정만으로 구현 안 됨
4. **`site_parameter_definition` Dead Code** — DB에는 있으나 크롤러 어디서도 참조 안 함
5. **Jobkorea 경력 매핑 단순** — 1~3년/3~5년/5~10년 전부 `"career"`로 동일
6. **지역 옵션 부족** — 프론트엔드에 7개 지역만 하드코딩

---

## 2. 목표

1. 각 사이트 검색 파라미터가 실제로 URL에 적용되도록 함
2. 사이트별 URL 파라미터명 + 값 매핑을 DB에서 관리
3. 경력/지역 선택 시 해당 사이트에 맞는 코드로 자동 변환
4. 법정동코드를 공통 키로 도입하여 확장성 확보
5. 프론트엔드에서 동적으로 드롭다운 렌더링

---

## 3. 사이트별 검색 파라미터 분석

### 3.1 사람인 (saramin)

**크롤링 URL**: `https://www.saramin.co.kr/zf_user/jobs/list/job-category`

| 파라미터 | URL 파라미터 | 설명 | 현재 상태 |
|----------|-------------|------|----------|
| 키워드 | `stext` | 검색어 (URL 인코딩) | ✅ 적용됨 |
| 경력 | `career_level` | 1=신입, 2=경력, 3=신입/경력 | ✅ 적용됨 (6개 값 매핑) |
| 지역 | `loc_cd` | 법정동코드 기반 (서울=101000) | ❌ 미적용 |
| 직무 | `cat_kewd` | 직무 카테고리 (IT개발=235) | ❌ 하드코딩 |
| 고용형태 | `job_type` | 1=정규직, 2=계약직 등 | ❌ 미적용 |

**참고**: 사람인 공식 API(`oapi.saramin.co.kr`)에서는 `loc_cd`, `job_mid_cd`, `job_cd`, `edu_lv` 등 지원

### 3.2 잡코리아 (jobkorea)

**크롤링 URL**: `https://www.jobkorea.co.kr/recruit/joblist`

| 파라미터 | URL 파라미터 | 설명 | 현재 상태 |
|----------|-------------|------|----------|
| 키워드 | `stext` | 검색어 | ✅ 적용됨 |
| 경력 | `careerType` | new=신입, career=경력 | ✅ 적용됨 (단순 2값) |
| 지역 | `local` | 지역 코드 | ❌ 미적용 |
| 직무 | `dutyCtgr` | 직무 카테고리 (IT개발=1003101) | ❌ 하드코딩 |

**참고**: 잡코리아 지역 선택 UI — 서울, 경기, 인천, 부산, 대구, 대전, 광주, 세종, 강원, 제주, 충남, 충북, 전남, 전북, 경남, 경북

### 3.3 원티드 (wanted)

**API**: `https://www.wanted.co.kr/api/v4/jobs`

| 파라미터 | URL 파라미터 | 설명 | 현재 상태 |
|----------|-------------|------|----------|
| 키워드 | `query` | 검색어 | ❌ 빈 블록 |
| 경력 | `years` | 최소 연차 (-1=무관, 0=신입) | ❌ 미사용 |
| 지역 | `locations` | 배열 (seoul, gyeonggi 등) | ❌ hardcoded "all" |
| 직무 | `category_tags` | 태그 ID 배열 | ❌ 미사용 |

**참고**: 원티드 OpenAPI v1에서 `category_tags`, `skill_tags`, `years`, `locations` 파라미터 지원 확인

### 3.4 리멤버 (remember)

**API**: `POST https://career-api.rememberapp.co.kr/job_postings/search`

| 파라미터 | Request Body | 설명 | 현재 상태 |
|----------|-------------|------|----------|
| 키워드 | `query` | 검색어 | ❌ "unsupported" 가정 |
| 경력 | `min_experience` | 최소 경력 (년) | ❌ 미사용 |
| 지역 | `sido` | 시도명 (텍스트) | ❌ 미사용 |

---

## 4. 아키텍처 설계

### 4.1 전체 흐름

```
[기존]
  CrawlSiteConfig.paramValues (JSON)
    → 크롤러가 직접 parseParams()
    → 하드코딩된 매핑 함수 (mapCareerCode 등)
    → URL构建

[변경]
  CrawlSiteConfig.paramValues (JSON)
    → SiteSearchMapper.toSiteParams(siteName, paramValues)
      → site_search_mapping 테이블 참조
      → 표준 키 → 사이트별 URL 파라미터 + 값으로 변환
    → 크롤러에 변환된 Map<String, String> 전달
    → 크롤러는 순수 HTTP 요청 + 응답 파싱만 담당
```

### 4.2 핵심 컴포넌트

```
SiteSearchMapper (서비스)
  ├── SiteSearchMappingRepository (DB 접근)
  │     └── site_search_mapping 테이블
  ├── SiteParameterDefinitionRepository (옵션 참조)
  │     └── site_parameter_definition 테이블
  └── toSiteParams(siteName, paramValues) → Map<String, String>

크롤러 (SaraminCrawler 등)
  └── search(siteConfig, siteParams) ← 변환된 파라미터 전달
```

---

## 5. DB 설계

### 5.1 새 테이블: site_search_mapping

```sql
-- ============================================================
-- 7. site_search_mapping (사이트별 검색 파라미터 매핑)
-- ============================================================
CREATE TABLE IF NOT EXISTS site_search_mapping (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    site_definition_id BIGINT NOT NULL,
    standard_key VARCHAR(50) NOT NULL COMMENT '공통 표준 키 (keyword, career, location, job_type)',
    url_param_name VARCHAR(100) NOT NULL COMMENT '사이트 URL 파라미터명 (stext, loc_cd, career_level 등)',
    value_type ENUM('direct', 'mapped', 'range') DEFAULT 'direct' COMMENT '값 변환 방식',
    value_mapping JSON COMMENT '값 매핑 {"3~5년":"5","5~10년":"8"} 또는 범위 설정',
    is_enabled BOOLEAN DEFAULT TRUE,
    display_order INT DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_site_mapping (site_definition_id, standard_key),
    FOREIGN KEY (site_definition_id) REFERENCES site_definition(id) ON DELETE CASCADE,
    INDEX idx_site_mapping_standard_key (standard_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

### 5.2 value_type 설명

| 타입 | 설명 | 예시 |
|------|------|------|
| `direct` | 값을 그대로 URL에 붙임 | 키워드: `"React"` → `stext=React` |
| `mapped` | `value_mapping` JSON으로 코드 변환 | 경력: `"3~5년"` → `career_level=5` |
| `range` | 범위 파라미터로 변환 | 원티드 years: `"3~5년"` → `years=3` |

### 5.3 초기 데이터

#### 사람인 (site_definition_id = 1)

```sql
INSERT INTO site_search_mapping (site_definition_id, standard_key, url_param_name, value_type, value_mapping, display_order) VALUES
(1, 'keyword',   'stext',         'direct',  NULL, 1),
(1, 'career',    'career_level',  'mapped',  '{"신입":"1","경력":"2","1~3년":"3","3~5년":"5","5~10년":"8","10년이상":"12"}', 2),
(1, 'location',  'loc_cd',        'mapped',  '{"서울":"101000","경기":"102000","인천":"230000","부산":"260000","대구":"270000","대전":"300000","광주":"290000","세종":"360000","강원":"420000","제주":"500000","충남":"440000","충북":"430000","전남":"460000","전북":"450000","경남":"480000","경북":"470000"}', 3),
(1, 'job_type',  'cat_kewd',      'mapped',  '{"개발":"235","기획":"200","디자인":"260","마케팅":"300","영업":"400","연구개발":"350"}', 4),
(1, 'employment','job_type',      'mapped',  '{"정규직":"1","계약직":"2","인턴":"3","프리랜서":"4","파견직":"5"}', 5);
```

#### 잡코리아 (site_definition_id = 2)

```sql
INSERT INTO site_search_mapping (site_definition_id, standard_key, url_param_name, value_type, value_mapping, display_order) VALUES
(2, 'keyword',   'stext',          'direct',  NULL, 1),
(2, 'career',    'careerType',     'mapped',  '{"신입":"new","경력":"career","1~3년":"career","3~5년":"career","5~10년":"career","10년이상":"career"}', 2),
(2, 'location',  'local',          'mapped',  '{"서울":"1","경기":"2","인천":"3","부산":"4","대구":"5","대전":"6","광주":"7","세종":"8","강원":"9","제주":"10","충남":"11","충북":"12","전남":"13","전북":"14","경남":"15","경북":"16"}', 3),
(2, 'job_type',  'dutyCtgr',       'mapped',  '{"서버/백엔드":"1003101","프론트엔드":"1003102","풀스택":"1003103","모바일":"1003104","인프라/DBA":"1003105","데이터/AI":"1003106","보안":"1003107","게임":"1003108","기타":"1003199"}', 4);
```

#### 원티드 (site_definition_id = 3)

```sql
INSERT INTO site_search_mapping (site_definition_id, standard_key, url_param_name, value_type, value_mapping, display_order) VALUES
(3, 'keyword',   'query',          'direct',  NULL, 1),
(3, 'career',    'years',          'mapped',  '{"신입":"0","1~3년":"1","3~5년":"3","5~10년":"5","10년이상":"10"}', 2),
(3, 'location',  'locations',      'mapped',  '{"서울":"seoul","경기":"gyeonggi","인천":"incheon","부산":"busan","대구":"daegu","대전":"daejeon","광주":"gwangju","세종":"sejong","강원":"gangwon","제주":"jeju"}', 3),
(3, 'job_type',  'job_group_ids',  'mapped',  '{"백엔드":"518","프론트엔드":"660","모바일":"519","데이터":"777","인프라":"669"}', 4);
```

#### 리멤버 (site_definition_id = 6)

```sql
INSERT INTO site_search_mapping (site_definition_id, standard_key, url_param_name, value_type, value_mapping, display_order) VALUES
(6, 'keyword',   'query',          'direct',  NULL, 1),
(6, 'career',    'min_experience', 'mapped',  '{"신입":"0","1~3년":"1","3~5년":"3","5~10년":"5","10년이상":"10"}', 2),
(6, 'location',  'sido',           'direct',  NULL, 3);
```

---

## 6. 법정동코드 표준화

### 6.1 표준 지역 코드 (시/도 수준)

| 지역명 | 법정동코드 | 사람인 loc_cd | 잡코리아 local | 원티드 locations |
|--------|-----------|-------------|---------------|-----------------|
| 서울 | 101000 | 101000 | 1 | seoul |
| 경기 | 102000 | 102000 | 2 | gyeonggi |
| 인천 | 230000 | 230000 | 3 | incheon |
| 부산 | 260000 | 260000 | 4 | busan |
| 대구 | 270000 | 270000 | 5 | daegu |
| 대전 | 300000 | 300000 | 6 | daejeon |
| 광주 | 290000 | 290000 | 7 | gwangju |
| 세종 | 360000 | 360000 | 8 | sejong |
| 강원 | 420000 | 420000 | 9 | gangwon |
| 제주 | 500000 | 500000 | 10 | jeju |
| 충남 | 440000 | 440000 | 11 | chungnam |
| 충북 | 430000 | 430000 | 12 | chungbuk |
| 전남 | 460000 | 460000 | 13 | jeonnam |
| 전북 | 450000 | 450000 | 14 | jeonbuk |
| 경남 | 480000 | 480000 | 15 | gyeongnam |
| 경북 | 470000 | 470000 | 16 | gyeongbuk |

### 6.2 확장 계획

시/군/구 수준으로 확장 시:
```json
{
  "101000": "서울",
  "101050": "서울 관악구",
  "101060": "서울 광진구",
  "101070": "서울 구로구",
  "101130": "서울 마포구"
}
```

---

## 7. 백엔드 구현

### 7.1 도메인 모델

```java
@Entity
@Table(name = "site_search_mapping")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SiteSearchMapping {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "site_definition_id", nullable = false)
    private SiteDefinition siteDefinition;

    @Column(name = "standard_key", nullable = false, length = 50)
    private String standardKey;

    @Column(name = "url_param_name", nullable = false, length = 100)
    private String urlParamName;

    @Enumerated(EnumType.STRING)
    @Column(name = "value_type", nullable = false, length = 20)
    private ValueType valueType;

    @Column(name = "value_mapping", columnDefinition = "JSON")
    private String valueMapping;

    @Column(name = "is_enabled")
    private Boolean isEnabled = true;

    @Column(name = "display_order")
    private Integer displayOrder = 0;

    public enum ValueType {
        direct, mapped, range
    }
}
```

### 7.2 Repository

```java
@Repository
public interface SiteSearchMappingRepository extends JpaRepository<SiteSearchMapping, Long> {
    List<SiteSearchMapping> findBySiteDefinitionIdAndIsEnabledTrueOrderByDisplayOrder(Long siteDefinitionId);
    List<SiteSearchMapping> findBySiteDefinition_SiteNameAndIsEnabledTrueOrderByDisplayOrder(String siteName);
}
```

### 7.3 서비스

```java
@Service
@RequiredArgsConstructor
public class SiteSearchMapper {

    private final SiteSearchMappingRepository mappingRepository;
    private final ObjectMapper objectMapper;

    /**
     * 표준 paramValues를 사이트별 URL 파라미터로 변환한다.
     * 예: {"career":"3~5년","location":"서울"} → {"career_level":"5","loc_cd":"101000"}
     */
    public Map<String, String> toSiteParams(String siteName, String paramValues) {
        Map<String, String> standardParams = parseParamValues(paramValues);
        List<SiteSearchMapping> mappings = mappingRepository
                .findBySiteDefinition_SiteNameAndIsEnabledTrueOrderByDisplayOrder(siteName);

        Map<String, String> siteParams = new LinkedHashMap<>();
        for (SiteSearchMapping mapping : mappings) {
            String value = standardParams.get(mapping.getStandardKey());
            if (value == null || value.isEmpty()) continue;

            String converted = convertValue(value, mapping);
            if (converted != null && !converted.isEmpty()) {
                siteParams.put(mapping.getUrlParamName(), converted);
            }
        }
        return siteParams;
    }

    private String convertValue(String value, SiteSearchMapping mapping) {
        return switch (mapping.getValueType()) {
            case direct -> value;
            case mapped -> mapValue(value, mapping.getValueMapping());
            case range -> mapRange(value, mapping.getValueMapping());
        };
    }

    private String mapValue(String value, String valueMappingJson) {
        if (valueMappingJson == null) return value;
        try {
            JsonNode node = objectMapper.readTree(valueMappingJson);
            JsonNode mapped = node.get(value);
            return mapped != null ? mapped.asText() : null;
        } catch (Exception e) {
            return null;
        }
    }

    private String mapRange(String value, String valueMappingJson) {
        // 범위 변환 로직 (예: "3~5년" → "3")
        if (valueMappingJson != null) return mapValue(value, valueMappingJson);
        String cleaned = value.replaceAll("[^0-9]", "");
        return cleaned.isEmpty() ? null : cleaned;
    }

    private Map<String, String> parseParamValues(String paramValues) {
        if (paramValues == null || paramValues.isEmpty()) return Map.of();
        try {
            JsonNode node = objectMapper.readTree(paramValues);
            Map<String, String> params = new HashMap<>();
            node.fields().forEachRemaining(e -> params.put(e.getKey(), e.getValue().asText()));
            return params;
        } catch (Exception e) {
            return Map.of();
        }
    }
}
```

### 7.4 크롤러 수정 (사람인 예시)

```java
// 기존
private String buildUrl(String keyword, String career, String location, String jobType, int page) {
    StringBuilder sb = new StringBuilder(BASE_URL);
    sb.append("?cat_kewd=235"); // 하드코딩
    if (!career.isEmpty()) {
        String careerCode = mapCareerCode(career); // 하드코딩 매핑
        sb.append("&career_level=").append(careerCode);
    }
    // ...
}

// 변경
private String buildUrl(Map<String, String> siteParams, int page) {
    StringBuilder sb = new StringBuilder(BASE_URL);
    sb.append("?");
    boolean first = true;
    for (Map.Entry<String, String> entry : siteParams.entrySet()) {
        if (!first) sb.append("&");
        sb.append(entry.getKey()).append("=")
          .append(URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8));
        first = false;
    }
    if (page > 1) sb.append("&page=").append(page);
    return sb.toString();
}
```

---

## 8. 프론트엔드 구현

### 8.1 동적 드롭다운

기존 하드코딩 상수를 제거하고 DB에서 로드:

```tsx
// 기존 (Search.tsx)
const CAREERS = ["전체", "경력무관", "1~3년", "3~5년", "5~10년", "10년 이상"];
const LOCATIONS = ["전체", "서울", "경기", "인천", "부산", "대구", "기타"];

// 변경
const { data: siteParams } = useQuery({
  queryKey: ["siteParams", selectedSite],
  queryFn: () => fetchSiteParameters(selectedSite),
  // → GET /scraper/sites/{id}/parameters
});

// siteParams에서 career, location 옵션 추출
const careers = siteParams?.find(p => p.paramKey === "career")?.options || [];
const locations = siteParams?.find(p => p.paramKey === "location")?.options || [];
```

### 8.2 지역 표시 개선

법정동코드를 표시명에서 분리:

```tsx
// "서울(101000)" → 표시: "서울", 값: "서울"
// options JSON에서 "(코드)" 부분을 제거하여 표시
const displayLocation = (option: string) => option.replace(/\(\d+\)/, '').trim();
```

---

## 9. 구현 단계

| 단계 | 내용 | 상태 |
|------|------|------|
| 1 | `site_search_mapping` DDL + 초기 데이터 | 🔜 예정 |
| 2 | `SiteSearchMapping` 엔티티 + Repository | 🔜 예정 |
| 3 | `SiteSearchMapper` 서비스 구현 | 🔜 예정 |
| 4 | 크롤러별 `buildUrl()` 수정 (매핑 적용) | ✅ 완료 (하드코딩 매핑) |
| 5 | Wanted/Remember 키워드 적용 | ✅ 완료 |
| 6 | 프론트엔드 동적 드롭다운 | 🔜 예정 |
| 7 | 테스트 + 통합 검증 | 🔜 예정 |

---

## 10. 리스크 및 확인 사항

| 항목 | 설명 | 상태 |
|------|------|------|
| 사람인 `loc_cd` | `job-category` 엔드포인트에서 `loc_cd` 지원 확인 | ✅ 확인 완료 (경기 23→0) |
| 잡코리아 `local` | `local=I000` 부분 작동 (서울 필터링 일부 효과) | ⚠️ 부분적 (정확한 매핑 코드 확인 필요) |
| 원티드 `query` | `query=Java` 키워드 검색 정상 동작 | ✅ 확인 완료 |
| 원티드 `years` | `years=3` 경력 필터 동작 (20→14건) | ✅ 확인 완료 |
| 원티드 `locations` | `locations=seoul` 필터 미동작 (동일 20건) | ❌ 확인 필요 |
| 리멤버 `query` | `query=Java` 키워드 검색 동작 여부 확인 필요 | ⚠️ 부분적 |
| 법정동코드 변경 | 행안부에서 간헐적 업데이트 | 정기 동기화 검토 |

---

## 11. 참고 자료

- [사람인 API 가이드](https://oapi.saramin.co.kr/guide/job-search)
- [원티드 OpenAPI](https://openapi.wanted.jobs/api-docs/v1/)
- [잡코리아 API](https://www.jobkorea.co.kr/service/api)
- [DB 설계 표준](../architecture/db-standards/db-design-standard.md)
- [스크래퍼 아키텍처](./architecture.md)
