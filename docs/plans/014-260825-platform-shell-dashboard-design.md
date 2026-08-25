# 014-260825 플랫폼 통합 셸(UI/UX) 및 대시보드 고도화 설계 문서

## 개요
- **목적**: platform/scraper/resume 3개 SPA를 GitHub처럼 동일한 셸(헤더+서브내비+접이식 사이드바)로 통일하고, 대시보드에 각 모듈의 실시간 API 데이터를 표시한다.
- **범위**: platform/frontend, modules/scraper/frontend, modules/resume/frontend (백엔드 소폭 추가: 대시보드 집계 API)
- **작성일**: 2026-08-25
- **작성자**: AI Assistant / 사용자

## 1. 배경 및 이유

### 1.1 현재 문제점

| 앱 | 셸 구조 | 문제 |
|----|---------|------|
| platform (`layouts/PlatformLayout.tsx`) | 좌측 사이드바 고정 + 메인 | 대시보드가 모듈 링크 카드 2장뿐 — "메뉴와 차이가 없다" |
| scraper (`components/Layout.tsx` + `CommonHeader.tsx`) | 상단 헤더 + 서브내비 바 | 벨 알림(SSE 수집 진행), 사용자명 있음. 제일 완성도 높음 |
| resume (`CommonHeader.tsx`) | 헤더 안에 탭 박제 | 알림·서브내비 없음, 스크래퍼와 패턴 불일치 |

- 세 앱이 서로 다른 네비게이션 패턴 → 모듈을 옮겨 다니면 "다른 사이트" 느낌
- 대시보드에 실데이터 없음 → 접속할 이유가 약함
- 로그아웃 위치·방식, 로그인 리다이렉트가 앱마다 다름

### 1.2 MSA에서 "한 페이지 같은" 경험 (질문 답변)

**가능하며, 표준적인 방법이다.** GitHub 자체가 저장소별로 나뉜 서비스를 하나의 셸로 묶은 사례.
본 프로젝트는 조건이 유리하다:

1. **동일 origin**: nginx가 `/`, `/scraper/*`, `/resume/*`을 한 도메인으로 프록시 → 세션(localStorage accessToken) 공유됨
2. **JWT 단일화**: auth가 발급한 토큰을 3개 백엔드가 같은 public key로 검증
3. **URL만으로 이동**: `<a href="/resume/">` 전환 — 새로고침처럼 보이지만 셸이 동일하면 끊김이 없음

즉 백엔드는 그대로 두고 **프론트 셸의 시각적 일치 + URL 연속성**으로 해결한다.

## 2. 요구 사항

### 2.1 기능 요구 사항
- [ ] FR-001: 3개 앱이 동일한 글로벌 헤더를 사용한다 (로고, 주메뉴, 우측: 알림·아바타 드롭다운)
- [ ] FR-002: 주메뉴 = 대시보드(/) · 스크래퍼(/scraper/) · 이력서(/resume/) — 현재 앱 하이라이트
- [ ] FR-003: 헤더 아래 **컨텍스트 서브내비**: 앱별 2차 메뉴 (스크래퍼: 검색/스케줄/뷰어, resume: 이력서/공고/지원, platform: 개요/관리)
- [ ] FR-004: 햄버거 클릭 시 **접이식 사이드바**가 슬라이드로 등장 (관리 메뉴, 계정 설정, 빠른링크). 기본 닫힘
- [ ] FR-005: 알림 벨 — scraper SSE 수집 진행(기존) + 추후 지원 상태 변경 등 확장 여지
- [ ] FR-006: 아바타 클릭 드롭다운 — 이름/이메일, 계정 설정, 관리자(ADMIN만), 로그아웃
- [ ] FR-007: 대시보드 실데이터 카드 — 아래 §4 데이터 소스 참조, 각 카드는 해당 화면으로 링크
- [ ] FR-008: 미로그인 시 헤더에 [로그인] 버튼, 대시보드는 공개 요약만 표시

### 2.2 비기능 요구 사항
- 일관성: 색·간격·타이포는 Tailwind 유틸 클래스 토큰으로 고정 (§3.3)
- 성능: 대시보드 집계 API는 캐시 가능한 가벼운 SELECT만 (p95 < 300ms)
- 호환: 기존 페이지 라우팅/기능 변경 없음 — 셸만 교체

## 3. 설계

### 3.1 목표 레이아웃 (GitHub 스타일)

```
┌──────────────────────────────────────────────────────────────┐
│ ☰  SH Platform   대시보드 | 스크래퍼 | 이력서      🔔  👤▼    │ ← GlobalHeader (h-12, slate-900)
├──────────────────────────────────────────────────────────────┤
│  🔍 통합검색  📅 스케줄  📄 뷰어            (현재 앱 컨텍스트) │ ← SubNav (h-10, slate-100/800)
├───────────┬──────────────────────────────────────────────────┤
│ (사이드바) │                                                  │
│  슬라이드  │              페이지 콘텐츠                        │
│  인/아웃   │                                                  │
└───────────┴──────────────────────────────────────────────────┘
```

- 사이드바: 햄버거 토글, 오버레이 방식(콘텐츠 위에 슬라이드), 외부 클릭/Esc로 닫힘
- 서브내비: 앱별 정의를 props로 주입 (`navItems: {label, href, icon, active}[]`)
- 현재 앱 판정: `location.pathname.startsWith("/scraper")` 등

### 3.2 셸 공유 전략 (의사결정 포인트)

| 안 | 방식 | 장점 | 단점 |
|----|------|------|------|
| **A안(채택)** | **복제-동기화**: 셸 컴포넌트 3벌 복제, `SHELL_VERSION` 주석과 체크리스트로 동기화 관리 | 빌드 인프라 변경 0, 배포 워크플로우 무영향, 즉시 착수 | 수정 시 3곳 반복 (셸은 변화가 적어 부담小) |
| B안 | common-ui npm 패키지(file:/workspace) | 진짜 단일 소스 | 3개 package.json/vite/deploy 스크립트 개편 필요 |
| C안 | Module Federation 등 마이크로프론트 | 런타임 공유 | 과공학, 복잡도 급증 |

> 셸 코드는 한 번 잡히면 잘 바뀌지 않는다. B안은 모듈이 4개 이상으로 늘거나
> 셸 수정이 잦아지면 그때 도입 (§6 향후 과정).

파일 규약 (각 앱 동일 위치):
```
src/
├── shell/
│   ├── GlobalHeader.tsx    # 로고+주메뉴+벨+아바타 (props: currentApp, notifications, user)
│   ├── SubNav.tsx          # 2차 메뉴 바 (props: items[])
│   ├── SideDrawer.tsx      # 접이식 사이드바 (props: sections[])
│   ├── AppShell.tsx        # 3종 조립 레이아웃 (각 앱의 Layout이 이것만 사용)
│   └── tokens.ts           # 디자인 토큰(클래스 문자열 상수)
```

### 3.3 디자인 토큰 (tokens.ts)

```ts
export const T = {
  headerBg: "bg-slate-900 text-white",
  headerH: "h-12 px-4",
  subnavBg: "bg-slate-100 border-b border-slate-200 text-slate-600",
  subnavActive: "text-slate-900 font-semibold border-b-2 border-slate-900",
  drawerBg: "bg-slate-900 text-slate-200 w-64",
  card: "bg-white rounded-xl border border-slate-200 shadow-sm",
  accent: "blue", // 스크래퍼 blue / resume green / admin amber
};
```

- 기존 스크래퍼 헤더(slate-900/h-14)를 h-12로 통일하며 3앱 동일화
- lucide-react 아이콘 통일 (이모지 제거)

### 3.4 네비게이션 매핑

| 앱 | 주메뉴 하이라이트 | 서브내비 항목 | 사이드바 섹션 |
|----|------------------|---------------|---------------|
| platform | 대시보드 | 개요 · 관리자(ADMIN만) | 계정 설정, 빠른링크(Swagger×3, Javadoc, 테스트리포트) |
| scraper | 스크래퍼 | 통합검색 · 스케줄등록 · 뷰어 | 크롤러 목록 바로가기, 설정 |
| resume | 이력서 | 이력서 관리 · 공고 탐색 · 지원 관리 | 내 이력서 목록(동적), 대표 이력서 열기 |

### 3.5 알림 통합
- 1단계: scraper SSE 수집 진행 알림을 **전역 벨로 승격** — resume/platform에서도 표시?
  - SSE는 scraper 오리진에서만 받을 수 있으므로, 다른 앱에선 "마지막 수집 결과" 폴링(GET /crawl-logs 최신) 또는 생략
  - 결론: 1단계는 scraper 앱에서만 실시간, 다른 앱 벨에는 정적 요약(최근 수집 결과 링크). 과투자 방지
- 2단계(여유 시): 지원 상태 변경 알림, 마감일 D-1 알림

### 3.6 대시보드 데이터 소스

카드 구성 (클릭 시 해당 화면 이동):

| 카드 | 데이터 | 소스 API | 비고 |
|------|--------|----------|------|
| 수집 공고 총 건수 | job_postings COUNT | 🆕 `GET /scraper/api/v1/stats/summary` | 신규 엔드포인트 |
| 오늘 수집 | 당일 crawledAt COUNT | 〃 | |
| 활성 스케줄 | is_active config COUNT | 〃 | |
| 최근 수집 실행 | crawl_logs 최신 5건 | 기존 `GET /crawl-logs` | 시간/사이트/건수 |
| 내 이력서 N개 | documents COUNT | 기존 `GET /api/v1/documents` | resume |
| 지원 현황 파이프라인 | status별 COUNT | 기존 `GET /api/v1/applications` (클라 집계) | 미니 바차트 |
| 내 스크랩 N건 | scraps COUNT | 기존 `GET /scraper/job-scrap` | |
| 계정 | 사용자명/이메일 | 기존 auth /me | |

- 신규 백엔드는 **scraper `/stats/summary` 하나**뿐 — 나머지는 기존 API 조합
- 미로그인: 계정/이력서/지원 카드 대신 "로그인하고 관리 시작하기" CTA

### 3.7 인증 흐름 통일
- 로그아웃: accessToken/refreshToken 삭제 후 `/` 로 replace (3앱 동일 함수를 tokens.ts 옆 auth.ts에 복제)
- 401 처리: 각 앱 api 클라이언트가 이미 리다이렉트 수행 — 유지
- 로그인 버튼: `/?redirect={현재경로}` (auth 앱이 돌려보냄)

## 4. 구현 계획

| 단계 | 내용 | 예상 분량 |
|------|------|-----------|
| Phase S1 | scraper 셸을 AppShell로 재구성(기준점 제작) — 헤더 h-12, 서브내비 유지, SideDrawer 추가 | 0.5d |
| Phase S2 | platform 셸 교체: 사이드바→AppShell(햄버거 드로어), 대시보드 실데이터 카드 v1(resume/기존 API 먼저) | 1d |
| Phase S3 | resume 셸 교체: CommonHeader→AppShell, 탭을 서브내비로 이동 | 0.5d |
| Phase S4 | scraper `/stats/summary` 추가 + 대시보드 스크래퍼 카드 연결 | 0.5d |
| Phase S5 | 폴리시: 반응형(모바일 서브내비 스크롤), Esc/오버레이 닫기, 접근성(aria) | 0.5d |

각 단계 커밋 단위 종료. 배포는 기존 워크플로우 그대로(프론트 빌드 산출물).

## 5. 리스크 및 대응

| 리스크 | 대응 |
|--------|------|
| 3벌 복제로 셸 드리프트(불일치 재발) | tokens.ts 단일 참조 + 파일 상단 SHELL_VERSION 주석 + 수정 시 체크리스트 |
| scraper 기존 SSE 알림 회귀 | CommonHeader 로직을 GlobalHeader의 notification 영역으로 **이식**, UI만 통일 |
| 대시보드 CORS/인증 | 동일 origin이라 CORS 불필요. JWT는 기존 authHeaders 재사용 |
| 모바일 레이아웃 깨짐 | 주메뉴는 md 미만에서 축약(아이콘), 서브내비 가로 스크롤 |

## 6. 향후 과정 (이 문서 범위 밖)
- common-ui 패키지화(A안→B안 전환 트리거: 모듈 4개+ 또는 월 2회 이상 셸 수정)
- 지원 상태/마감일 알림 (백엔드 이벤트 필요)
- 대시보드 위젯 사용자 설정(드래그 정렬)

---
*작성일: 2026-08-25*
