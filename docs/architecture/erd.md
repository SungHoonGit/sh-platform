---
title: ERD
description: ERD - architecture module documentation
category: architecture
created: 2026-07-13
updated: 2026-07-21
---

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

---

## 9. SaaS 테넌트 관리 테이블 (Phase 5)

### 9.1 sh_tenant — 테넌트

```sql
CREATE TABLE sh_tenant (
    id              BIGINT          AUTO_INCREMENT PRIMARY KEY,
    name            VARCHAR(100)    NOT NULL,
    slug            VARCHAR(50)     NOT NULL UNIQUE,
    domain          VARCHAR(100),
    logo_url        VARCHAR(500),
    status          ENUM(ACTIVE,SUSPENDED,DELETED) DEFAULT ACTIVE,
    plan_type       ENUM(FREE,BASIC,PRO,ENTERPRISE) DEFAULT FREE,
    max_users       INT             DEFAULT 5,
    settings        JSON,
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_sh_tenant_slug (slug),
    INDEX idx_sh_tenant_status (status)
);
```

| 컬럼 | 설명 |
|------|------|
| `slug` | URL용 식별자 (예: my-company) |
| `status` | 테넌트 상태 (ACTIVE/SUSPENDED/DELETED) |
| `plan_type` | 구독 플랜 (FREE/BASIC/PRO/ENTERPRISE) |
| `max_users` | 최대 사용자 수 |
| `settings` | 테넌트별 커스텀 설정 (JSON) |

### 9.2 sh_tenant_member — 테넌트 멤버

```sql
CREATE TABLE sh_tenant_member (
    id              BIGINT          AUTO_INCREMENT PRIMARY KEY,
    tenant_id       BIGINT          NOT NULL,
    user_id         BIGINT          NOT NULL,
    role            ENUM(OWNER,ADMIN,MEMBER,GUEST) DEFAULT MEMBER,
    status          ENUM(ACTIVE,INVITED,SUSPENDED) DEFAULT INVITED,
    invited_at      DATETIME,
    joined_at       DATETIME,
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (tenant_id) REFERENCES sh_tenant(id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    UNIQUE KEY uk_tenant_user (tenant_id, user_id),
    INDEX idx_sh_tenant_member_user (user_id),
    INDEX idx_sh_tenant_member_tenant (tenant_id)
);
```

| 컬럼 | 설명 |
|------|------|
| `role` | 멤버 역할 (OWNER/ADMIN/MEMBER/GUEST) |
| `status` | 멤버 상태 (ACTIVE/INVITED/SUSPENDED) |
| `invited_at` | 초대 일시 |
| `joined_at` | 가입 일시 |

### 9.3 sh_tenant_invitation — 테넌트 초대장

```sql
CREATE TABLE sh_tenant_invitation (
    id              BIGINT          AUTO_INCREMENT PRIMARY KEY,
    tenant_id       BIGINT          NOT NULL,
    email           VARCHAR(200)    NOT NULL,
    role            ENUM(ADMIN,MEMBER,GUEST) DEFAULT MEMBER,
    token           VARCHAR(100)    NOT NULL UNIQUE,
    expires_at      DATETIME        NOT NULL,
    accepted_at     DATETIME,
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (tenant_id) REFERENCES sh_tenant(id) ON DELETE CASCADE,
    INDEX idx_sh_tenant_invitation_token (token),
    INDEX idx_sh_tenant_invitation_email (email)
);
```

| 컬럼 | 설명 |
|------|------|
| `token` | 초대 토큰 (고유값) |
| `expires_at` | 초대 만료 일시 |
| `accepted_at` | 초대 수락 일시 |

### 9.4 sh_tenant_audit_log — 테넌트 감사 로그

```sql
CREATE TABLE sh_tenant_audit_log (
    id              BIGINT          AUTO_INCREMENT PRIMARY KEY,
    tenant_id       BIGINT          NOT NULL,
    user_id         BIGINT,
    action          VARCHAR(50)     NOT NULL,
    target_type     VARCHAR(50),
    target_id       BIGINT,
    details         JSON,
    ip_address      VARCHAR(45),
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (tenant_id) REFERENCES sh_tenant(id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL,
    INDEX idx_sh_tenant_audit_tenant (tenant_id),
    INDEX idx_sh_tenant_audit_user (user_id),
    INDEX idx_sh_tenant_audit_created (created_at)
);
```

| 컬럼 | 설명 |
|------|------|
| `action` | 수행한 작업 (예: CREATE_TENANT, INVITE_MEMBER) |
| `target_type` | 작업 대상 유형 (예: TENANT, MEMBER) |
| `target_id` | 작업 대상 ID |
| `details` | 작업 상세 정보 (JSON) |
| `ip_address` | 작업 수행 IP |

### 9.5 ERD 다이어그램 (테넌트 관리)

```
┌────────────────────────────────────────────────────────────────────┐
│                          sh_tenant                                 │
├────────────────────────────────────────────────────────────────────┤
│ PK │ id                BIGINT                                      │
│    │ name              VARCHAR(100)  NN                            │
│ UQ │ slug              VARCHAR(50)   NN                            │
│    │ domain            VARCHAR(100)                                │
│    │ logo_url          VARCHAR(500)                                │
│    │ status            ENUM          DEFAULT ACTIVE                 │
│    │ plan_type         ENUM          DEFAULT FREE                  │
│    │ max_users         INT           DEFAULT 5                     │
│    │ settings          JSON                                       │
│    │ created_at        DATETIME      NN                            │
│    │ updated_at        DATETIME      NN (ON UPDATE)                │
└────────────────────┬───────────────────────────────────────────────┘
                     │ 1
                     │
                     │ N
┌────────────────────┴───────────────────────────────────────────────┐
│                      sh_tenant_member                               │
├────────────────────────────────────────────────────────────────────┤
│ PK │ id                BIGINT                                      │
│ FK │ tenant_id         BIGINT        NN                            │
│ FK │ user_id           BIGINT        NN                            │
│    │ role              ENUM          DEFAULT MEMBER                 │
│    │ status            ENUM          DEFAULT INVITED                │
│    │ invited_at        DATETIME      NULL                          │
│    │ joined_at         DATETIME      NULL                          │
│    │ created_at        DATETIME      NN                            │
│ UQ │ uk_tenant_user    (tenant_id, user_id)                        │
└────────────────────────────────────────────────────────────────────┘

┌────────────────────────────────────────────────────────────────────┐
│                    sh_tenant_invitation                             │
├────────────────────────────────────────────────────────────────────┤
│ PK │ id                BIGINT                                      │
│ FK │ tenant_id         BIGINT        NN                            │
│    │ email             VARCHAR(200)  NN                            │
│    │ role              ENUM          DEFAULT MEMBER                 │
│ UQ │ token             VARCHAR(100)  NN                            │
│    │ expires_at        DATETIME      NN                            │
│    │ accepted_at       DATETIME      NULL                          │
│    │ created_at        DATETIME      NN                            │
└────────────────────────────────────────────────────────────────────┘

┌────────────────────────────────────────────────────────────────────┐
│                    sh_tenant_audit_log                              │
├────────────────────────────────────────────────────────────────────┤
│ PK │ id                BIGINT                                      │
│ FK │ tenant_id         BIGINT        NN                            │
│ FK │ user_id           BIGINT                                      │
│    │ action            VARCHAR(50)   NN                            │
│    │ target_type       VARCHAR(50)                                 │
│    │ target_id         BIGINT                                      │
│    │ details           JSON                                       │
│    │ ip_address        VARCHAR(45)                                 │
│    │ created_at        DATETIME      NN                            │
└────────────────────────────────────────────────────────────────────┘
```
