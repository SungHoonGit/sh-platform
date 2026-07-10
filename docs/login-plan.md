# 로그인/인증 기획

## 방식

| 항목 | 결정 |
|------|------|
| 일반 로그인 | ✅ (이메일 + BCrypt) |
| 소셜 로그인 | ✅ 카카오, 네이버, 구글, 깃헙 |
| 이메일 인증 | ✅ (Gmail SMTP, 기존 인프라 활용) |
| 휴대폰 인증 | ❌ (추후 검토, 비용 문제) |
| JWT | ✅ RS256 (비대칭키) |
| Refresh Token | ✅ MariaDB 저장 (Redis는 추후) |

## ERD (초안)

### users

| 컬럼 | 타입 | 설명 |
|------|------|------|
| id | BIGINT PK | 자동 증가 |
| email | VARCHAR(100) UNIQUE | 로그인 ID |
| password | VARCHAR(200) | BCrypt 암호화 (소셜은 null) |
| name | VARCHAR(50) | |
| role | ENUM(USER, ADMIN) | |
| provider | VARCHAR(20) | LOCAL / KAKAO / NAVER / GOOGLE / GITHUB |
| provider_id | VARCHAR(100) | 소셜 플랫폼 고유 ID |
| email_verified | BOOLEAN | 이메일 인증 여부 |
| created_at | DATETIME | |
| updated_at | DATETIME | |

### refresh_tokens

| 컬럼 | 타입 | 설명 |
|------|------|------|
| id | BIGINT PK | |
| user_id | BIGINT FK | users.id |
| token | VARCHAR(500) UNIQUE | Refresh Token |
| expires_at | DATETIME | 만료 시간 |

## API 엔드포인트 (초안)

| Method | Path | 설명 |
|--------|------|------|
| POST | /api/auth/signup | 회원가입 |
| POST | /api/auth/verify-email | 이메일 인증코드 발송 |
| POST | /api/auth/verify-code | 인증코드 확인 |
| POST | /api/auth/login | 로그인 (JWT 발급) |
| POST | /api/auth/refresh | 토큰 갱신 |
| GET | /api/auth/oauth2/{provider} | 소셜 로그인 리다이렉트 |
| POST | /api/auth/logout | 로그아웃 (토큰 폐기) |

## 기술 스택

| 항목 | 버전 |
|------|------|
| Java | 21 LTS |
| Spring Boot | 3.5.x |
| Spring Security | 6.x (JWT + OAuth2 Client) |
| MariaDB | 10.11 (VM2) |
| ORM | Spring Data JPA |
| Build | Gradle |
