# SH Pass — SQL DDL

**데이터베이스:** `sh_pass`
**생성일:** 2026-07-11
**방식:** Hibernate `ddl-auto: update` (Spring Boot 기동 시 자동 생성)
**DB 서버:** 161.33.138.23 (내부: 10.0.0.39)

---

## 테이블 목록

| 테이블 | 설명 | 엔티티 클래스 |
|--------|------|-------------|
| `users` | 회원 정보 | UserEntity.java |
| `refresh_tokens` | 리프레시 토큰 | RefreshTokenEntity.java |
| `verification_codes` | 이메일 인증 코드 | VerificationCodeEntity.java |

---

## 1. users

```sql
CREATE TABLE `users` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) NOT NULL,
  `email` varchar(100) NOT NULL,
  `email_verified` bit(1) NOT NULL,
  `locale` varchar(10) NOT NULL,
  `name` varchar(50) NOT NULL,
  `password` varchar(200) DEFAULT NULL,
  `provider` varchar(20) NOT NULL,
  `provider_id` varchar(100) DEFAULT NULL,
  `role` enum('ADMIN','USER') NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK6dotkott2kjsp8vw4d0m25fb7` (`email`),
  KEY `idx_users_email` (`email`),
  KEY `idx_users_provider` (`provider`,`provider_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

**컬럼 상세:**

| 컬럼 | 타입 | Null | 설명 |
|------|------|:----:|------|
| id | BIGINT | NO | PK, 자동증가 |
| email | VARCHAR(100) | NO | 이메일 (고유제약) |
| password | VARCHAR(200) | YES | BCrypt 해시, 소셜 로그인 시 NULL |
| name | VARCHAR(50) | NO | 이름 |
| role | ENUM('ADMIN','USER') | NO | 권한 |
| provider | VARCHAR(20) | NO | 가입 경로 (LOCAL/KAKAO/NAVER/GOOGLE/GITHUB) |
| provider_id | VARCHAR(100) | YES | 소셜 프로바이더 ID |
| email_verified | BIT | NO | 이메일 인증 여부 |
| locale | VARCHAR(10) | NO | 선호 언어 (ko/en) |
| created_at | DATETIME(6) | NO | 생성일시 |
| updated_at | DATETIME(6) | NO | 수정일시 |

---

## 2. refresh_tokens

```sql
CREATE TABLE `refresh_tokens` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) NOT NULL,
  `expires_at` datetime(6) NOT NULL,
  `token` varchar(500) NOT NULL,
  `user_id` bigint(20) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKghpmfn23vmxfu3spu3lfg4r2d` (`token`),
  KEY `idx_refresh_token` (`token`),
  KEY `idx_refresh_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

**컬럼 상세:**

| 컬럼 | 타입 | Null | 설명 |
|------|------|:----:|------|
| id | BIGINT | NO | PK, 자동증가 |
| user_id | BIGINT | NO | users.id 참조 (Application 레벨) |
| token | VARCHAR(500) | NO | 리프레시 토큰 (UUID 형식) |
| expires_at | DATETIME(6) | NO | 만료일시 |
| created_at | DATETIME(6) | NO | 생성일시 |

---

## 3. verification_codes

```sql
CREATE TABLE `verification_codes` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `code` varchar(6) NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `email` varchar(100) NOT NULL,
  `expires_at` datetime(6) NOT NULL,
  `purpose` varchar(30) NOT NULL,
  `verified` bit(1) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_verification_email` (`email`,`purpose`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

**컬럼 상세:**

| 컬럼 | 타입 | Null | 설명 |
|------|------|:----:|------|
| id | BIGINT | NO | PK, 자동증가 |
| email | VARCHAR(100) | NO | 인증 대상 이메일 |
| code | VARCHAR(6) | NO | 6자리 인증 코드 |
| purpose | VARCHAR(30) | NO | 용도 (SIGNUP / RESET_PASSWORD) |
| expires_at | DATETIME(6) | NO | 만료일시 (생성 후 10분) |
| verified | BIT | NO | 인증 완료 여부 |
| created_at | DATETIME(6) | NO | 생성일시 |

---

## 참고사항

- 모든 테이블 `ENGINE=InnoDB`, `CHARSET=utf8mb4`
- FK 제약조건 없음 (Application 레벨에서 관리)
- `ddl-auto: update` → 테이블/컬럼 자동 생성, 기존 테이블은 변경 안 함
- 변경 이력은 별도 문서에서 관리
