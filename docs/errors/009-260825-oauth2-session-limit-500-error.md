# 009-260825 OAuth2 세션 한도 초과가 500으로 떨어지는 문제

## 개요
- **발생일**: 2026-08-25
- **환경**: OCI Ubuntu ARM64, Java 21, Spring Boot 3.4.4 (prod profile)
- **심각도**: 🟡 Warning — auth 서비스 자체는 정상, 단 `APP_SESSION_MAX_PER_USER=1` + 중복차단 설정 시 **기존 세션存活 동안 모든 신규 디바이스 로그인 불가**

## 1. 오류 현상

### 1.1 에러 메시지
```
[SESSION] login blocked: userId=2, sessions=1
java.lang.IllegalStateException: max_sessions_exceeded
    at com.shplatform.auth.domain.SessionService.createSession(SessionService.java:51)
    at com.shplatform.auth.infrastructure.oauth2.OAuth2SuccessHandler.onAuthenticationSuccess(...)

jakarta.servlet.ServletException: Circular view path [error]:
would dispatch back to the current handler URL [/error] again.
```
→ 브라우저에는 **500 Internal Server Error** 페이지로 표시 (PC/모바일 공통)

### 1.2 재현 단계
1. 서버 `.env`: `APP_SESSION_MAX_PER_USER=1`, `APP_SESSION_PREVENT_DUPLICATE=true` 설정 상태
2. PC에서 네이버 간편로그인 성공 → Redis에 세션 1개 생성 (TTL 180분)
3. 다른 기기(모바일)에서 같은 계정 로그인 → 콜백 `/login/oauth2/code/naver`에서 500

### 1.3 오해 포인트
- 처음엔 "모바일만 실패"처럼 보였으나, PC도 **세션 살아있는 동안 재로그인하면 동일하게 500**
- "중복 세션이면 무조건 500"이 맞냐는 질문 → 차단 자체는 의도된 동작이나 **응답 방식이 버그**

## 2. 원인 분석

### 2.1 근본 원인 (2중 구조)

| # | 원인 | 설명 |
|---|------|------|
| 1 | 도메인 예외 위반 | `SessionService`가 프레임워크 예외(`IllegalStateException`)를 던짐. 프로젝트 규칙은 `BusinessException + ErrorCode` |
| 2 | 필터 체인 한계 | OAuth2 Success/Failure Handler는 **DispatcherServlet 이전**(Security Filter)에서 실행 → `@RestControllerAdvice`(GlobalExceptionHandler)가 적용되지 않음 → raw 예외 = 톰캣 500 |
| 3 | 증폭 요인 | `server.error.whitelabel.enabled: false`인데 커스텀 `/error` 뷰 미설정 → 에러 페이지 렌더링마저 `Circular view path` ServletException |

### 2.2 왜 단위 테스트를 통과했나
- `AuthServiceImplTest`는 `SessionService`를 mock → 실제 throw 타입 검증 없음
- OAuth2 흐름 테스트 부재 (SuccessHandler는 테스트 없음)

## 3. 해결 방법

### 3.1 해결 과정
1. 임시 우회: 기존 세션 삭제로 모바일 로그인 먼저 확보
   ```bash
   redis-cli -a 'sh_redis_2026!' DEL "user:sessions:2"
   ```
2. 근본 수정: 도메인 예외화 + 핸들러에서 catch 후 프론트 안내 페이지 리다이렉트

### 3.2 최종 코드 변경 (`7a2aa8a`)
```java
// SessionService — 변경 전
throw new IllegalStateException("max_sessions_exceeded");
// 변경 후
throw new BusinessException(ErrorCode.SESSION_LIMIT_EXCEEDED);  // HttpStatus.CONFLICT(409)

// OAuth2SuccessHandler — 변경 전: createSession 예외가 그대로 필터 밖으로
// 변경 후: 세션 생성 선실행 + catch 시 리다이렉트
try {
    sessionId = sessionService.createSession(userId, ip, device);
    refreshTokenService.save(refreshToken, userId);
} catch (BusinessException e) {
    getRedirectStrategy().sendRedirect(request, response,
            frontendUrl + "/auth/error?message=session_limit");
    return;
}
```
- REST 로그인(`/api/v1/auth/login`)은 GlobalExceptionHandler가 BusinessException을 받아 **409 JSON** 응답
- OAuth2 로그인은 프론트 `/auth/error?message=session_limit` 안내 페이지로 리다이렉트
  ("다른 기기에서 로그인 중입니다..." 메시지 추가)
- 부수 수정: Refresh Token 저장을 세션 생성 **성공 후**로 순서 변경 (차단 시 토큰 잔존 버그 제거)

## 4. 예방 방법

1. **핸들러 체인 예외 규칙**: Security Filter(Success/Failure Handler, Filter 내부)에서는
   `@RestControllerAdvice`가 동작하지 않음 → 반드시 try/catch 후 redirect/response 작성
2. **도메인 예외 통일**: Service에서는 `IllegalStateException` 등 표준 예외 금지,
   `BusinessException + ErrorCode`만 사용 (테스트에서도 ErrorCode 검증 가능)
3. **whitelabel 비활성 시 /error 대책**: 커스텀 ErrorController 또는 JSON error 응답 설정
   (미설정 시 모든 미처리 예외가 2차 ServletException으로 변질돼 원인 추적 어려움)
4. **세션 정책 변경 시 테스트 매트릭스**: PC+모바일 동시 로그인, 로그아웃 후 재로그인,
   TTL 만료 후 재로그인 조합 수동 확인

## 5. 참고 자료
- 관련 문서: `docs/errors/008-260825-admin-controller-bean-conflict-error.md` (동일 날자 auth 장애)
- 관련 코드:
  - `modules/auth/backend/src/main/java/com/shplatform/auth/domain/SessionService.java`
  - `modules/auth/backend/src/main/java/com/shplatform/auth/infrastructure/oauth2/OAuth2SuccessHandler.java`
  - `modules/auth/frontend/src/pages/AuthError.tsx`

---
*작성일: 2026-08-25*
