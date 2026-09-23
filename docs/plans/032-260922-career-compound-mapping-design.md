# 032-260922 사람인/잡코리아 경력 필터 복합 매핑 DB화 설계 문서

## 개요
- **목적**: 사람인 `exp_cd`+`exp_min/max`, 잡코리아 `careerList`+`careerMin/Max` 경력 필터를 코드에 박지 않고 `site_search_mapping`으로 관리 — **배포 없이 경력 옵션·코드 조정**
- **범위**: scraper BE( compound value type + mapper + 크롤러 DB 우선) + DDL v13 + 관리 UI 타입 노출 + 테스트. 원티드/리멤버 career는 이미 단일 `mapped`로 동작 — 변경 없음
- **작성일**: 2026-09-22
- **선결**: 설계 029(DB 1차 소스), 030(관리 API) — 본 과제는 030의 "복합 value type 후속 후보"(9/15 일지 향후작업4)를 수행

## 1. 배경 및 현재 상태
| 사이트 | 실제 URL/API 파라미터 | 현재 소스 | DB 행 상태 |
|--------|----------------------|-----------|------------|
| 사람인 | `exp_cd`(1/2) + `exp_min`/`exp_max`(수치 구간) | `SaraminCrawler.appendCareerParams` **switch 하드코딩** | id=1 career→`career_level`(레거시, 미사용) |
| 잡코리아 | `careerList`["1"/"2"] + `careerMin`/`careerMax` | `mapCareerType`+`careerRange` **switch 하드코딩** | id=2 career→`careerType`(레거시 new/career, 미사용) |
| 원티드 | `years` 단일 | `toSiteParams` (DB) | 정상 |
| 리멤버 | `min_experience` 단일 | `toSiteParams` (DB) | 정상 |

- 9/15 재진단: 단일 코드 문자열 `value_mapping`으로는 **코드+수치 구간 복합체계 표현 불가** → 유지 판단 → 본 과제로 해소
- careerMin/careerMax **숫자 입력 경로**(프론트에서 연수 직접 지정)는 매핑 키 없음 — 기존대로 유지 (DB화 대상 아님)

## 2. 요구 사항
- [ ] FR-001: `value_type`에 `compound` 추가 — value_mapping은 `{"표준값":{"파라미터명":"값", ...}, ...}` (중첩 객체)
- [ ] FR-002: `SiteSearchMapper.mapCompoundParams(siteName, standardKey, value)` — 표준값 → 사이트 파라미터 Map. 행 부재·비compound·미매핑 시 빈 Map (fallback 신호)
- [ ] FR-003: 사람인 `appendCareerParams` — DB compound 우선, 없으면 기존 switch fallback
- [ ] FR-004: 잡코리아 `appendCareerParams` — DB compound 우선, 없으면 기존 switch fallback
- [ ] FR-005: DDL v13 — ENUM에 `compound` 추가 + 사람인/잡코리아 career 행을 현행 코드와 동일한 compound 시드로 1회 마이그레이션(멱등 UPDATE)
- [ ] FR-006: 관리 API 검증 — compound도 유효 JSON 객체 필수
- [ ] FR-007: 관리 UI value type 셀렉트에 compound 노출
- [ ] FR-008: `toSiteParams`(단일 값 변환)는 compound 행 스킵 — 복합은 `mapCompoundParams` 전용

## 3. 설계
### 3.1 value_mapping 구조 (compound)
**사람인 career (site_definition_id=1)**
```json
{"신입":{"exp_cd":"1"},"경력":{"exp_cd":"2"},"1~3년":{"exp_cd":"2","exp_min":"1","exp_max":"3"},"3~5년":{"exp_cd":"2","exp_min":"3","exp_max":"5"},"5~10년":{"exp_cd":"2","exp_min":"5","exp_max":"10"},"10년이상":{"exp_cd":"2","exp_min":"10"}}
```
**잡코리아 career (site_definition_id=2)**
```json
{"신입":{"careerList":"1"},"경력":{"careerList":"2"},"1~3년":{"careerList":"2","careerMin":"1","careerMax":"3"},"3~5년":{"careerList":"2","careerMin":"3","careerMax":"5"},"5~10년":{"careerList":"2","careerMin":"5","careerMax":"10"},"10년이상":{"careerList":"2","careerMin":"10"}}
```
- `url_param_name`은 대표 파라미터 표시용 (`exp_cd` / `careerList`)
- careerList는 애초에 배열 파라미터 — mapper는 스트링으로 주고 크롤러가 `List.of(...)` 감쌈

### 3.2 API/메서드
| 위치 | 변경 |
|------|------|
| `SiteSearchMapping.ValueType` | `compound` enum 추가 |
| `SiteSearchMapper` | `mapCompoundParams(siteName, standardKey, value)` 신규; `convertValue`에서 compound→null (toSiteParams 스킵) |
| `SaraminCrawler.appendCareerParams` | DB 우선 → empty면 switch |
| `JobkoreaCrawler.appendCareerParams` | DB 우선 → empty면 switch |
| `SiteSearchMappingService.validateMapping` | compound 포함 JSON 객체 검증 (기존 분기 유지) |
| `platform/master.ts` + `AdminMaster` | ValueType/셀렉트에 compound |

### 3.3 DDL (docs/scraper/ddl-v13.sql)
```sql
USE scraper_platform;
ALTER TABLE site_search_mapping
    MODIFY value_type ENUM('direct','mapped','range','compound') DEFAULT 'direct';
-- 멱등 마이그레이션 (이미 compound면 no-op 동일 값 재설정)
UPDATE site_search_mapping SET value_type='compound', url_param_name='exp_cd', value_mapping='...' 
 WHERE site_definition_id=1 AND standard_key='career';
UPDATE site_search_mapping SET value_type='compound', url_param_name='careerList', value_mapping='...' 
 WHERE site_definition_id=2 AND standard_key='career';
```
- deploy-backend.yml에 `ddl-v13.sql` 라인 추가 (어플리케이션 기동 전)

### 3.4 fallback 정책
- DB 행 없음 / 비활성 / 표준값 미포함 / JSON 파싱 실패 → **빈 Map** → 기존 하드코딩 switch 사용 (무중단)
- careerMin/careerMax 숫자 경로는 매핑 불필요 — 기존 분기 유지

## 4. 구현 계획
| 단계 | 내용 | 검증 |
|------|------|------|
| 1 | 설계 문서 본 문서 | - |
| 2 | ddl-v13 + deploy 라인 | 재실행 멱등 |
| 3 | ValueType + mapCompoundParams + validate | 단위 테스트 |
| 4 | 사람인·잡코리아 크롤러 DB 우선 | CrawlerMappingTest |
| 5 | 관리 UI compound | tsc+vite build |
| 6 | 전체 scraper test + FE build | CI 배포 → 관리 UI에서 career compound 확인 |

## 5. 참고 자료
- `docs/daily/2026-09-15-work-log.md` — 하드코딩 유지 판단·후속 후보 기록
- `docs/plans/030-260915-search-mapping-admin-api-design.md` — 관리 API
- `docs/scraper/ddl-v2.sql:273-371` — 테이블·기존 career 시드
- `SaraminCrawler.appendCareerParams` / `JobkoreaCrawler.mapCareerType`·`careerRange`

---
*작성일: 2026-09-22*
