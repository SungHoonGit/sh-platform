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
        "emailVerified": false
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
| `RATE_LIMITED` | 429 | 요청 과다 (분당 5회) |

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

## 7. 소셜 로그인 (OAuth2)

### GET /auth/oauth2/{provider}

| provider | URL |
|----------|-----|
| kakao | `GET /auth/oauth2/kakao?returnUrl=/dashboard` |
| naver | `GET /auth/oauth2/naver?returnUrl=/dashboard` |
| google | `GET /auth/oauth2/google?returnUrl=/dashboard` |
| github | `GET /auth/oauth2/github?returnUrl=/dashboard` |

### OAuth2 플로우

```
1. 프론트엔드: 사용자를 백엔드 OAuth2 URL로 리다이렉트
   GET /api/v1/auth/oauth2/kakao?returnUrl=/dashboard

2. 백엔드: 카카오 인증 페이지로 리다이렉트
   → 사용자가 카카오에서 동의

3. 카카오 → 백엔드 콜백
   /login/oauth2/code/kakao?code=xxx

4. 백엔드: code → access_token 교환 → 사용자 정보 조회
   → DB에 사용자 없으면 자동 회원가입
   → JWT 발급

5. 백엔드 → 프론트엔드 리다이렉트
   http://localhost:3000/auth/callback?accessToken=xxx&refreshToken=xxx&provider=kakao&returnUrl=/dashboard

6. 프론트엔드: 토큰 저장 + returnUrl로 이동
```

### 프론트엔드 콜백 페이지 (/auth/callback)

URL 파라미터에서 토큰 추출:
- `accessToken`: JWT Access Token
- `refreshToken`: Refresh Token
- `provider`: 소셜 로그인 제공자 (kakao/naver/google/github)
- `returnUrl`: 이전 페이지 경로

### 프론트엔드 에러 페이지 (/auth/error)

URL 파라미터에서 에러 메시지 추출:
- `message`: 에러 코드 (oauth2_failed, email_not_found 등)

### OAuth2 관련 에러

| Code | Status | 상황 |
|------|:------:|------|
| `OAUTH2_FAILED` | 401 | 소셜 로그인 실패 |
| `OAUTH2_USER_NOT_FOUND` | 404 | 소셜 로그인 사용자 없음 |
| `OAUTH2_PROVIDER_ERROR` | 502 | 소셜 로그인 제공자 통신 오류 |

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

## 9. Rate Limiting

| API | 분당 제한 |
|-----|----------|
| POST /auth/login | 5회 |
| POST /auth/verify-email | 3회 |
| POST /auth/verify-code | 3회 |
| 그 외 인증 API | 30회 |

> 제한 초과 시 HTTP 429 + `RATE_LIMITED` 응답

---

## 10. 인증 이벤트 로그

서버 로그에 다음 정보가 기록됩니다 (ISMS-P 2.9.4 준수):

| 이벤트 | 로그 형식 |
|--------|----------|
| 로그인 성공 | `[AUTH] login success: email={}, userId={}, provider=LOCAL` |
| 로그인 실패 | `[AUTH] login failed: email={}, reason={}` |
| OAuth2 로그인 | `[OAUTH2] login success: provider={}, email={}, userId={}` |
| 토큰 갱신 | `[AUTH] token refresh success: userId={}` |
| 로그아웃 | `[AUTH] logout: userId={}` |
| 회원가입 | `[AUTH] signup success: email={}, userId={}` |

---

## 11. 시퀀스 다이어그램

### 일반 로그인

```
┌──────┐    ┌──────────┐    ┌──────────┐
│Client│    │  Auth    │    │   DB     │
│      │    │ Service  │    │(MariaDB) │
└──┬───┘    └────┬─────┘    └────┬─────┘
   │             │               │
   │ POST /login │               │
   │ email, pw   │               │
   ├────────────>│  verify pw    │
   │             │──────────────>│
   │             │  SELECT user  │
   │             │<──────────────│
   │             │               │
   │             │  JWT 발급     │
   │             │  save refresh │
   │             │──────────────>│
   │             │  INSERT token │
   │             │<──────────────│
   │             │               │
   │ 200 + token │               │
   │<────────────│               │
```

### OAuth2 로그인

```
┌──────┐    ┌──────────┐    ┌──────────┐    ┌──────────┐
│Client│    │  Auth    │    │ Provider │    │   DB     │
│      │    │ Service  │    │(Kakao)   │    │(MariaDB) │
└──┬───┘    └────┬─────┘    └────┬─────┘    └────┬─────┘
   │             │               │               │
   │ GET /oauth2 │               │               │
   │ /kakao      │               │               │
   ├────────────>│               │               │
   │             │  Redirect to  │               │
   │             │  kakao.com    │               │
   │<────────────│               │               │
   │             │               │               │
   │  User auth  │               │               │
   │  at Kakao   │               │               │
   ├────────────────────────────>│               │
   │             │               │               │
   │             │  code → token │               │
   │             │  user info    │               │
   │             │<──────────────│               │
   │             │               │               │
   │             │  SELECT/INSERT user           │
   │             │──────────────────────────────>│
   │             │               │               │
   │             │  JWT 발급     │               │
   │             │  save refresh │               │
   │             │──────────────────────────────>│
   │             │               │               │
   │  Redirect to frontend      │               │
   │  with JWT   │               │               │
   │<────────────│               │               │
```

---

## 12. 계정 연결 (Account Linking)

### 개요

같은 이메일로 여러 소셜 프로바이더에서 로그인하면 자동으로 하나의 계정에 연결됩니다.

### 동작 흐름

1. 사용자가 카카오로 로그인 → 새 계정 생성
2. 같은 사람이 네이버로 로그인
3. 백엔드에서 이메일 확인 → 기존 계정 발견
4. 자동으로 네이버 프로바이더 연결
5. 같은 계정으로 로그인 (JWT 발급)

### API 엔드포인트

| Method | URL | 설명 | 인증 |
|--------|-----|------|:----:|
| GET | /auth/oauth2/link-check | 이메일로 연결된 프로바이더 확인 | ❌ |
| POST | /auth/oauth2/link | 기존 계정에 프로바이더 연결 | ❌ |
| GET | /auth/oauth2/providers | 내 연결된 프로바이더 목록 | ✅ |
| DELETE | /auth/oauth2/providers/{provider} | 프로바이더 연결 해제 | ✅ |

---

### 12.1 이메일 연결 확인

### GET /auth/oauth2/link-check

이메일로 이미 연결된 프로바이더가 있는지 확인합니다.

```
Request:
GET /api/v1/auth/oauth2/link-check?email=user@example.com

Response 200:
{
    "code": "SUCCESS",
    "data": {
        "email": "user@example.com",
        "linked": true,
        "providers": ["KAKAO", "NAVER"],
        "providerCount": 2
    }
}
```

| 필드 | 타입 | 설명 |
|------|------|------|
| email | String | 확인할 이메일 |
| linked | Boolean | 연결된 프로바이더 존재 여부 |
| providers | List | 연결된 프로바이더 목록 |
| providerCount | Integer | 연결된 프로바이더 수 |

---

### 12.2 프로바이더 연결

### POST /auth/oauth2/link

기존 계정에 새로운 소셜 프로바이더를 연결합니다.

```
Request:
POST /api/v1/auth/oauth2/link

{
    "email": "user@example.com",
    "password": "Password1!",
    "provider": "NAVER"
}

Response 200:
{
    "code": "SUCCESS",
    "message": "프로바이더가 연결되었습니다.",
    "data": {
        "provider": "NAVER",
        "email": "user@example.com"
    }
}
```

| 필드 | 타입 | 필수 | 설명 |
|------|------|:----:|------|
| email | String | Y | 기존 계정 이메일 |
| password | String | Y | 기존 계정 비밀번호 |
| provider | String | Y | 연결할 프로바이더 |

에러:

| Code | Status | 상황 |
|------|:------:|------|
| PROVIDER_ALREADY_LINKED | 409 | 이미 연결된 프로바이더 |
| USER_NOT_FOUND | 404 | 해당 이메일의 사용자 없음 |

---

### 12.3 연결된 프로바이더 목록

### GET /auth/oauth2/providers

현재 로그인한 사용자에게 연결된 모든 프로바이더를 조회합니다.

```
Request:
GET /api/v1/auth/oauth2/providers
Header: Authorization: Bearer eyJhbGciOi...

Response 200:
{
    "code": "SUCCESS",
    "data": {
        "providers": [
            {"provider": "KAKAO", "connectedAt": "2026-07-12T19:54:08"},
            {"provider": "NAVER", "connectedAt": "2026-07-13T09:30:15"}
        ],
        "count": 2,
        "canUnlink": true
    }
}
```

---

### 12.4 프로바이더 연결 해제

### DELETE /auth/oauth2/providers/{provider}

현재 로그인한 사용자의 특정 프로바이더 연결을 해제합니다.

```
Request:
DELETE /api/v1/auth/oauth2/providers/KAKAO
Header: Authorization: Bearer eyJhbGciOi...

Response 200:
{
    "code": "SUCCESS",
    "message": "프로바이더 연결이 해제되었습니다.",
    "data": {"provider": "KAKAO"}
}
```

에러:

| Code | Status | 상황 |
|------|:------:|------|
| LAST_PROVIDER_CANNOT_UNLINK | 400 | 마지막 프로바이더는 해제 불가 |
| PROVIDER_NOT_FOUND | 404 | 해당 프로바이더가 연결되어 있지 않음 |
