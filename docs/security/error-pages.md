# 에러 페이지 전략 (Error Pages)

> 상태: 검토 문서 (초안)
> 대상: auth / scraper / platform 프론트엔드 + Spring Boot 백엔드 + nginx
> 관련 문서: [OWASP 웹 취약점 대응](./owasp-hardening.md), [API 인증 가이드](../auth/api-auth.md), [Nginx 가이드](../guides/nginx-guide.md)

---

## 1. 목표

1. 사용자가 **빈 화면(white screen)** 을 보는 상황을 제거한다.
2. 404 / 500 / OAuth2 실패 / 인프라 오류(502)에 **브랜딩된 에러 페이지**를 제공한다.
3. 에러 응답에 **스택 트레이스·서버 버전 등 내부 정보가 노출되지 않도록** 한다. (OWASP A05)
4. SPA **딥링크** (예: `/scraper/schedule` 직접 입력)가 404가 아니라 앱으로 진입하도록 한다.

---

## 2. 현재 상태 (검토 결과)

### 2.1 프론트엔드 (React Router v7)

| 앱 | catch-all 라우트 | 에러 페이지 | ErrorBoundary |
|----|:---:|:---:|:---:|
| auth (`/`) | `*` → `<Navigate to="/">` | 없음 | 없음 |
| scraper (`/scraper/`) | `*` → `<Navigate to="/">` | 없음 | 없음 |
| platform (`/platform/`) | **없음** | 없음 | 없음 |

- platform은 catch-all이 없어 `/platform/없는경로` 접근 시 **빈 화면**이 된다.
- 세 앱 모두 ErrorBoundary가 없어 **런타임 JS 에러 = 흰 화면**이다.
  - 실제 사례: 2026-08-01 `platform` App.tsx의 Router 밖 `<Navigate>` 크래시로 흰 화면 재현.
- `/auth/error` 페이지는 기획만 되어 있고 미구현 (roadmap의 "프론트엔드 연동" 항목).

### 2.2 백엔드 (Spring Boot 3.4.4, `common` 모듈)

- `GlobalExceptionHandler` (`common/.../exception/GlobalExceptionHandler.java`)가
  `BusinessException` / `MethodArgumentNotValidException` / `Exception`을
  `ApiResponse.error(code, message)`로 통일 응답 → 스택 트레이스 미노출. ✅
- 다만 **미처리 케이스**가 있음:
  - 404 (`NoResourceFoundException`) — 핸들러 없음 → Whitelabel HTML 응답
  - 405 (`HttpRequestMethodNotSupportedException`) — 핸들러 없음
  - 타입 불일치 400 (`MethodArgumentTypeMismatchException`) — 핸들러 없음
- `server.error.*` 설정 미기입 (기본값: Whitelabel ON, `include-stacktrace=never`).

### 2.3 스크래퍼 SPA 딥링크

- scraper SPA는 **백엔드(8081) 정적 리소스**로 서빙 (nginx 경유).
- `/scraper/` 는 index.html로 정상 서빙되지만, `/scraper/schedule` 같은 딥링크는
  **SPA fallback이 없어 404**. (알려진 한계)

### 2.4 nginx

- `error_page` 설정 없음 → 502/503(배포 재시작 순간)이 기본 nginx 에러 페이지.
- `server_tokens` 설정 없음 → nginx 버전 헤더 노출 가능.
- `error_page` / 보안 헤더는 [OWASP 문서](./owasp-hardening.md)에서 별도 다룸.

---

## 3. 대안 검토

### 3.1 프론트엔드 404 페이지

| 대안 | 내용 | 장점 | 단점 | 판단 |
|------|------|------|------|:---:|
| **A. 앱별 404 페이지 + catch-all** | 각 SPA에 `NotFound` 컴포넌트, `*` 라우트 연결 | 단순, 라우트 단위 제어 | 중복 코드 | ⭐ 권고 |
| B. 공통 `common/frontend` 컴포넌트 | `ErrorPage` 를 공용 모듈로 만들어 재사용 | 일관성, 중복 제거 | 공통 모듈 관리 필요 | 함께 사용 |
| C. nginx error_page 404 | HTTP 레벨에서만 처리 | 서버 다운 시에도 동작 | SPA 내 라우팅 에러 미해결 | 보조 수단 |

### 3.2 프론트엔드 런타임 에러 (ErrorBoundary)

| 대안 | 내용 | 장점 | 단점 | 판단 |
|------|------|------|------|:---:|
| **A. ErrorBoundary 추가** | 각 SPA 루트를 감싸 에러 시 브랜딩된 화면 + "새로고침" 버튼 | 흰 화면 제거 | React 19에서 클래스 컴포넌트 필요 | ⭐ 권고 |
| B. window.onerror 전역 처리 | 에러 감지 후 reload | 간단 | 원인 파악·UX 나쁨 | 비권장 |
| C. 현재 상태 유지 | 없음 | - | 흰 화면 지속 | ❌ |

### 3.3 백엔드 에러 응답 확장

| 대안 | 내용 | 장점 | 단점 | 판단 |
|------|------|------|------|:---:|
| **A. GlobalExceptionHandler 확장** | 404/405/400(타입불일치) 핸들러 추가 | JSON 통일 유지, 최소 변경 | 케이스별 코드 추가 | ⭐ 권고 |
| B. Whitelabel 허용 | Spring 기본 HTML 에러 | 설정 불필요 | 브랜딩·일관성 없음, 노출 위험 | ❌ |
| C. BasicErrorController 커스텀 | 에러 컨트롤러 재정의 | 세밀 제어 | 오버엔지니어링 | 비권장 |

### 3.4 SPA 딥링크 fallback

| 대안 | 내용 | 장점 | 단점 | 판단 |
|------|------|------|------|:---:|
| **A. Spring fallback 컨트롤러** | `/scraper/**` 중 API가 아닌 경로 → index.html forward | 백엔드 단일 처리 | API 경로 예외 처리 필요 | ⭐ 권고 |
| B. nginx try_files | `/scraper/` 를 nginx가 static 서빙하며 fallback | nginx 단일 처리 | 현재 구조(백엔드 static 서빙)와 중복 | 상황 따라 |
| C. 현재 상태 유지 | 딥링크 404 | - | UX 나쁨 | ❌ |

> 참고: scraper SPA를 **nginx static 서빙으로 전환**하는 방안(B)은 "모든 SPA를 nginx가
> 서빙"하는 장기 아키텍처로 전환할 때 함께 검토. 지금은 변경 최소화(A)를 권고.

### 3.5 인프라 오류 페이지 (nginx)

| 대안 | 내용 | 장점 | 단점 | 판단 |
|------|------|------|------|:---:|
| **A. error_page 502/503/504 → 502.html** | 배포 재시작 순간 브랜딩 페이지 | 사용자 경험 개선 | 정적 HTML 관리 필요 | ⭐ 권고 |
| B. 기본 nginx 에러 페이지 | 현재 상태 | - | 영문 기본 페이지 | ❌ |

---

## 4. 권고안 (요약)

| 영역 | 적용 내용 |
|------|-----------|
| 프론트엔드 | 각 SPA에 `NotFound` 페이지 + `*` catch-all 연결 |
| 프론트엔드 | 공용 `ErrorBoundary` (react-error-boundary or 커스텀) → 브랜딩 에러 화면 + 새로고침 |
| 프론트엔드 | `/auth/error` 페이지 (OAuth2 실패 콜백, `?message=` 파라미터) |
| 백엔드 | `GlobalExceptionHandler`에 404/405/400(타입불일치) 핸들러 추가 |
| 백엔드 | `server.error.whitelabel.enabled: false` |
| 백엔드(scraper) | 딥링크 fallback: API 외 `/scraper/**` → index.html forward |
| nginx | 502/503/504 → 브랜딩된 `502.html` (빌드 산출물) |
| nginx | 설정 파일을 저장소에 버전 관리 (`infra/nginx/`) 후 배포 적용 |

---

## 5. 구현 계획 (체크리스트)

- [ ] auth 프론트: `NotFound` + `*` catch-all, ErrorBoundary
- [ ] scraper 프론트: `NotFound` + `*` catch-all, ErrorBoundary
- [ ] platform 프론트: `NotFound` + `*` catch-all (빈 화면 해소), ErrorBoundary
- [ ] auth 프론트: `/auth/error` 페이지 (OAuth2 실패 콜백)
- [ ] common 백엔드: GlobalExceptionHandler 확장 (404/405/400)
- [ ] 4개 모듈 application.yml: `server.error.whitelabel.enabled: false`
- [ ] scraper 백엔드: SPA 딥링크 fallback (API 예외 경로)
- [ ] nginx: `error_page` 502/503/504 + 브랜딩 HTML
- [ ] nginx 설정 저장소 버전 관리 + 배포 워크플로 반영
- [ ] 각 앱: 실제 404/500/502 시나리오 수동 검증

---

## 6. 검증 시나리오

| 시나리오 | 기대 결과 |
|----------|-----------|
| `/platform/없는경로` 직접 접근 | 브랜딩 404 페이지 |
| `/scraper/없는경로` 직접 접근 | 브랜딩 404 페이지 |
| `/scraper/schedule` 딥링크 | 스케줄 화면 (fallback) |
| JS 런타임 에러 발생 | ErrorBoundary 에러 화면 + 새로고침 버튼 |
| OAuth2 실패 콜백 | `/auth/error?message=...` 브랜딩 페이지 |
| API 없는 경로 (예: `GET /api/v1/auth/없음`) | JSON `{code:"NOT_FOUND", message:"..."}` |
| 배포 재시작 중 502 | 브랜딩 502.html |
| 에러 응답에 스택 트레이스 노출 여부 | 노출 없음 (서버 로그에만 기록) |
