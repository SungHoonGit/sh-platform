# 011-260828-ui-shared-guide 가이드

## 개요
- **목적**: MSA 프론트(scraper/resume/platform/auth)가 공용으로 쓰는 React UI 패키지(`packages/ui-shared`)의 구조·사용법·문제 해결 안내
- **대상**: 프론트를 개발하는 AI 에이전트 / 개발자
- **작성일**: 2026-08-28

## 1. 개념
### 1.1 정의
`packages/ui-shared`(별칭 `@sh-platform/ui`)는 모든 프론트가 **소스로 직접 참조**하는 공용 패키지입니다.
- **전역 다이얼로그 오버라이드**: `window.alert/confirm/prompt`를 사이트 스타일의 커스텀 UI로 대체
  - `alert` / `confirm` → **우측 상단 토스트 스택** (여러 개 쌓임, alert는 4초 자동 소멸 + ✕)
  - `prompt`(입력 필요) → **중앙 모달** 유지
- **공용 컴포넌트**: `DialogHost`, `BlockConfirmDialog`, `BlacklistManagerModal`

Spring의 `:common` Gradle 모듈(공용 라이브러리)과 같은 역할을 React 쪽에서 수행합니다.

### 1.2 왜 필요한가
- 같은 컴포넌트(예: 차단 다이얼로그)를 scraper/resume에 **복붙**하던 것을 제거 (단일 소스)
- `alert/confirm/prompt`를 한 번만 구현하면 **어디서 호출해도 자동으로 커스텀** (브라우저 기본 다이얼로그 제거)
- 모든 프론트가 동일한 UI/동작을 공유

### 1.3 배포 모델 (중요)
각 프론트는 **독립 배포**되는 별도 SPA입니다. 따라서 공용 패키지는 "빌드 후 공개"가 아니라 **각 앱이 Vite alias + tsconfig paths로 소스를 직접 끌어다 빌드**합니다.
→ 공용 패키지에 대한 별도 설치/배포가 없어도 되고, 개별 앱 배포에도 안전합니다.

## 2. 설정 방법
### 2.1 사전 조건
- 모든 프론트가 Tailwind 4 + Vite + TS `moduleResolution: bundler` 사용
- 공용 패키지는 **lucide-react 등 외부 UI 의존을 쓰지 않음** (auth는 lucide 미설치 → 아이콘 없는 텍스트 기반)

### 2.2 신규 프론트에 연동하는 4단계
`@sh-platform/ui`를 쓰려면 각 프론트에서 다음 4개 파일을 고친다.

**(1) `vite.config.ts` — alias (런타임 번들링용)**
```ts
import { fileURLToPath, URL } from "node:url";
resolve: {
  alias: [
    { find: /^@sh-platform\/ui$/, replacement: fileURLToPath(new URL("../../packages/ui-shared/src/index.ts", import.meta.url)) },
    { find: /^react\/jsx-runtime$/, replacement: fileURLToPath(new URL("node_modules/react/jsx-runtime.js", import.meta.url)) },
    { find: /^react$/, replacement: fileURLToPath(new URL("node_modules/react", import.meta.url)) },
  ],
},
```
> `../..` 개수는 **vite.config.ts 위치 → repo 루트** 거리. 예: `modules/auth/frontend`는 `../../..` (3칸).

**(2) `tsconfig.app.json` — paths (타입체크용)**
```json
"ignoreDeprecations": "6.0",
"baseUrl": ".",
"paths": {
  "@sh-platform/ui": ["../../packages/ui-shared/src/index.ts"],
  "react": ["node_modules/@types/react"],
  "react/*": ["node_modules/@types/react/*"]
}
```
`include`에 공용 소스를 추가:
```json
"include": ["src", "../../packages/ui-shared/src"]
```
> `react`/`react/*`를 **`@types/react`**로 매핑해야 공용 패키지 파일(앱 `node_modules` 조상 밖)도 react 타입을 찾습니다.
> `baseUrl`은 TS 6.0에서 deprecated → `ignoreDeprecations: "6.0"` 필수.

**(3) `src/index.css` — Tailwind 클래스 스캔 범위 추가**
```css
@import "tailwindcss";
@source "../../../../packages/ui-shared/src";
```
> `@source` 경로는 **index.css 위치(src) → repo 루트** 거리. `src`가 한 단계 깊으므로 vite/tsconfig보다 `..` 1개 더.

**(4) `src/main.tsx` — 루트에 마운트**
```tsx
import { DialogHost, initGlobalDialogs } from "@sh-platform/ui";
initGlobalDialogs();
// <App/> 아래에 <DialogHost />
```

## 3. 사용법
### 3.1 전역 오버라이드
`main.tsx`에서 `initGlobalDialogs()` 호출 후, 기존 코드의 `alert()/confirm()/prompt()`는 그대로 두면 됩니다.
```ts
initGlobalDialogs();
alert("저장되었습니다");           // 우측 상단 토스트 (4초 자동 소멸 + ✕)
const ok = await confirm("정말 삭제할까요?"); // 우측 상단 토스트 + 취소/확인 버튼
const name = await prompt("이름은?");         // 중앙 모달 (입력)
```
- **alert/confirm은 토스트 스택**이라 여러 개가 우측 상단에 세로로 쌓입니다.
- **prompt는 중앙 모달** (입력이 필요해서 토스트 부적합).

⚠️ **confirm/prompt는 Promise 반환으로 바뀜** — 반환값을 쓰는 코드는 반드시:
```ts
const remove = async () => {
  if (!(await confirm("삭제할까요?"))) return;
  ...
};
```

### 3.2 공용 컴포넌트 직접 사용
```tsx
import { BlockConfirmDialog, BlacklistManagerModal } from "@sh-platform/ui";
```
- `BlockConfirmDialog`: 제어형(props: open/company/onCancel/onConfirm)
- `BlacklistManagerModal`: 제어형(props: open/items/onClose/onUnblock/emptyHint, 제네릭 아이템)

## 4. 문제 해결
| 문제 | 원인 | 해결책 |
|------|------|--------|
| `Cannot find module '@sh-platform/ui'` (tsc) | paths 경로의 `..` 개수 오류 (frontend/rota가 아니라 src/ 기준으로 세어 과다) | vite/tsconfig 기준 **frontend 루트 → 루트** 거리로 수정 |
| `TS5101: baseUrl deprecated` | TS 6.0 | `"ignoreDeprecations": "6.0"` 추가 |
| 공용 파일에서 `react/react/jsx-runtime` 타입 없음 | 공용 패키지가 앱 `node_modules` 조상 밖 | paths에 `react`→`node_modules/@types/react` 매핑 |
| `Rolldown failed to resolve react/jsx-runtime` | 런타임 번들링이 공용 위치에서 react 못 찾음 | **vite alias**에 react/react-jsx/runtime 추가 (반드시 필요) |
| `'tsc' is not recognized` (auth) | 해당 프론트 node_modules 미설치 | 해당 디렉토리에서 `npm install` |

## 5. 참고 자료
- 문서: `docs/errors/012-260828-ui-shared-react-build-error.md`
- 관련 개념: Spring `:common` 공용 모듈(AGENTS.md), 모노레포 구조(AGENTS.md)

---
*작성일: 2026-08-28*
