# 012-260828-ui-shared-react-build-error 오류 기록

## 개요
- **발생일**: 2026-08-28
- **환경**: Windows, React 19, Vite 8, TS 6.0 (moduleResolution bundler)
- **심각도**: 🟡 Warning (도입 초기에 3종 빌드 오류 → 해결 완료)

## 1. 오류 현상
공용 패키지(`packages/ui-shared`)를 scraper/resume/platform/auth 프론트에 소스로 연동하던 중
순차적으로 3가지 오류가 발생.

### 1.1 에러 메시지
```
error TS5101: Option 'baseUrl' is deprecated and will stop functioning in TypeScript 7.0.
error TS2307: Cannot find module '@sh-platform/ui' or its corresponding type declarations.
error TS7026: JSX element implicitly has type 'any' because no interface 'JSX.IntrinsicElements' exists.
error TS7016: Could not find a declaration file for module 'react'.
[rolldown]: Failed to resolve import "react/jsx-runtime" from ".../packages/ui-shared/src/BlacklistManagerModal.tsx".
'tsc' is not recognized as an internal or external command (auth frontend)
```

### 1.2 재현 단계
1. 공용 패키지를 만들고 `@sh-platform/ui` alias/paths로 각 프론트에 연결
2. 특정 프론트에서 `npm run build`(tsc -b && vite build) 실행

## 2. 원인 분석
### 2.1 근본 원인
공용 소스가 **각 앱의 `node_modules` 조상 밖**(`packages/ui-shared`)에 있어서 react 타입·런타임을
자동 해석하지 못하고, TS 6.0의 `baseUrl` deprecation 및 경로 계산 오류가 겹침.

| 오류 | 원인 |
|------|------|
| TS5101 (baseUrl deprecated) | TypeScript 6.0에서 `baseUrl` deprecated |
| Cannot find module @sh-platform/ui | `paths`의 `..` 개수를 `src/` 기준이 아니라 frontend/기준 4칸으로 잘못 계산 (실제 3칸) |
| JSX/react 타입 없음 (공용 파일) | 공용 패키지가 앱 `node_modules` 업스트림이 아니라 react 타입 미해석 |
| Rolldown react/jsx-runtime | TS는 되지만 **런타임 번들**은 Vite가 공용 위치에서 react를 못 찾음 |
| tsc not recognized (auth) | auth 프론트 `node_modules` 미설치 (한 번도 로컬 빌드 안 됨) |

### 2.2 관련 코드
- `packages/ui-shared/` — 공용 소스
- 각 프론트 `vite.config.ts` / `tsconfig.app.json` / `src/index.css`

## 3. 해결 방법
1. **경로 개수**: vite/tsconfig는 `frontend/루트 → repo 루트` 거리로(`modules/auth/frontend`=3칸, `platform/frontend`=2칸). index.css(`src/`)는 1칸 더.
2. **react 타입**: tsconfig paths에 `"react": ["node_modules/@types/react"]` + `"react/*"` 매핑 (공용 파일도 같은 타입 사용).
3. **baseUrl**: `"ignoreDeprecations": "6.0"` 추가 후 유지.
4. **런타임 react**: vite `resolve.alias`에 `react`/`react/jsx-runtime`을 앱 `node_modules/react`로 재정의.
5. **auth 설치**: `modules/auth/frontend`에서 `npm install`.

## 4. 예방 방법
- 새 프론트 연동 시: 가이드 `docs/guides/011-260828-ui-shared-guide.md`의 "2.2 신규 프론트 연동 4단계" 그대로.
- `@source`/paths/alias 세 곳 모두 누락 없이 넣을 것 (셋 다 필요).

## 5. 참고 자료
- 가이드: `docs/guides/011-260828-ui-shared-guide.md`
- 관련: AGENTS.md 모노레포 구조

---
*작성일: 2026-08-28*
