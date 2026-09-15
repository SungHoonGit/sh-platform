# SH Platform Frontend Integration Guide

## API Base URL

```
https://sunghoonyk.duckdns.org
```

> `/api` prefix 사용 — nginx에서 Spring Boot 8080으로 자동 프록시

---

## 인증 API

### 1. 회원가입

```
POST /api/v1/auth/signup
Content-Type: application/json

{
    "email": "user@example.com",
    "password": "Password1!",
    "name": "홍길동"
}

→ 201 Created
```

### 2. 로그인

```
POST /api/v1/auth/login
Content-Type: application/json

{
    "email": "user@example.com",
    "password": "Password1!"
}

→ 200 OK
{
    "data": {
        "accessToken": "eyJ...",
        "refreshToken": "uuid...",
        "expiresIn": 3600,
        "tokenType": "Bearer"
    }
}
```

### 3. 토큰 갱신

```
POST /api/v1/auth/refresh
Content-Type: application/json

{
    "refreshToken": "uuid..."
}

→ 200 OK (기존 토큰 폐기 + 새 토큰 발급)
```

### 4. 로그아웃

```
POST /api/v1/auth/logout
Authorization: Bearer eyJ...

{
    "refreshToken": "uuid..."
}

→ 200 OK
```

---

## OAuth2 소셜 로그인

### 프론트에서 해야 할 일

1. 사용자가 소셜 로그인 버튼 클릭
2. 아래 URL로 리다이렉트:

```
GET /oauth2/authorization/{provider}?redirect_uri={frontend_callback_url}
```

| provider | 비고 |
|----------|------|
| kakao | 카카오 로그인 |
| naver | 네이버 로그인 |
| google | 구글 로그인 |
| github | 깃헙 로그인 |

예시:
```
<a href="https://sunghoonyk.duckdns.org/oauth2/authorization/kakao?redirect_uri=https://frontend.example.com/auth/callback">
  카카오 로그인
</a>
```

### 콜백 페이지 (/auth/callback)

OAuth2 성공 시 백엔드가 프론트 콜백 URL로 리다이렉트:

```
https://frontend.example.com/auth/callback?accessToken=eyJ...&refreshToken=uuid...&provider=kakao
```

프론트에서 할 처리:
1. URL 파라미터에서 `accessToken`, `refreshToken`, `provider` 추출
2. `localStorage`에 저장
3. 메인 페이지로 이동

### 에러 처리

OAuth2 실패 시:

```
https://frontend.example.com/auth/error?message=oauth2_failed
```

---

## 공통 에러 응답

| HTTP | Code | 상황 |
|:----:|------|------|
| 400 | INVALID_INPUT | 입력값 검증 실패 |
| 401 | UNAUTHORIZED | 로그인 실패 |
| 401 | TOKEN_EXPIRED | 토큰 만료 → refresh 필요 |
| 403 | EMAIL_NOT_VERIFIED | 이메일 미인증 |
| 409 | DUPLICATE_EMAIL | 이메일 중복 |
| 429 | RATE_LIMITED | 요청 과다 |

모든 에러 응답 형식:

```json
{
    "code": "ERROR_CODE",
    "message": "사용자에게 보여줄 메시지"
}
```

---

## 개발 환경

| 항목 | 값 |
|------|-----|
| API | `https://sunghoonyk.duckdns.org` |
| 인증 | JWT Bearer Token (RS256, 1h) |
| Refresh Token | 14일, Rotation 방식 |
| CORS | 프론트 도메인 허용 필요 시 요청 |

---

## 관련 자료

- 프론트 구현 샘플 코드: `docs/auth/frontend-auth-guide.md`
- ERD: `docs/architecture/erd.md`
- 전체 API 명세: `docs/auth/api-auth.md`
