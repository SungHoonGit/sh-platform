# Auth Backend

인증 서비스 (port 8080)

## 개요

- 로그인/회원가입 (이메일 + 비밀번호)
- 소셜 로그인 (카카오, 네이버, 구글, 깃험)
- JWT 토큰 발급/검증
- 관리자 API

## 기술 스택

- Java 21 LTS
- Spring Boot 3.4.4
- Spring Security + OAuth2
- JWT (RS256)
- MariaDB

## 구조

```
backend/src/main/java/com/shplatform/auth/
├── api/                    # API DTO
├── config/                 # 설정 (Security, OAuth2)
├── controller/             # REST 컨트롤러
│   ├── AuthController.java      # 로그인/회원가입
│   ├── SocialController.java    # 소셜 로그인
│   └── AdminController.java     # 관리자 API
├── domain/                 # 비즈니스 로직
├── infrastructure/         # Repository, Entity
└── AuthApplication.java
```

## API 엔드포인트

| 메서드 | 경로 | 설명 |
|--------|------|------|
| POST | `/api/v1/auth/signup` | 회원가입 |
| POST | `/api/v1/auth/login` | 로그인 |
| POST | `/api/v1/auth/refresh` | 토큰 갱신 |
| GET | `/api/v1/auth/me` | 내 정보 |
| GET | `/api/v1/oauth/{provider}` | 소셜 로그인 URL |
| GET | `/api/v1/oauth/{provider}/callback` | 소셜 콜백 |
| GET | `/api/v1/admin/users` | 사용자 목록 (관리자) |

## 환경 변수 (.env)

```bash
# DB
SPRING_DATASOURCE_URL=jdbc:mariadb://10.0.0.39:3306/auth_platform
SPRING_DATASOURCE_USERNAME=sh_user
SPRING_DATASOURCE_PASSWORD=SHpass1234!

# JWT
JWT_PRIVATE_KEY=...
JWT_PUBLIC_KEY=...

# OAuth2
OAUTH_KAKAO_CLIENT_ID=...
OAUTH_NAVER_CLIENT_ID=...
OAUTH_GOOGLE_CLIENT_ID=...
```

## 로컬 실행

```bash
./gradlew :modules:auth:backend:build
java -jar modules/auth/backend/build/libs/sh-platform-auth-*.jar
```

## 배포

```bash
sudo systemctl restart sh-platform-auth
sudo journalctl -u sh-platform-auth --since "5 min ago" -f
```
