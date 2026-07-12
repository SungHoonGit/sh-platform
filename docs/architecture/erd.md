# ERD (데이터 모델)

## 1. 표기법

- `PK`: Primary Key
- `FK`: Foreign Key
- `UQ`: Unique
- `NN`: Not Null
- `idx_`: 인덱스

---

## 2. users — 사용자

```sql
CREATE TABLE users (
    id              BIGINT          AUTO_INCREMENT PRIMARY KEY,
    email           VARCHAR(100)    NOT NULL UNIQUE,
    password        VARCHAR(200),                           -- BCrypt, 소셜 회원은 NULL
    name            VARCHAR(50)     NOT NULL,
    role            ENUM('USER', 'ADMIN') NOT NULL DEFAULT 'USER',
    provider        VARCHAR(20)     NOT NULL DEFAULT 'LOCAL', -- LOCAL/KAKAO/NAVER/GOOGLE/GITHUB
    provider_id     VARCHAR(100),                           -- 소셜 플랫폼 고유 ID
    email_verified  BOOLEAN         NOT NULL DEFAULT FALSE,
    locale          VARCHAR(10)     NOT NULL DEFAULT 'ko',  -- 다국어 설정
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_users_email (email),
    INDEX idx_users_provider (provider, provider_id)
);
```

| 컬럼 | 설명 |
|------|------|
| `password` | LOCAL 회원만 저장, 소셜은 NULL |
| `provider` | 가입 경로 (`LOCAL`, `KAKAO`, `NAVER`, `GOOGLE`, `GITHUB`) |
| `provider_id` | 소셜 로그인 시 각 플랫폼의 고유 사용자 ID |
| `locale` | `ko`, `en` — 추후 메시지/이메일 언어 결정 |

---

## 3. refresh_tokens — 리프레시 토큰

```sql
CREATE TABLE refresh_tokens (
    id              BIGINT          AUTO_INCREMENT PRIMARY KEY,
    user_id         BIGINT          NOT NULL,
    token           VARCHAR(500)    NOT NULL UNIQUE,
    expires_at      DATETIME        NOT NULL,
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_refresh_token (token),
    INDEX idx_refresh_user (user_id),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);
```

| 컬럼 | 설명 |
|------|------|
| `token` | JWT Refresh Token (UUID or JWT) |
| `expires_at` | 만료 시간 (예: 14일 후) |

---

## 4. verification_codes — 이메일 인증

```sql
CREATE TABLE verification_codes (
    id              BIGINT          AUTO_INCREMENT PRIMARY KEY,
    email           VARCHAR(100)    NOT NULL,
    code            VARCHAR(6)      NOT NULL,
    purpose         ENUM('SIGNUP', 'RESET_PASSWORD') NOT NULL,
    expires_at      DATETIME        NOT NULL,
    verified        BOOLEAN         NOT NULL DEFAULT FALSE,
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_verification_email (email, purpose)
);
```

| 컬럼 | 설명 |
|------|------|
| `code` | 6자리 숫자 인증코드 |
| `purpose` | 용도 구분 (회원가입 / 비밀번호 재설정) |
| `expires_at` | 5분 후 만료 |
| `verified` | 인증 완료 시 TRUE |

---

## 5. roles — 권한 (추후 확장)

```sql
CREATE TABLE roles (
    id              BIGINT          AUTO_INCREMENT PRIMARY KEY,
    name            VARCHAR(30)     NOT NULL UNIQUE,
    description     VARCHAR(200)
);

CREATE TABLE user_roles (
    user_id         BIGINT          NOT NULL,
    role_id         BIGINT          NOT NULL,
    PRIMARY KEY (user_id, role_id),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (role_id) REFERENCES roles(id) ON DELETE CASCADE
);
```

> Phase 2 초기에는 `users.role` 단일 컬럼으로 충분. Phase 4 MSA 확장 시 roles/user_roles 분리.

---

## 6. ERD 다이어그램

```
┌────────────────────────────────────────────────────────────────────┐
│                          users                                      │
├────────────────────────────────────────────────────────────────────┤
│ PK │ id                BIGINT                                      │
│ UQ │ email             VARCHAR(100)  NN                            │
│    │ password          VARCHAR(200)  (BCrypt, NULL for social)      │
│    │ name              VARCHAR(50)   NN                            │
│    │ role              ENUM          DEFAULT 'USER'               │
│    │ provider          VARCHAR(20)   DEFAULT 'LOCAL'               │
│    │ provider_id       VARCHAR(100)  (social only)                  │
│    │ email_verified    BOOLEAN       DEFAULT FALSE                 │
│    │ locale            VARCHAR(10)   DEFAULT 'ko'                  │
│    │ created_at        DATETIME      NN                            │
│    │ updated_at        DATETIME      NN (ON UPDATE)                │
└────────────────────┬───────────────────────────────────────────────┘
                     │ 1
                     │
                     │ N
┌────────────────────┴───────────────────────────────────────────────┐
│                     refresh_tokens                                  │
├────────────────────────────────────────────────────────────────────┤
│ PK │ id                BIGINT                                      │
│ FK │ user_id           BIGINT        NN  (→ users.id)             │
│ UQ │ token             VARCHAR(500)  NN                            │
│    │ expires_at        DATETIME      NN                            │
│    │ created_at        DATETIME      NN                            │
└────────────────────────────────────────────────────────────────────┘

┌────────────────────────────────────────────────────────────────────┐
│                     verification_codes                              │
├────────────────────────────────────────────────────────────────────┤
│ PK │ id                BIGINT                                      │
│    │ email             VARCHAR(100)  NN                            │
│    │ code              VARCHAR(6)    NN                            │
│    │ purpose           ENUM          (SIGNUP/RESET_PASSWORD)       │
│    │ expires_at        DATETIME      NN                            │
│    │ verified          BOOLEAN       DEFAULT FALSE                 │
│    │ created_at        DATETIME      NN                            │
└────────────────────────────────────────────────────────────────────┘
```

---

## 7. 추후 확장 테이블 (Phase 3~4)

### housing 테이블 (ai-housing)

```sql
CREATE TABLE articles (
    id              BIGINT          AUTO_INCREMENT PRIMARY KEY,
    title           VARCHAR(200)    NOT NULL,
    content         TEXT,
    source          VARCHAR(50),                        -- SH/LH
    url             VARCHAR(500),
    posted_at       DATE,
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP
);
```

### job_scraper 연동

```sql
CREATE TABLE job_postings (
    id              BIGINT          AUTO_INCREMENT PRIMARY KEY,
    company         VARCHAR(100)    NOT NULL,
    title           VARCHAR(200)    NOT NULL,
    skills          VARCHAR(500),
    location        VARCHAR(100),
    url             VARCHAR(500),
    source          VARCHAR(50),                        -- JobKorea/Saramin/Wanted
    posted_at       DATE,
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP
);
```

---

## 8. 인덱스 전략

| 테이블 | 인덱스 | 이유 |
|--------|--------|------|
| users | `idx_users_email` | 로그인 시 email 조회 |
| users | `idx_users_provider` | 소셜 로그인 시 provider+provider_id 조회 |
| refresh_tokens | `idx_refresh_token` | 토큰값으로 사용자 조회 |
| refresh_tokens | `idx_refresh_user` | 사용자별 토큰 목록 조회 (정리) |
| verification_codes | `idx_verification_email` | 이메일별 인증코드 확인 |
