# 039-261001-4앱-메뉴구조-반응형-UX개선 설계 문서

## 개요
- **목적**: 4개 앱(auth·scraper·resume·platform)의 메뉴 구조 재정의 + 반응형 웹 전환 (사용자 지정 핵심 개선)
- **범위**: shell 공통 네비 + 4앱 레이아웃 + 반응형(페이지별 점진) + **메뉴관리 DB화 대비 구조**
- **작성일**: 2026-10-01
- **작성자**: 사용자 지시(4앱 UX·UI 개선) / AI 설계
- **선행 조건**: 실사용 검증 10종(`docs/daily/2026-10-01-todo.md`) — 개선 착수 전 완료 권장

## 1. 배경 및 이유
### 1.1 현황 진단 (2026-10-01 코드 조사)
| 앱 | 상단 SubNav | 좌측 드로어(2단) | 문제 |
|----|------------|-----------------|------|
| scraper | 통합검색·스케줄등록·뷰어·회사관리 | 데이터(통합검색) + 수집(스케줄·뷰어·회사) | **subnav와 드로어 100% 중복** — 어디를 봐야 할지 모호 |
| platform | 개요·관리 | 개인서비스(4) + 관리(6) + **개발자링크(6)** | 개발자링크(Swagger·Javadoc·리포트)가 **사용자 메뉴에 상시 노출** — 번잡 |
| resume | 이력서만들기·프로젝트·채용탐색·지원현황 | 내 이력서(동적 8) + 탐색메뉴(3) + 개발자링크(1) | 탐색메뉴 = subnav 중복, 개발자 링크 상시 |
| auth | 자체 헤더 (login·signup) | — | 메뉴 최소 — 문제 없음 |

- 메뉴 정의가 각 레이아웃 **인라인 배열**(`Layout.tsx`·`PlatformLayout.tsx`·`App.tsx`) — 하나의 소스가 아니고 **메뉴관리(DB/CRUD) 불가**
- 반응형 미대응: GlobalHeader 앱 전환 `hidden sm:flex`(모바일에서 사라짐), SubNav 고정 가로 flex(좁은 화면 넘침), 페이지 테이블/그리드 무대응

### 1.2 왜 필요한가
- 사용자: "사이드 메뉴가 번잡하다", "페이지에 맞는 MSA별 메뉴 구조도", "반응형 웹이 가장 클듯", "메뉴관리도 나중에 가능하도록"

## 2. 요구 사항
### 2.1 기능 요구 사항
- [ ] FR-001: 앱별 메뉴를 **단일 메뉴 데이터**로 정의하고 SubNav·드로어를 같은 소스에서 도출 (중복 제거의 본질: 소스 분리)
- [ ] FR-002: 메뉴 데이터 타입을 `packages/shell`에 공통화 — 향후 `GET /api/v1/menus?app={app}` API 응답과 **동일 스키마**로 DB화(메뉴관리 CRUD) 전환 가능
- [ ] FR-003: 메뉴 구조 재정의 — 상단 SubNav는 **핵심 작업 3~4개(primary)**, 드로어는 전체 목록·2단 계층, 개발자링크는 **도구 섹션으로 분리·접기**(admin/개발자 모드에서만)
- [ ] FR-004: 메뉴 항목에 `roles`(ALL/ADMIN)·`order`·`visibility` 부여 — 권한/표시 순서 데이터화

### 2.2 비기능 요구 사항 (반응형)
- [ ] NFR-001: **≥ sm(640px)** 모바일에서 앱 전환·메뉴·주요 작업(검색/목록/저장) 사용 가능
- [ ] NFR-002: **≥ md(768px)** 태블릿: 2컬럼 폼, 테이블 가로 스크롤 또는 카드형
- [ ] NFR-003: **≥ lg(1024px)** 데스크톱: 현행 유지 (회귀 없음)
- [ ] NFR-004: 성능 — 반응형 유틸 추가 시 번클 증가 최소 (Tailwind 유틸은 스캔 시에만 출력)

## 3. 설계

### 3.1 메뉴 데이터 (FR-001~004, DB화 대비)
```ts
// packages/shell/src/menuTypes.ts (신규)
export interface MenuItem {
  id: string;            // "scraper.search" — DB PK 대비
  label: string;
  href?: string;         // 내부 이동 (app base 자동 접두)
  icon?: string;         // lucide 아이콘명 (DB 저장용 문자열 → FE는 map으로 LucideIcon 변환)
  external?: boolean;
  primary?: boolean;     // true → 상단 SubNav 노출
  section?: string;      // 드로어 섹션 라벨
  order: number;
  roles?: Array<"ALL" | "ADMIN">;  // 기본 ALL
  visible?: boolean;     // 향후 메뉴관리 토글 (기본 true)
}
export interface AppMenu { app: AppName; items: MenuItem[]; }
```
- 4앱의 메뉴 정의를 **앱별 `src/menus.ts` 1파일**로 이동 (`AppMenu` 객체)
- AppShell/SubNav/SideDrawer는 `items`를 받아 primary 필터·section 그룹핑·order 정렬 — **기존 props 구조 유지(호환) + menus 데이터 추가**
- 향후 DB화: `GET /menus` 응답이 `AppMenu`와 동일 → 로컬 `menus.ts`를 fetch fallback으로 교체만 하면 됨 (메뉴관리 CRUD UI는 별도 과제)

### 3.2 메뉴 구조 재정의안 (FR-003)
**scraper** (작업 흐름: 검색 → 수집 → 검증)
| 구분 | 항목 |
|------|------|
| SubNav (primary 3) | 통합검색 · 스케줄 · 뷰어 |
| 드로어 · 검색 | 통합검색 |
| 드로어 · 수집 | 스케줄 등록, 공고 뷰어 |
| 드로어 · 데이터 | 회사 관리 |
| 드로어 · 도구 (admin) | Swagger · Scraper (기존 platform 개발자링크 이관 검토) |

**platform** (개인 → 관리 → 도구)
| 구분 | 항목 |
|------|------|
| SubNav (primary 2) | 개요 · 관리(ADMIN) |
| 드로어 · 개인 | 내 이력서, 공고 탐색, 지원 관리, 계정 설정 |
| 드로어 · 관리 (ADMIN) | 권한·사용자·테넌트·감사·세션·마스터 (기존) |
| 드로어 · 도구 (ADMIN, **접힘**) | Swagger 3종·Javadoc·테스트리포트·SchemaSpy — 아코디언 기본 닫힘 |

**resume** (이력서 → 포트폴리오 → 탐색)
| 구분 | 항목 |
|------|------|
| SubNav (primary 4) | 이력서 · 프로젝트 · 채용탐색 · 지원현황 |
| 드로어 · 내 이력서 | 동적 문서 8 (기존 유지) |
| 드로어 · 탐색 | 프로젝트, 채용탐색, 지원현황 (subnav 동일 소스 도출 — label/href 재사용) |
| 드로어 · 도구 (접힘) | Swagger · Resume |

**auth** — 메뉴 없음 (변경 없음)

### 3.3 반응형 설계 (NFR)
**R1 — shell 공통 (packages/shell)**
- GlobalHeader: 모바일에서 앱 전환 nav를 **햄버거/앱 스위치 시트**로 제공 (`hidden sm:flex` → 모바일 Menu 버튼으로 앱 목록·앱 내 메뉴 통합)
- SubNav: `overflow-x-auto` 가로 스크롤 (모바일) + `shrink-0` — 또는 R2에서 드로어 통합(디폴트 가로 스크롤로 진행)
- SideDrawer: 현행 fixed slide-in 유지 (모바일·태블릿 공용 OK), 데스크톱도 햄버거로 통일 가능(2단계 — 현행 유지 권장, 드로어는 추가 탐색)
- AppShell main: 콘텐츠 패딩 `p-3 sm:p-4 lg:p-6` 수준의 점진적 여백

**R2 — 페이지별 (앱 단위 점진)**
- 목록/검색 페이지: 테이블 → **`overflow-x-auto` 보장** 우선, 이어서 카드형(모바일 `md:hidden` 카드 + `hidden md:block` 테이블)
- 폼 페이지: `grid-cols-1 md:grid-cols-2` 컬럼 분할
- viewer/편집: 좌측 TOC·우측 미리보기 → 모바일에서는 세로 스택 + 하단 고정 바(이력서 view)
- 우선순위: resume 편집/뷰 · scraper 검색 → platform admin 목록 → 나머지

**R3 — 검증**: 뷰포트 360/768/1280 3종 체크 (브라우저 devtools), 실사용 10종과 병행

### 3.4 아키텍처 (기존 유지)
```
packages/shell: menuTypes.ts(신규) + SubNav/드로어(데이터 소비)
4앱: src/menus.ts(신규, 단일 메뉴 소스) ← Layout/PlatformLayout/App 인라인 배열 이관
AppShell: menu props + items 병행 (기존 소비처 무변경 점진 전환)
```

## 4. 구현 계획
| Phase | 내용 | 게이트 |
|-------|------|--------|
| **P1** | shell `menuTypes` + 4앱 `menus.ts` 분리·구조 재정의(중복 제거·도구 섹션 접기) — **라우팅·동작 무변경, 메뉴만 재배치** | 4앱 빌드·lint, 라이브 메뉴 동일 검증 |
| **P2** | shell 반응형 R1 (헤더 모바일 앱 스위치, SubNav 가로스크롤, 패딩 점진) | 360px 뷰포트 점검 |
| **P3** | 페이지 반응형 R2 — 우선순위: resume 편집·뷰 → scraper 검색 → platform admin | 360/768 검증 |
| **P4** (선택·후속) | 메뉴관리 API(DB) + 관리 UI — `menus.ts`를 API fetch로 교체 | CRUD 별도 설계 |

## 5. 참고 자료
- 현황 코드: `modules/scraper/frontend/src/components/Layout.tsx`, `platform/frontend/src/layouts/PlatformLayout.tsx`, `modules/resume/frontend/src/App.tsx:66-`, `packages/shell/src/{GlobalHeader,SubNav,SideDrawer,AppShell}.tsx`
- 선행: `docs/daily/2026-10-01-todo.md` (실사용 검증 10종)
- 관련: 설계 035(셸 공통화) — 메뉴 props 구조를 만든 곳, 038(토큰)

---
*작성일: 2026-10-01*
