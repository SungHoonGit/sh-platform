# 038-260930-design-tokens-package-design 설계 문서

## 개요
- **목적**: UI 디자인 룰(공용 CSS·시맨틱 토큰)을 공통 패키지로 단일 소스화한다 (후보: UI 디자인 토큰 패키지화)
- **범위**: `packages/tokens`(신규), 4앱 `index.css`
- **작성일**: 2026-09-30
- **작성자**: AI Assistant / 사용자

## 1. 배경 및 이유
- 4앱이 Tailwind 4 기본 스포트 팔레트(slate/blue/indigo…)를 **직접 하드코딩**해 사용하며 시맨틱 토큰(의미 기반 룰)이 없다.
- **중복 사본**: `.md-view`(react-markdown 공용 스타일, 50줄)가 scraper·resume `index.css`에 **2벌**로 복제되어 있음 — 수정 시 동기화 불가.
- 공통 셸(`packages/shell`)·ui-shared로 레이아웃/컴포넌트는 통합됐으나 **스타일 룰의 단일 소스가 없다.**

## 2. 요구 사항
### 2.1 기능 요구 사항
- [ ] FR-001: `packages/tokens` 신설 — `tokens.css`에 (1) 시맨틱 토큰 `@theme` 레일 (2) 공용 `.md-view` 스타일
- [ ] FR-002: scraper·resume의 중복 `.md-view` 제거 → 패키지 import로 대체 (1벌)
- [ ] FR-003: 4앱 전부 tokens.css import (platform·auth도 기반 확보 — 향후 적용 지점)
- [ ] FR-004: **시각 회귀 0** — 빌드 산출 CSS로 기존과 동일함을 검증

### 2.2 비기능 요구 사항
- 루트 npm workspaces 없음 → 상대경로 `@import`(`@source` 기존 관례)로 해석, vite.config·tsconfig 변경 불필요

## 3. 설계
### 3.1 구조
```
packages/tokens/
├── package.json   (name: @sh-platform/tokens)
└── tokens.css
```
`tokens.css`:
```css
@theme {
  /* 시맨틱 토큰 레일 — 신규 코드부터 사용, 기존 마이그레이션은 후속 단계 */
  --color-canvas: #f8fafc;        /* 페이지 캔버스 (slate-50) */
  --color-surface: #ffffff;       /* 카드·패널 배경 */
  --color-line: #e2e8f0;          /* 경계선 (slate-200) */
  --color-ink: #1e293b;           /* 본문 (slate-800) */
  --color-ink-muted: #64748b;     /* 보조 텍스트 (slate-500) */
  --color-accent: #2563eb;        /* 강조·링크 (blue-600) */
  --color-accent-strong: #1d4ed8; /* 강조 hover (blue-700) */
  --radius-card: 0.75rem;         /* 카드 라운드 (rounded-xl) */
  --shadow-card: 0 1px 2px 0 rgb(0 0 0 / 0.05);  /* shadow-sm */
}
/* .md-view — react-markdown 공용 (scraper·resume 동일 사본을 통합) */
.md-view { ... }  /* 기존 내용 그대로 이식 */
```
### 3.2 적용
| 앱 | 변경 |
|----|------|
| scraper | `.md-view` 45줄 삭제 → `@import "../../../../packages/tokens/tokens.css";` |
| resume | 동일 |
| platform | `@import` 1줄 추가 |
| auth | `@import` 1줄 추가 |

### 3.3 검증 (시각 회귀 0 증명)
- 빌드 후 각 앱 CSS를 기존 dist CSS와 비교 — `.md-view` 규칙 동일성, 추가분은 `@theme` 도입분(유틸리티 미사용 시 헤더 정도)만 허용
- 라이브: 번들 CSS 해시 변경 + `.md-view` 문자열 존재

## 4. 구현 계획
| 단계 | 내용 | 상태 |
|------|------|------|
| Phase 1 | `packages/tokens` + 4앱 index.css 갱신(중복 제거) | 예정 |
| Phase 2 | 게이트: 4앱 build + lint + 기존 dist CSS와 비교 검증 | 예정 |
| Phase 3 | 배포 + 라이브 CSS 마커 검증 | 예정 |
| Phase 4 | 일지·AGENTS 갱신 | 예정 |

## 5. 후속 단계 (이번 범위 밖)
- 시맨틱 토큰의 기존 코드 마이그레이션(shell 공통 컴포넌트 → 4앱, 점진)
- 앱별 테마(다크 등) — 토큰 변수 재정의로 접근 가능

---
*작성일: 2026-09-30*
