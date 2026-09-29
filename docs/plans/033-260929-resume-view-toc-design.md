# 033-260929-resume-view-toc-design

# 이력서 뷰 목차(TOC) 사이드바 설계

## 개요
- **목적**: 이력서 뷰 페이지에 사람인식 우측 앵커 내비게이션(목차)을 제공 — 긴 이력서 스크롤 시 섹션 간 이동·현재 위치 파악
- **범위**: `ResumeViewPage`(로그인 뷰) + `ShareViewPage`(공유 뷰), 템플릿 3종(CLASSIC/MODERN/SARAMIN)
- **작성일**: 2026-09-29
- **작성자**: AI Assistant + 사용자

## 1. 배경 및 이유
- 설계 012 **FR-204**: "뷰 목차(TOC) 사이드바 — 사람인식 우측 앵커 내비게이션" (Phase 6~8 미구현 잔여)
- 설계 021 **FR-005**: 우측 고정 네비게이션 — 섹션 이동 편의
- 편집 페이지(EditPage) 우측 섹션 패널(순서 변경/보임·숨김 + `section-{key}` 앵커 스크롤)은 이미 구현되어 있으나, **뷰 페이지에는 목차가 없어** 긴 이력서에서 섹션 탐색이 불가능
- 범위 확정: 편집기 네비는 기존 존재 → **뷰 페이지 TOC만** 신규 구현

## 2. 요구 사항
- [ ] FR-301: 뷰 페이지 우측 sticky 목차 — 포함된 섹션 목록 표시
- [ ] FR-302: 목차 항목 클릭 → 해당 섹션으로 smooth scroll (EditPage와 동일한 `section-{key}` 앵커 규약)
- [ ] FR-303: 스크롤 위치에 따른 현재 섹션 하이라이트 (IntersectionObserver + 바닥 도달 보정)
- [ ] FR-304: 목차는 실제 렌더링되는(데이터가 존재하는) 섹션만 표시, 빈 섹션 제외
- [ ] FR-305: 데스크톱(≥lg 1024px)에서만 표시, 미만/인쇄(print)에서는 숨김 — 모바일·PDF 출력 영향 없음
- [ ] FR-306: 템플릿 3종 공통 적용 — 템플릿별 섹션 래퍼(`section`/`div`)에 앵커 id 주입

### 비기능
- 성능: IntersectionObserver + passive scroll (페이지당 observer 1개)
- 접근성: `<nav aria-label="목차">`, 버튼 요소 사용
- 출력: 인쇄 시 목차 숨김(기존 print 클래스 체계 유지)

## 3. 설계

### 3.1 앵커 규약
- 섹션 래퍼에 `id="section-{key}"` (EditPage `scrollTo`와 동일 규약, key = SECTION_LABELS 키 7종)
- 템플릿별 주입 지점:
  - ClassicTemplate: `Section` 컴포넌트 (`<section id>`)
  - SaraminTemplate: `Section` 컴포넌트 (`<section id>`)
  - ModernTemplate: `MainSection` (`<section id>`) + `SideSection` (`<div id>`) — 사이드바(연락처)의 "연락처" Section은 데이터 섹션이 아니므로 id 미부여
- nodes 레코드의 각 섹션에 `id="section-careers"` 형태로 리터럴 전달

### 3.2 TocNav 컴포넌트 (신규 `components/TocNav.tsx`)
```
입력: view: ResumeView, order: string[]
출력: <nav> sticky 목차 (없으면 null)
```
- **항목 구성**: `order` 순서대로, `view[key]` 배열이 비어있지 않은 섹션만 (`SECTION_LABELS`로 표시명) — 템플릿의 null 렌더 규칙과 동일(자격증 숨김 필터 적용 후 `filteredView` 기준)
- **하이라이트**: IntersectionObserver 밴드 `rootMargin: "-8% 0px -70% 0px"` (상단 8~30% 구간 = 현재 섹션) + **바닥 도달 보정** (스크롤이 문서 하단 근처면 마지막 항목 활성화 — 짧은 마지막 섹션이 밴드에 닿지 않는 케이스 보정)
- **클릭**: `document.getElementById('section-{key}').scrollIntoView({behavior:'smooth', block:'start'})` + 즉시 활성값 갱신
- **스타일**: `hidden lg:block w-44 shrink-0 print:hidden`, 내부 `sticky top-6` 흰 카드 — 활성 항목은 teal border-left + font-semibold (Modern 템플릿 teal 계열과 톤 일치)

### 3.3 페이지 레이아웃 (ResumeViewPage / ShareViewPage 공통)
```
<div className="max-w-5xl mx-auto px-4 flex gap-6">   ← 기존 max-w-3xl 대비 확장
  <div className="flex-1 min-w-0">                     ← 좌측: 툴바 + 문서 (기존 max-w-3xl 유지)
    툴바 (기존 그대로)
    <div className="max-w-3xl mx-auto ...">템플릿</div>
  </div>
  <TocNav view={filteredView} order={order} />        ← 우측: 목차 (lg 미만/인쇄 숨김)
</div>
```
- 좌측 컬럼에 기존 `max-w-3xl mx-auto` 유지 → TOC 미표시 화면(모바일·태블릿)에서 레이아웃 변화 없음
- Modern 템플릿은 자체 `max-w-3xl mx-auto` 내장 → 좌측 컬럼 내에서 중첩 무해

### 3.4 제외 범위
- EditPage 우측 섹션 패널 — 기존 구현 존재(순서/보임 토글 + 앵커 스크롤)
- PDF 출력(OpenPDF) — 서버 사이드 렌더, TOC 무관
- 편집 미리보기 모달 — 범위 외 (수요 발생 시 추가)

## 4. 구현 계획
| 단계 | 내용 | 비고 |
|------|------|------|
| Phase 1 | 템플릿 3종 섹션 앵커 id 주입 | Classic/Modern/Saramin |
| Phase 2 | `TocNav.tsx` 신규 + ResumeViewPage·ShareViewPage 레이아웃 반영 | FE 전용 |
| Phase 3 | 검증: `npm run build` (tsc -b) → 커밋 → CI 배포 → 실서버 번들 확인 | 백엔드 무관 |

## 5. 참고 자료
- `docs/plans/012-260824-resume-platform-roadmap-design.md` (FR-204)
- `docs/plans/021-260827-resume-benchmark-design.md` (FR-005, §3.3)
- `modules/resume/frontend/src/pages/EditPage.tsx:339` (`section-{key}` 앵커 규약)
- `modules/resume/frontend/src/components/templates/shared.tsx` (SECTION_LABELS/DEFAULT_ORDER)

---
*작성일: 2026-09-29*
