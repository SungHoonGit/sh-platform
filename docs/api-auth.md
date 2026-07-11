# 인증 API 명세

Base URL: `https://api.sung-hoon.io/api/v1`

---

## 1. 회원가입

### POST /auth/signup

```
Request:
Content-Type: application/json

{
    "email": "user@example.com",
    "password": "Password1!",
    "name": "홍길동"
}

Response 201:
{
    "code": "CREATED",
    "message": "회원가입이 완료되었습니다.",
    "data": {
        "id": 1,
        "email": "user@example.com",
        "name": "홍길동",
        "role": "USER",
        "provider": "LOCAL",
        "emailVerified": false,
        "createdAt": "2026-07-11T09:00:00"
    }
}
```

| 필드 | 타입 | 필수 | 설명 |
|------|------|:----:|------|
| email | String | ✅ | 이메일 형식 |
| password | String | ✅ | 8~20자, 영문+숫자+특수문자 포함 |
| name | String | ✅ | 2~20자 |

에러:

| Code | Status | 상황 |
|------|:------:|------|
| `DUPLICATE_EMAIL` | 409 | 이미 가입된 이메일 |
| `INVALID_INPUT` | 400 | 입력값 검증 실패 |

---

## 2. 이메일 인증코드 발송

### POST /auth/verify-email

```
Request:
{
    "email": "user@example.com",
    "purpose": "SIGNUP"
}

Response 200:
{
    "code": "SUCCESS",
    "message": "인증 메일이 발송되었습니다."
}
```

| 필드 | 타입 | 설명 |
|------|------|------|
| purpose | String | `SIGNUP` / `RESET_PASSWORD` |

> Gmail SMTP로 6자리 코드 발송. 코드 5분 만료.

---

## 3. 인증코드 확인

### POST /auth/verify-code

```
Request:
{
    "email": "user@example.com",
    "code": "123456",
    "purpose": "SIGNUP"
}

Response 200:
{
    "code": "SUCCESS",
    "message": "이메일 인증이 완료되었습니다."
}
```

에러:

| Code | Status | 상황 |
|------|:------:|------|
| `INVALID_CODE` | 400 | 코드 불일치 |
| `CODE_EXPIRED` | 400 | 코드 만료 |

---

## 4. 로그인

### POST /auth/login

```
Request:
{
    "email": "user@example.com",
    "password": "Password1!"
}

Response 200:
{
    "code": "SUCCESS",
    "message": "로그인 성공",
    "data": {
        "accessToken": "eyJhbGciOi...",
        "refreshToken": "dGhpcyBpcy...",
        "expiresIn": 3600,
        "tokenType": "Bearer"
    }
}
```

| 필드 | 설명 |
|------|------|
| accessToken | JWT (RS256), 1시간 유효 |
| refreshToken | UUID, 14일 유효, DB 저장 |

에러:

| Code | Status | 상황 |
|------|:------:|------|
| `UNAUTHORIZED` | 401 | 이메일/비번 불일치 |
| `EMAIL_NOT_VERIFIED` | 403 | 이메일 미인증 |

---

## 5. 토큰 갱신

### POST /auth/refresh

```
Request:
{
    "refreshToken": "dGhpcyBpcy..."
}

Response 200:
{
    "code": "SUCCESS",
    "data": {
        "accessToken": "eyJhbGciOi...",
        "refreshToken": "bmV3IHRva2Vu...",
        "expiresIn": 3600,
        "tokenType": "Bearer"
    }
}
```

> Refresh Token 사용 시 **기존 토큰 폐기 + 새 토큰 발급** (Rotation)

에러:

| Code | Status | 상황 |
|------|:------:|------|
| `TOKEN_EXPIRED` | 401 | Refresh Token 만료 → 재로그인 |
| `TOKEN_INVALID` | 401 | 유효하지 않은 토큰 |

---

## 6. 로그아웃

### POST /auth/logout

```
Header:
Authorization: Bearer eyJhbGciOi...

Request:
{
    "refreshToken": "dGhpcyBpcy..."
}

Response 200:
{
    "code": "SUCCESS",
    "message": "로그아웃 되었습니다."
}
```

> Refresh Token DB에서 삭제. Access Token은 클라이언트에서 폐기.

---

## 7. 소셜 로그인

### GET /auth/oauth2/{provider}

| provider | URL |
|----------|-----|
| kakao | `GET /auth/oauth2/kakao` |
| naver | `GET /auth/oauth2/naver` |
| google | `GET /auth/oauth2/google` |
| github | `GET /auth/oauth2/github` |

> 각 플랫폼 OAuth2 인증 페이지로 리다이렉트.
> 인증 완료 후 콜백 → 회원가입/로그인 처리 → JWT 발급 → 프론트엔드 리다이렉트.

---

## 8. JWT 명세

```json
Header:
{
  "alg": "RS256",
  "typ": "JWT"
}

Payload:
{
  "sub": "1",                    // user.id
  "email": "user@example.com",
  "role": "USER",
  "iat": 1720666800,             // 발급 시간
  "exp": 1720670400              // 만료 시간 (+1h)
}
```

| 항목 | 값 |
|------|-----|
| 알고리즘 | RS256 (비대칭키) |
| Access Token 만료 | 1시간 |
| Refresh Token 만료 | 14일 |
| Refresh Token 저장 | MariaDB `refresh_tokens` 테이블 |

---

## 9. 시퀀스 다이어그램

```
┌──────┐    ┌──────────┐    ┌──────────┐    ┌──────────┐
│Client│    │  Auth    │    │  Mail    │    │   DB     │
│      │    │ Service  │    │ Service  │    │(MariaDB) │
└──┬───┘    └────┬─────┘    └────┬─────┘    └────┬─────┘
   │             │               │               │
   │ POST /signup│               │               │
   │ email, pw   │               │               │
   ├────────────>│  BCrypt hash  │               │
   │             │──────────────>│               │
   │             │  INSERT user  │               │
   │             │<──────────────│               │
   │             │               │               │
   │ 201 Created │               │               │
   │<────────────│               │               │
   │             │               │               │
   │ POST /login │               │               │
   │ email, pw   │               │               │
   ├────────────>│  verify pw    │               │
   │             │  JWT 발급     │               │
   │             │──────────────>│               │
   │             │  save refresh │               │
   │             │<──────────────│               │
   │ 200 + token │               │               │
   │<────────────│               │               │
   │             │               │               │
   │  (API 호출) │               │               │
   │ Authorization: Bearer JWT   │               │
   ├─────────────────────────────>               │
   │  (Gateway JWT 검증 후 라우팅)                │
```



---

## 10. GitHub OAuth2 설정 (예시)

```yaml
spring:
  security:
    oauth2:
      client:
        registration:
          github:
            client-id: ${GITHUB_CLIENT_ID}
            client-secret: ${GITHUB_CLIENT_SECRET}
            scope: user:email
```

> Kakao/Naver/Google도 동일 패턴. 각각 `registration.{provider}` 추가.
> 콜백 URL: `https://api.sung-hoon.io/login/oauth2/code/{provider}`
