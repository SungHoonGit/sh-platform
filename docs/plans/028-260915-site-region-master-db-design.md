# 028-260915 사이트·지역 마스터 DB화 설계 문서

## 개요
- **목적**: 스크래퍼의 채용사이트·지역(시도) 마스터데이터를 DB로 일원화해 배포 없는 추가/변경을 가능하게 함
- **범위**: `site_definition` 확장 + `region` 테이블 신설 + 참조 API + 프론트 하드코딩 전환
- **작성일**: 2026-09-15
- **상위 계획**: `docs/plans/027-260915-project-cleanup-design.md` Phase B2

## 1. 배경 및 현재 상태 (조사 결과)

### 이미 DB화 완료
| 항목 | 상태 | 위치 |
|------|------|------|
| 채용사이트 코드·한글명 | ✅ `site_definition` (id, site_name, display_name, base_url, is_enabled) | ddl-v2.sql:115, `model/SiteDefinition.java` |
| 사이트 검색 파라미터 매핑 | ✅ `site_search_mapping` | ddl-v2.sql:273, `service/SiteSearchMapper.java` |
| 사이트 목록 API | ✅ `GET /sites`, `GET /sites/enabled` | `controller/SiteDefinitionController.java` |

### 여전히 하드코딩
| 항목 | 위치 | 영향 |
|------|------|------|
| 프론트 사이트 UI 목록 `SITES`(2개) | `Search.tsx:17`, `Viewer.tsx:11`, `Schedule.tsx:27` | site_definition과 6개로 불일치 가능, 신규 사이트 노출 불가 |
| 프론트 Excel 사이트명 맵(4개) | `Search.tsx:407`(`SITE_NAME_MAP`) | wanted/remember 등 누락 위험 |
| 백엔드 Excel 시트명 맵 | `JobPostingController.java:300~305` | `displayName`과 중복 정의 |
| `SearchRequest` 기본 sites(4개 고정) | `api/dto/SearchRequest.java:31` | 활성 사이트 변경 미반영 |
| 지역(17개 시도) 마스터 | `SearchFilters.tsx:1`(`REGIONS`) | DB/API 없음 |
| 사이트별 지역 코드 매핑 | `SaraminCrawler.mapLocationCode:295`, `JobkoreaCrawler.mapLocationCode:323`, `SiteSearchMappingInitializer:56/68/79` | 시도→사이트코드 switch, 크롤러 개정과 함께 별도 과제 |
| UI 표시 메타(아이콘·색·순서) | `SITE_TAB_COLORS`(Viewer), `SITES color` 등 | `site_definition`에 컬럼 부재 |
| 크롤러 등록 사이트 코드 | `SaraminCrawler.getSiteName()`(39) 등 4개 클래스 | DB 미참조 (등록 계약은 유지, 불일치 감지 필요) |

## 2. 요구 사항

### 2.1 기능 요구 사항
- [ ] FR-001: `site_definition`에 `display_order`, `icon`, `color` 컬럼 추가(멱등 시드 적용)
- [ ] FR-002: `region` 테이블 신설(17개 시도 + 정렬 + 활성 여부) + 멱등 시드
- [ ] FR-003: 참조 API — `GET /sites/enabled`(정렬·아이콘·색 포함), `GET /regions`, `GET /regions/search?q=`
- [ ] FR-004: 프론트 `SITES`/`REGIONS`/`DEFAULT_LOCATIONS` → API 조회로 전환(스켈레톤 상수 제거)
- [ ] FR-005: Excel 사이트명(프론트/백엔드) → `site_definition.display_name` 참조
- [ ] FR-006: `SearchRequest` 사이트 기본값 → 활성 사이트 기준(서비스 레이어)
- [ ] FR-007: 크롤러 site 코드 ↔ `site_definition` 불일치 시 기동 경고 로그

### 2.2 비기능 요구 사항
- 하드코딩 지양 원칙(AGENTS.md) 준수 — 시드 데이터만 DDL에, 표시 로직은 전부 DB 조회
- 기존 API 응답 하위 호환(불필요한 필드 제거 없음)
- 배포: deploy 워크플로우의 DDL 실행 절차에 따라 멱등 적용

## 3. 설계

### 3.1 데이터 모델
```sql
-- site_definition 확장 (ALTER + 멱등)
ALTER TABLE site_definition
  ADD COLUMN display_order INT NOT NULL DEFAULT 0,
  ADD COLUMN icon         VARCHAR(20)  NULL,
  ADD COLUMN color        VARCHAR(50)  NULL;

-- region 신설
CREATE TABLE IF NOT EXISTS region (
  id            BIGINT AUTO_INCREMENT PRIMARY KEY,
  name          VARCHAR(20)  NOT NULL UNIQUE,      -- 시도명 (서울, 경기, ...)
  display_order INT          NOT NULL DEFAULT 0,
  is_active     TINYINT(1)   NOT NULL DEFAULT 1,
  created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 시드 17개 (멱등: INSERT ... SELECT WHERE NOT EXISTS)
```

### 3.2 API 설계
| 메서드 | 경로 | 응답 |
|--------|------|------|
| GET | `/sites/enabled` | `[{id, siteName, displayName, baseUrl, isEnabled, displayOrder, icon, color}]` (display_order 오름차순) |
| GET | `/regions` | `[{id, name, displayOrder, active}]` (활성만, display_order순) |
| GET | `/regions/search?q=` | `[{id, name}]` — resume `ReferenceDataController` 패턴(`findByNameContaining`) |

### 3.3 프론트 전환
- `SearchFilters.tsx`: `REGIONS` 배열 삭제 → `GET /regions` 세션 캐시, `DEFAULT_LOCATIONS`는 UI 기본선택 상수 유지(설정 성격)
- `Search/Viewer/Schedule`: `SITES` 배열 삭제 → `GET /sites/enabled` 조회, 색상/아이콘은 응답 필드 사용
- `Viewer.SITE_TAB_COLORS`, `Search.SITE_NAME_MAP` → 사이트 목록 기반 매핑으로 대체(원티드/리멤버 색은 DB 시드로 보강)
- `Schedule.fetchSites()` 기존 호출은 siteDefinitionId 맵에 추가로 UI 목록까지 사용

### 3.4 백엔드 전환
- `SiteDefinition` 엔티티 + `SiteDefinitionController` 응답 DTO에 신규 필드 반영
- `JobPostingController` Excel 시트명: `siteDefinitionRepository`(또는 서비스)에서 `displayName` 조회, 없으면 `siteName` fallback
- `SearchRequest.sites`가 null/빈 값이면 활성 사이트(`isEnabled=true`) 코드로 기본값 세팅
- `Region` 엔티티·`RegionRepository`·`RegionController` 신설
- 크롤러-사이트 불일치 검증: `CrawlerFactory`(또는 기동 시) 로그 경고

### 3.5 제외 범위 (후속 과제)
- 사이트별 지역 코드 매핑(서울→101000 등) DB화는 크롤러 내부 매핑과 결합되어 있어 별도 설계로 분리
- `SiteSearchMappingInitializer` switch는 시드 생성용(멱등)이므로 유지

## 4. 구현 계획
| 단계 | 내용 | 검증 |
|------|------|------|
| 1 | DDL(region 신설 + site_definition 확장 + 시드) 작성, deploy 워크플로우 DDL 라인 추가 | DB 컬럼/시드 확인 |
| 2 | 백엔드: 엔티티 확장 + Region 신설 + API + SearchRequest 기본값 + Excel 맵 제거 + 크롤러 검증 | 단위 테스트 + 스모크 |
| 3 | 프론트: 3파일 SITES/REGIONS 전환 + 색상/아이콘 반영 | 프론트 빌드 + 실사용 |
| 4 | 문서(DDL 버전, AGENTS) 갱신 + 배포 | CI 배포 무회귀 |

## 5. 참고 자료
- resume 참조 API 패턴: `ReferenceDataController`(schools/majors/skills `search?q=`)
- `docs/plans/027-260915-project-cleanup-design.md` §3.4

---
*작성일: 2026-09-15*