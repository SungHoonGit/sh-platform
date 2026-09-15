# 027-260915 프로젝트 정리 설계 문서

## 개요
- **목적**: 코드 정리/문서 동기화/프로젝트 구조 정리 3축에 대한 전체 감사(As-Is) 결과와 실행 계획을 AI 에이전트·사용자가 공유
- **범위**: 모노레포 전체(common/modules/platform/packages), docs/, CI 워크플로우
- **작성일**: 2026-09-15

## 1. 배경 및 이유
- 기능 집중 기간(이력서 문서 단위 분리, 백업 구축·검증) 동안 구조적 정리가 적체됨
- 모노레포 개편(`docs/monorepo-restructuring.md`) 이전 잔재 디렉터리·파일이 루트에 잔존
- AGENTS.md **하드코딩 지양 원칙** 위반 잔재(지역·채용사이트 목록) 존재
- 레이어 규칙(`api → infrastructure` import 금지) 위반 잔존
- 목표: 배포·기능 회귀 없이 "현행 구조에 필요한 것만 남기기"

## 2. 감사 결과 (As-Is)

### 2.1 프로젝트 구조 (고아/중복)
| 경로 | 추적 파일 수 | 판정 | 근거 |
|------|-------------|------|------|
| `sh-platform-auth/` | 84 | **고아** | 개편 전 auth → `modules/auth/backend/`로 이관. `settings.gradle.kts`는 `common/`+`modules/**`만 포함 → 빌드 미참조 |
| `scraper-platform-backend/` | 42 | **고아** | 개편 전 scraper → `modules/scraper/backend/`로 이관 |
| `sh-platform-common/` | 26 | **고아** | 개편 전 공통 → `common/`로 이관 |
| `resume-platform/` | 6 | **고아** | 개편 전 resume → `modules/resume/backend/`로 이관 |
| `frontend/web/` | 19 | **고아** | 개편 전 scraper SPA → `modules/scraper/frontend/`로 이관. nginx conf에 서빙 루트 없음(확인 완료). 배포는 backend jar 내 `copyFrontendDist`로 수행 |
| `packages/ui-shared/` | 8 | ✅ **유지** | 모든 프론트가 `@sh-platform/ui` 소스 참조(tsconfig, docs/guides/011) |
| `docker-compose.yml` | 1 | ✅ **유지** | 로컬 MariaDB (plans/010) |
| `.project` | 1 | 정리 후보 | Eclipse 이클립스 잔재 |
| `start.sh` | 1 | 정리 후보 | 옛 경로(`sh-platform-auth/build/libs/...`) 참조하는 옛 시작 스크립트 |
| `SCRAPER-GUIDE.md` | 1 | 정리 후보 | 옛 가이드 (개편 전 내용) |

- CI: `.github/workflows/deploy-frontend.yml`은 `frontend/web/**` push만 트리거 → 현재 fact die 워크플로우

### 2.2 코드 (레이어·하드코딩)
**레이어 규칙 위반 (main → api/ → infrastructure import, 4건)**
| 파일 | 위반 import |
|------|-------------|
| `modules/auth/backend/.../api/AuthController.java` | `infrastructure.TokenProvider`, `infrastructure.oauth2.CustomOAuth2User` |
| `modules/auth/backend/.../api/tenant/TenantController.java` | `infrastructure.TokenProvider`, `infrastructure.oauth2.CustomOAuth2User` |
| `modules/auth/backend/.../api/AdminController.java` | `infrastructure.AdminAuditLogRepository` |
| `modules/auth/backend/.../api/admin/AdminService.java` | `infrastructure.UserEntity`, `UserRepository`, `tenant.*` |

- `shared/config/*`(SecurityConfig 등)와 domain→infrastructure import는 규칙상 허용(설정/정상) — 대상 아님.

**하드코딩 마스터 데이터 (AGENTS 원칙 위반)**
| 위치 | 내용 |
|------|------|
| 프론트 `modules/scraper/frontend/.../SearchFilters.tsx` | `REGIONS`, `DEFAULT_LOCATIONS` (지역 목록) |
| 프론트 `Search.tsx`·`Viewer.tsx`·`Schedule.tsx` | `SITES` (채용사이트 코드 목록) |
| 백엔드 `JobPostingController.java:301` | 사이트 명칭 목록 하드코딩 ("saramin·사람인" 등) |
| 백엔드 `SiteSearchMappingInitializer.java` | `switch(siteName)` 사이트별 페이지 처방 |
| 백엔드 `JobPostingExportItem/ SearchRequest` | "saramin/jobkorea/wanted/remember" 4개 고정 전제 |

- UI 표시 전용 상수(아이콘, 분 단위, 컬럼 폭, 크론 DOW 매핑)는 허용 범위. TODO/FIXME 실업무 잔존 없음(테스트 픽스처 문자열뿐).

### 2.3 문서 (동기화 대상)
| 문서 | 판정 |
|------|------|
| `docs/react-frontend-design.md`, `docs/react-frontend-design-v3.md` | 개편 전 설계 → **보관(archive)** |
| `docs/oci-platform-guide.md` | 개편 전 인프라 구조 일부 → 갱신 또는 부분 보관 |
| `docs/monorepo-restructuring.md` | 개편 실행 기록 → **유지(역사 기록)** |
| `docs/plans/doc-viewer.md` | 넘버링 컨벤션 위반 → archive 이동 |
| `docs/plans/012...roadmap` 등 | 현행 계획 문서 → 유지 |
| `docs/infrastructure/` vs `docs/infra/` | 폴더 중복 → 병합 |
| `docs/archive/ · common/ · development/ · front/ · saas/` | 분류 필요 |

## 3. 설계 (정리 방안)

### 3.1 Phase A — 구조 정리
1. `git rm -r` 고아 디렉터리 5개(옛 이력은 git history로 보존) + `.project`, `start.sh`
2. `deploy-frontend.yml` 제거(또는 `modules/**/frontend/**` 재지정 — scraper/resume 프론트는 deploy-backend의 `copyFrontendDist`로 빌드되므로 **제거 권장**)
3. `SCRAPER-GUIDE.md` → `docs/archive/2026/` 이동
4. 사전 검증: nginx conf 전수 무참조 확인, workflow 트리거 재스캔, `git grep` 참조 확인
   - 리스크: 낮음(정적 파일·미포함 모듈). 배포 영향 없음

### 3.2 Phase B1 — 레이어 위반 수정 (4건)
- 원칙: api/ 레이어는 DTO·Controller만, 로직은 domain 서비스로 위임
- `AuthController`·`TenantController`: 토큰 생성·`CustomOAuth2User` 해석을 domain(`AuthService`/별도 도메인 서비스) 메서드로 이관, api는 DTO 반환만
- `AdminController`·`api/admin/AdminService`: 통계·감사 조회 로직을 domain(`AdminService`)로 이관(테스트 100% 유지)
- 검증: 모듈 단위 테스트 통과 + 배포 무회귀

### 3.3 Phase C — 문서 정리
- archive: 개편 전 설계 3건 + `doc-viewer.md` + `SCRAPER-GUIDE.md` → `docs/archive/2026/`
- 병합: `docs/infractructure` → `docs/infra/`(내용 합치고 보관)
- 현행 문서만 남긴 후 목록 갱신

### 3.4 Phase B2 — 마스터 데이터 DB화 (별도 설계 필요)
- **별도 설계 문서(028) 작성 후 진행** — 범위가 커서 이번 정리의 마지막 단계
- 대상: `channel`(채용사이트: code·한글명·active·display_order·icon) + `region`(시도) 테이블, 참조 API(`GET .../search?q=`)
- 프론트 `SITES/REGIONS/DEFAULT_LOCATIONS` → API 조회로 전환, 백엔드 Controller 하드코딩 목록·switch 제거
- ```common` 테이블 시드 + DDL(+deploy workflow 라인) 추가
- UI 아이콘·컬럼 등 표시 메타는 테이블 확장 또는 앱 상수 허용 판단

## 4. 구현 계획
| 단계 | 내용 | 리스크 | 검증 |
|------|------|--------|------|
| A | 고아 제거 + 죽은 workflow 제거 + 보관 | 낮음 | 빌드 통과, 배포 무회귀, nginx 정상 |
| B1 | 레이어 위반 4건 수정 | 낮음 | 모듈 테스트 100% |
| C | 문서 보관/병합 | 낮음 | 목록 대조 |
| B2 | 사이트·지역 마스터 DB화 (설계 028 후) | 중 | API 스모크 + 프론트 빌드/전환 검증 |

- 권장 순서: A → B1 → C → B2 (리스크 오름차순 + 가시적 성과)

## 5. 승인/판단 필요 항목
- `deploy-frontend.yml`: 제거(권장) vs paths 변경
- `start.sh`·`.project`: 삭제 vs 보관
- B2의 아이콘/컬럼 등 UI 메타: 테이블화 vs 앱 상수 허용
- scraper 프론트 `REGIONS`: 현재 scraper 필터 전용인지, 공용화 대상인지
- `docs/oci-platform-guide.md`: 갱신 vs 부분 보관

## 6. 참고 자료
- `docs/monorepo-restructuring.md` (개편 기록)
- `AGENTS.md` 하드코딩 지양 원칙 / 레이어 규칙

---
*작성일: 2026-09-15*