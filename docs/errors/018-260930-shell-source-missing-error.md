# 018-260930-shell-source-missing-error 오류 기록

## 개요
- **발생일**: 2026-09-30 (사용자 보고: 로그인 세션 정리 후 header 공통 CSS 깨짐)
- **환경**: 4앱 프론트엔드 (Tailwind 4 + Vite), 라이브
- **심각도**: 🔴 Critical — 3앱 헤더·서브내비·사이드드로어 스타일 유실 (3b 배포 9/29부터 잠재)

## 1. 오류 현상
### 1.1 증상 (사용자 보고)
- platform: 헤더가 **하얗게** (배경색 없음)
- resume: 헤더 영역 **아예 안 보임**, 사이드 드로어가 왼쪽에서 안 나옴·이상함
- scraper: 동일하게 이상

### 1.2 라이브 CSS 검증 결과 (유실 증거)
| 클래스 | 용도 | 유실 전 (라이브) |
|--------|------|------|
| `bg-slate-900` | 헤더 배경 (T.headerBg) | platform **0** |
| `h-12` | 헤더 높이 (T.headerH) | scraper·resume **0** |
| `w-64` | 사이드 드로어 폭 (T.drawerBg) | resume·platform **0** |

## 2. 원인 분석
### 2.1 근본 원인
설계 035 3b에서 3앱의 shell 사본을 `packages/shell`로 옮기면서
**4앱 `index.css`의 `@source`에 `packages/shell/src`를 추가하지 않음**.

Tailwind 4는 `@source`로 스캔한 파일에서만 유틸리티 클래스를 출력한다.
앱 밖(`packages/`)의 shell 파일이 스캔되지 않아
`T.headerBg`·`T.headerH`·`T.drawerBg` 등 config.ts의 토큰 클래스가 **모든 앱 CSS에서 제외**.

- 앱 자체 코드에 같은 클래스가 있으면 우연히 살아남음 (platform의 `bg-slate-900`=0, scraper=1)
- ui-shared는 `@source`에 있었고 shell만 누락 → 3b(9/29)부터 유실, 038 토큰 작업의 "CSS 완전 동일" 검증이 **이미 깨진 상태를 그대로 비교**해 통과함 (검증 사각지대)

### 2.2 관련 코드
- `modules/*/frontend/src/index.css`: `@source "../../../../packages/ui-shared/src"`만 존재
- `packages/shell/src/config.ts:6-25` — `T` 토큰 클래스 전부 유실 대상

## 3. 해결 방법
### 3.1 해결 과정
4앱 `index.css`에 `@source "…/packages/shell/src"` 추가 → 빌드 CSS에서 `h-12`·`bg-slate-900`·`w-64` 0→1 복원 확인 → 배포(36675035401) → 라이브 4앱 CSS가 로컬 빌드와 동일·클래스 전부 존재.

### 3.2 최종 코드 변경
```css
@import "tailwindcss";
@import "../../../../packages/tokens/tokens.css";
@source "../../../../packages/ui-shared/src";
@source "../../../../packages/shell/src";   /* ← 추가 */
```

## 4. 예방 방법
- **패키지 클래스가 앱 밖에 있으면 반드시 해당 앱 `@source`에 추가할 것** — shell/core/ui-shared/tokens 전부 해당
- 빌드 CSS 비교 검증은 "기존과 동일"만으로 회귀 보장이 안 됨 — 기존이 이미 깨진 상태일 수 있음 → **핵심 클래스 마커 존재 여부를 라이브에서 직접 확인**할 것
- 새 패키지 추가 체크리스트: (1) 4앱 vite alias (2) tsconfig paths (3) **4앱 @source** (4) 빌드 마커 검증

## 5. 참고 자료
- 커밋 `866c30a` (fix 2번)
- 관련: `docs/errors/017-260930-resume-service-arg-order-reversed-error.md` (같은 배포)

---
*작성일: 2026-09-30*
