# 036-260929-차단 키워드 부분일치 옵션 설계 문서

## 개요
- **목적**: 회사 차단(블랙리스트) 키워드에 매칭 방식 옵션을 추가한다 — 기존 정확일치 단일 방식에 더해 **부분일치(contains)** 를 선택할 수 있게 한다.
- **범위**: scraper 백엔드(판정 전 지점) + 공용 프론트(ui-shared·scraper·resume) — DDL·API·UI 전 구간
- **작성일**: 2026-09-29
- **작성자**: AI Assistant / 사용자
- **선행 문서**: `031-260916-company-manage-design.md` (차단·메모 구조), `032-260922-career-compound-mapping-design.md` (ENUM 옵션 DB화 선례)

## 1. 배경 및 이유
- 현재 차단 판정은 **정규화 후 정확일치(exact) 단일 방식**이다 (`CompanyBlacklistService.normalize` → `Set.contains` / SQL `NOT IN` / 프론트 `Set.has`).
- 사용자는 "에이피"처럼 일부 문자열만으로 계열사 묶음 차단을 원함 (부분일치 수요 — AGENTS 후보 과제로 등록된 지 반년).
- 추가 근거:
  - 옵션값은 바뀔 수 있으므로 **DDL ENUM으로 DB화** (하드코딩 지양 원칙, `site_search_mapping.value_type` 선례 = 설계 032).
  - 기본값 `exact` + 기존 행 DEFAULT → **배포 즉시 동작 변화 없음** (안전 배포).

## 2. 요구 사항
### 2.1 기능 요구 사항
- [ ] FR-006-001: 차단 항목에 매칭 방식 `match_type ENUM('exact','contains')` 컬럼 추가 (기본 `exact`, 기존 행 자동 보호)
- [ ] FR-006-002: 차단 생성/수정 시 매칭 방식 선택 — 공용 `BlockConfirmDialog`에 셀렉트(정확일치/부분일치), 기본 정확일치, 수정 시 기존 값 프리필
- [ ] FR-006-003: **판정 단일 소스** — 백엔드 `BlockMatcher`, 프론트 `ui-shared` 헬퍼가 동일 규칙(정규화명 기준 exact=`equals`, contains=`contains`)을 정의하고 모든 판정 지점이 이를 사용
- [ ] FR-006-004: 파생 조회도 매칭 반영 — 숨김 공고 수(비고 열), 차단 뱃지(목록/슬라이드/자동완성), 슬라이드 차단 토글 대상, 북마크 강제 해제
- [ ] FR-006-005: 관리 UI 표시 — `BlacklistManagerModal`·슬라이드오버에 부분일치 배지/표기
- [ ] FR-006-006: 실시간 검색·목록 SQL 필터도 매칭 방식 반영 (DB 레벨, 페이지네이션 유지)

### 2.2 비기능 요구 사항
- 성능: 목록 SQL 필터는 서버 페이지네이션 유지 (메모리 일괄 필터로 전환하지 않음), 숨김 수 집계는 부분일치 항목당 1회 쿼리(항목 수가 소수)
- 보안: 키워드는 정규화 후 저장·비교 (기존 `normalize` 단일 소스 유지), LIKE 와일드카드(`%`,`_`) 포함 키워드는 희귀 케이스 근사 오차 허용(기존 주석 관례)
- 하위 호환: API 응답은 `matchType` 필드 추가(기존 필드 무변경), 프론트 미지원 시 `undefined → exact` 처리

## 3. 설계

### 3.1 데이터 모델 (DDL v14)
```sql
-- ddl-v14.sql (Re-runnable: ADD COLUMN IF NOT EXISTS)
ALTER TABLE company_blacklist
    ADD COLUMN IF NOT EXISTS match_type ENUM('exact','contains') NOT NULL DEFAULT 'exact'
    AFTER company_name_normalized
    COMMENT '키워드 매칭 방식 (exact=정확일치, contains=부분일치)';
```
- 엔티티: `CompanyBlacklist.matchType` — `@Enumerated(EnumType.STRING)`, 중첩 enum `MatchType { exact, contains }`
  (**소문자 상수** — 설계 032 `SiteSearchMapping.ValueType` 패턴 그대로, Jackson 직렬화가 프론트 소문자 유니언과 자동 일치)
- 고유 제약 `(account_id, company_name_normalized)` 유지 — 매칭 방식과 무관하게 키워드명 중복은 1건(방식만 다름→동일 키워드 중복 등록 불가, 유의미)

### 3.2 판정 단일 소스 — `BlockMatcher`
`CompanyBlacklistService` 안에 중첩 record (기존 `BlockStatsResponse` 선례):
```java
public record BlockMatcher(List<CompanyBlacklist> entries) {
    public static BlockMatcher of(List<CompanyBlacklist> entries);
    public boolean isEmpty();
    /** 정규화 회사명과 일치하는 첫 항목 (등록 역순 = 최신 우선). 미일치 시 empty */
    public CompanyBlacklist firstMatch(String normalized);
    public boolean isBlocked(String normalized);
}
// CompanyBlacklistService.matcher(accountId) → BlockMatcher.of(list(accountId))
```
- 규칙: `exact` → `entry.getCompanyNameNormalized().equals(normalized)`,
  `contains` → `normalized.contains(entry.getCompanyNameNormalized())`
- 기존 `normalizedNames()`는 호출처가 matcher로 전환 후 미사용이면 제거(테스트 정리 포함)

### 3.3 백엔드 적용 지점 표

| # | 지점 | 현 상태 | 변경 |
|---|------|---------|------|
| 1 | `JobPostingController.getJobs` (107~168) | `Set.contains` 메모리 필터 | `BlockMatcher`로 전환 (`isEmpty`로 기존 분기 유지) |
| 2 | `JobPostingRepository.searchRecent` (64~76) | SQL `j.company NOT IN (정규화명)` — **원문↔정규화명 불일치 정합성 버그** | JPQL 2중 `NOT EXISTS`로 재작성: exact는 `LOWER(REPLACE(...)) = b.companyNameNormalized`, contains는 `... LIKE CONCAT('%', b.companyNameNormalized, '%')` — 정규화 반영 + 방식 인지 + **SQL 페이지네이션 유지** |
| 3 | `SearchService` (110~141) | `resolveBlockedCompanies(): Set` | `resolveBlockMatcher(): BlockMatcher` + `isBlocked(normalize(company))` |
| 4 | `CompanyNoteService.enrich` (329~361) | `Map<정규화명, true>` 정확일치 | `firstMatch` — 차단 뱃지·차단카테고리(blockReasons)를 일치 항목에서 취득. 전체 탭 차단행 추가 루프는 그대로(키워드 행 노출) |
| 5 | `CompanyNoteService.blockedData.hiddenCounts` (422~450) | `countByNormalizedCompanyIn` (IN 정확일치) | exact 항목은 기존 배치 쿼리 + **contains 항목은 신규 `countByNormalizedCompanyContaining`(native LIKE)** 결과 병합 |
| 6 | `CompanyNoteService.suggestCompanies` (254~264) | `Map` 뱃지 | `firstMatch` 뱃지 |
| 7 | `CompanyNoteService.dropBookmarkIfBlocked` (311~318) | `existsBy...Normalized` | 목록 조회 + `firstMatch` (항목 소수 — 쿼리 수 동일) |
| 8 | `CompanyBlacklistController` DTO | `AddRequest`/`UpdateRequest` | `matchType` 필드 추가 (null→exact), 서비스 `add`/`update` 시그니처 확장. `GET /company-blacklist`는 엔티티 직렬화라 자동 반영 |

- **미변경**: 크롤링 시 필터 없음(읽기 시점 필터 정책 유지), `findRecentByNormalizedCompany`(메모 회사 관련 공고 = 정확 회사 대상), `stats`(항목 단위 집계), platform 대시보드.

### 3.4 프론트 적용 지점 표

| # | 지점 | 변경 |
|---|------|------|
| 1 | `ui-shared/BlockConfirmDialog` | 매칭 방식 셀렉트 신규(정확일치/부분일치, 기본 exact, `initialMatchType` 프리필) + `onConfirm`에 5번째 파라미터 `matchType` |
| 2 | `ui-shared` 신규 헬퍼 `matchesBlocked.ts` | `findBlockEntry(normalized, entries)` / `isBlockedCompany(normalized, entries)` — `matchType undefined → exact` 호환. 백엔드 `BlockMatcher`와 동일 규칙 |
| 3 | `ui-shared/BlacklistManagerModal` | `BlacklistItemLike.matchType?` + 키워드 옆 부분일치 배지(amber chip), 수정 콜백은 기존 그대로(호출부가 `initialMatchType` 전달) |
| 4 | `scraper/api/scraper.ts` | `BlacklistItem.matchType`, `addBlacklist(..., matchType?)`, `updateBlacklist(..., matchType?)` 바디 추가 |
| 5 | `Search.tsx`·`Viewer.tsx`·`PostingsBrowsePage.tsx` | **`blacklisted: Set` 상태 폐기 → `blItems` 단일 소스** + `useMemo`로 `isBlockedCompany(normCompany(j.company), blItems)` 필터 (로드·차단·해제·수정 모두 `blItems`만 갱신 — 이중 상태 드리프트 제거), `onConfirm` matchType 전달 |
| 6 | `Companies.tsx` | `unblockByNormalized`의 `===` 비교 → `findBlockEntry` (부분일치 항목 해제 가능), 행 차단 판별도 동일 |
| 7 | `CompanySlideOver.tsx` | `blockedEntry` 정확일치 조회 → `findBlockEntry`, "차단 키워드:" 줄에 부분일치 표기, 토글·인라인 카테고리 편집은 기존 항목 id 재사용 |
| 8 | `PostingsBrowsePage.tsx` 내장 blacklist API 래퍼 | matchType 파라미터·타입 추가 (resume은 `api/scraper.ts` 미공유) |

### 3.5 API 변경
```
POST /scraper/company-blacklist        AddRequest    += matchType?: "exact"|"contains"   (null→exact)
PUT  /scraper/company-blacklist/{id}   UpdateRequest += matchType?: "exact"|"contains"   (null→유지)
GET  /scraper/company-blacklist        응답 엔티티     += matchType: "exact"|"contains"   (신규 필드 추가)
```
- 위반 시 `INVALID_INPUT` (허용값 2개 외 거부) — `ValueType` 검증 패턴 따름.

### 3.6 매칭 의미론
- 둘 다 **정규화명 기준**: 정규화 = 소문자 + 공백 제거 + `(주)"/"㈜"/"주식회사` 제거 (기존 `normalize`, FE `normCompany` 동일 복제 유지).
- `contains`: 정규화 회사명에 정규화 키워드가 **포함**되면 차단 (`"주식회사 에이피시스템"` → `apsystem`, 키워드 `ap` 면 차단).
- 혼용 허용: exact `삼성전자` + contains `에이피` 공존 가능(고유 제약은 키워드명 기준).
- 정확일치·부분일치가 모두 걸리는 회사: 차단 = 당연(OR 조건), `firstMatch`는 등록 역순 최신 항목 우선(카테고리 표시용).

## 4. 구현 계획
| 단계 | 내용 | 게이트 |
|------|------|--------|
| Phase 1 (BE) | ddl-v14 + deploy 라인, MatchType/엔티티, BlockMatcher, 서비스·컨트롤러·리포지토리 8개 지점, 테스트 신규/수정 | `./gradlew :modules:scraper:backend:test` |
| Phase 2 (FE) | ui-shared(dialog·헬퍼·매니저모달) → scraper(api·Search·Viewer·Companies·SlideOver) → resume(PostingsBrowse) | 4앱 `npm run build` + platform `npm run lint` |
| 배포 | 두 커밋을 한 push로 배포(DDL→앱 순서는 워크플로우 보장), 라이브 마커 검증 | deploy run success + 번들 마커 |
| 기록 | 작업 일지 + AGENTS 갱신 | docs 커밋 |

### 4.1 테스트 계획
- `CompanyBlacklistServiceTest`: BlockMatcher(exact/contains/혼용/firstMatch/isEmpty), add·update의 matchType 저장·수정, 잘못된 값 INVALID_INPUT
- `CompanyNoteServiceTest`: 부분일치 키워드의 숨김 수(contains LIKE), 차단 뱃지 firstMatch, suggest 뱃지
- `JobPostingRepository` 검증은 H2 `@DataJpaTest` 신설 검토 — searchRecent JPQL의 `replace()` 파싱 게이트(불확실성 해소)
- FE: 빌드(tsc) + oxlint — 동작 확인은 배포 후 운영 실사용 (기존 프로세스)

## 5. 참고 자료
- 현황 조사: 차단 판정 지점 BE 8 + FE 8, 유일한 기존 부분일치는 차단 목록 `q` 검색(판정 아님)
- 선례: `SiteSearchMapping.ValueType` (소문자 enum + `@Enumerated(STRING)` + DDL ENUM), ddl-v8 `information_schema` 가드, ddl-v13 헤더 규약
- 관련 이슈: `searchRecent`의 원문↔정규화명 `NOT IN` 불일치는 본 재작성으로 동시 해결

---
*작성일: 2026-09-29*
