# 030-260915 site_search_mapping 관리 API 설계 문서

## 개요
- **목적**: 크롤러 검색 매핑이 이제 DB(`site_search_mapping.value_mapping`)를 1차 소스로 사용하지만, **수정 수단이 DDL 시드 + 재배포뿐**이어서 API를 추가해 배포 없이 마스터를 관리한다.
- **범위**: `site_search_mapping` CRUD API + 값 검증 + 단위 테스트. 관리 UI는 후속(단독 과제).
- **작성일**: 2026-09-15
- **전제**: 설계 029로 `value_mapping`(지역·직무·경력 코드 등)이 사람인·잡코리아·원티드 크롤링의 1차 소스가 됨. 잡코리아·원티드·리멤버는 `toSiteParams`가 이 테이블을 전면 사용.

## 1. 배경 및 현재 상태
| 항목 | 상태 |
|------|------|
| 테이블 | `site_search_mapping` — unique(site_definition_id, standard_key), FK → site_definition ON DELETE CASCADE (ddl-v2.sql:273~287) |
| 시드 | `docs/scraper/ddl-v2.sql` INSERT IGNORE 4개 사이트 (사람인 5행·잡코리아 4행·원티드 4행·리멤버 3행) |
| 조회 | `SiteSearchMapper.toSiteParams` — `findBySiteDefinition_SiteNameAndIsEnabledTrueOrderByDisplayOrder` |
| 수정수단 | **없음** — value_mapping 값 변경 = DDL 수정 + CI 배포 (지역코드 1개 추가해도 재배포) |

### 1.1 필요성
- AGENTS 원칙: "지금 DB화해 두면 나중에 추가/변경이 **배포 없이** 끝난다". 시드 INSERT는 멱등이라 재배포해도 기존 행 갱신이 되지 않으므로, **DB 직접 수정 없이는 크롤러 매핑 변경이 불가능**한 상태.
- 관리자 UI(후속)와 정합성: site/region은 이미 CRUD API가 있으나 매핑만 빠져 관리 화면이 불완전.

## 2. 요구 사항
- [ ] FR-001: 매핑 목록 조회(전체/사이트별) — site명·표시명 포함 응답
- [ ] FR-002: 매핑 행 생성 — (site, standard_key) 유일, 값 타입별 JSON 검증
- [ ] FR-003: 매핑 행 수정 — value_mapping·url_param_name·is_enabled·display_order 갱신
- [ ] FR-004: 매핑 행 삭제
- [ ] FR-005: `mapped`/`range`는 value_mapping **유효한 JSON 객체** 필수, `direct`는 value_mapping 불가
- [ ] FR-006: 중복·미존재는 표준 오류(DUPLICATE_NAME/NOT_FOUND/INVALID_INPUT)

### 2.2 비기능
- 크롤러·SiteSearchMapper 시그니처 변경 없음(동작 보존)
- 응답은 siteDefinition Lazy 직렬화 회피용 전용 DTO

## 3. 설계
### 3.1 API
| 메서드 | 경로 | 설명 |
|--------|------|------|
| GET | `/search-mappings` | 전체 목록 (site.시도 asc, display_order asc) |
| GET | `/search-mappings?siteName=saramin` | 사이트별 목록 |
| GET | `/search-mappings/{id}` | 단건 조회 |
| POST | `/search-mappings` | 생성 `{siteDefinitionId, standardKey, urlParamName, valueType, valueMapping, isEnabled, displayOrder}` |
| PUT | `/search-mappings/{id}` | 수정 (site·standard_key 불변) |
| DELETE | `/search-mappings/{id}` | 삭제 |

### 3.2 DTO/검증
- 요청 `SearchMappingRequest` record (siteDefinitionId·standardKey·urlParamName·valueType·valueMapping·isEnabled·displayOrder)
- 응답 `SearchMappingResponse` record (id·siteName·siteDisplayName·standardKey·urlParamName·valueType·valueMapping·isEnabled·displayOrder·updatedAt)
- 검증 규칙:
  - standardKey 공백 금지, urlParamName 공백 금지
  - (siteDefinitionId, standardKey) 중복 → `DUPLICATE_NAME`
  - `value_type=mapped|range` → value_mapping 필수 + `ObjectMapper.readTree`로 JSON 객체인지 검증, 아니면 `INVALID_INPUT`
  - `value_type=direct` → value_mapping 무시(null 처리)
  - 존재하지 않는 id → `NOT_FOUND`, 존재하지 않는 site → `NOT_FOUND`

### 3.3 클래스
```
SiteSearchMappingService          (service, @Transactional)
  - listAll() / listBySite(siteName) / getById(id)
  - create(request) / update(id, request) / delete(id)
SearchMappingRequest/Response     (api/dto)
SearchMappingController           (/search-mappings)
SiteSearchMappingRepository       + findAllByOrderBySiteDefinition_SiteNameAscDisplayOrderAsc,
                                    findBySiteDefinition_SiteNameOrderByDisplayOrderAsc,
                                    existsBySiteDefinitionIdAndStandardKey
```

## 4. 구현 계획
| 단계 | 내용 | 검증 |
|------|------|------|
| 1 | DTO 2종 + repo 메서드 | 컴파일 |
| 2 | Service CRUD + 검증 | 단위 테스트(가짜 repo) |
| 3 | Controller | 전체 scraper 테스트 |
| 4 | 커밋 → CI 배포 → 실서버 api-docs 확인 | 스키마에 /search-mappings 반영 |

## 5. 참고 자료
- `docs/scraper/ddl-v2.sql` site_search_mapping 스키마·시드
- `docs/plans/029-260915-crawler-location-db-design.md` (value_mapping 1차 소스화 근거)
- 기존 CRUD 패턴: `SiteDefinitionController`, `RegionController`(+`RegionService`) 

---
*작성일: 2026-09-15*