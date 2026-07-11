# 개발 표준 정의서

## 1. 프로젝트 개요

| 항목 | 내용 |
|------|------|
| 프로젝트명 | sh-platform |
| 아키텍처 | 모놀리식 → 점진적 MSA 분리 |
| Java | 21 LTS |
| Spring Boot | 3.5.x |
| Spring Cloud Gateway | 추후 MSA 분리 시 도입 |
| Build | Gradle (Kotlin DSL) |
| DB | MariaDB 10.11 (VM2) |

---

## 2. 패키지 구조

### 2.1 단일 모듈 (Phase 2, 현재)

```
com.shplatform/
├── ShPlatformApplication.java          # @SpringBootApplication (루트 패키지)
├── auth/
│   ├── api/                            # Controller + Request/Response DTO
│   │   ├── AuthController.java
│   │   ├── SignupRequest.java
│   │   ├── LoginRequest.java
│   │   └── TokenResponse.java
│   ├── domain/                         # 순수 비즈니스 로직 (Spring 의존 없음)
│   │   ├── AuthService.java            # 인터페이스
│   │   ├── AuthServiceImpl.java
│   │   ├── User.java                   # 도메인 모델
│   │   └── Token.java
│   └── infrastructure/                 # JPA, 외부 API 어댑터
│       ├── UserEntity.java
│       ├── UserRepository.java
│       ├── UserMapper.java             # Entity ↔ Domain 변환
│       └── TokenProvider.java          # JWT 발급/검증
├── housing/
│   ├── api/
│   │   ├── HousingController.java
│   │   └── HousingResponse.java
│   ├── domain/
│   │   ├── HousingService.java
│   │   └── Article.java
│   └── infrastructure/
│       ├── ArticleEntity.java
│       ├── ArticleRepository.java
│       └── ArticleMapper.java
└── shared/                             # 공통 유틸리티
    ├── config/
    │   ├── SecurityConfig.java
    │   ├── WebConfig.java
    │   └── JpaConfig.java
    ├── exception/
    │   ├── GlobalExceptionHandler.java
    │   └── BusinessException.java
    ├── dto/
    │   └── ApiResponse.java            # 통일 응답 포맷
    └── util/
        └── MessageUtils.java           # 다국어 메시지 조회
```

### 2.2 멀티 모듈 (Phase 4, MSA 분리 시)

```
sh-platform/
├── sh-platform-core/                   # 공통 라이브러리 (API 응답, 예외, 유틸)
├── auth-service/                       # 인증 서비스 (VM3)
├── housing-service/                    # AI Housing 서비스 (VM3)
├── gateway-service/                    # API Gateway (VM1, Spring Cloud Gateway)
├── build.gradle.kts                    # 루트 빌드
└── settings.gradle.kts
```

### 2.3 레이어 규칙

```
api/ → domain/ → infrastructure/  (domain은 api/infrastructure를 절대 import 금지)
```

- `domain/`: `@Service`, 순수 Java. Spring annotation, JPA annotation 사용 금지
- `api/`: `@RestController`, `@Valid`. DTO만 노출. Entity 직접 노출 금지
- `infrastructure/`: `@Repository`, `@Entity`, JPA 구현체. Mapper로 domain 변환

---

## 3. 네이밍 컨벤션

### 3.1 Java

| 항목 | 규칙 | 예시 |
|------|------|------|
| 클래스 | PascalCase | `UserService`, `LoginRequest` |
| 메서드 | camelCase | `findByEmail()`, `createUser()` |
| 상수 | UPPER_SNAKE | `DEFAULT_PAGE_SIZE` |
| 패키지 | 전부 소문자 | `com.shplatform.auth.domain` |
| DTO | 목적 접미사 | `SignupRequest`, `TokenResponse` |
| Entity | (없음) | `UserEntity` (→ domain `User`와 구분) |
| Mapper | `Entity→Domain` 접미사 | `UserMapper` |

### 3.2 REST API

```
HTTP 동사는 URL에 넣지 않는다
  ✅ GET /users, POST /users
  ❌ GET /getUsers, POST /createUser

복수형 명사 사용
  ✅ /api/auth/users
  ❌ /api/auth/user

경로는 소문자, 단어 구분은 하이픈
  ✅ /api/auth/refresh-token
  ❌ /api/auth/refreshToken

버저닝은 Header 또는 Prefix
  ✅ Accept: application/vnd.shplatform.v1+json
  ✅ /api/v1/auth/login
```

### 3.3 DB

| 항목 | 규칙 | 예시 |
|------|------|------|
| 테이블 | snake_case, 복수형 | `users`, `refresh_tokens` |
| 컬럼 | snake_case | `email_verified`, `created_at` |
| PK | `id` (BIGINT) | `id BIGINT AUTO_INCREMENT` |
| FK | `{테이블명}_id` | `user_id` |
| 인덱스 | `idx_{테이블명}_{컬럼명}` | `idx_users_email` |
| 생성시간 | `created_at` | `DATETIME NOT NULL` |
| 수정시간 | `updated_at` | `DATETIME ON UPDATE` |

---

## 4. API 응답 포맷

### 4.1 성공

```json
{
  "code": "SUCCESS",
  "message": "요청이 처리되었습니다.",
  "data": { ... },
  "timestamp": "2026-07-11T09:00:00+09:00"
}
```

### 4.2 실패

```json
{
  "code": "INVALID_INPUT",
  "message": "이메일 형식이 올바르지 않습니다.",
  "data": null,
  "timestamp": "2026-07-11T09:00:00+09:00"
}
```

### 4.3 에러 코드 목록

| Code | HTTP Status | 설명 |
|------|:----------:|------|
| `SUCCESS` | 200 | 정상 처리 |
| `CREATED` | 201 | 생성 성공 |
| `INVALID_INPUT` | 400 | 입력값 검증 실패 |
| `UNAUTHORIZED` | 401 | 인증 실패 |
| `FORBIDDEN` | 403 | 권한 없음 |
| `NOT_FOUND` | 404 | 리소스 없음 |
| `DUPLICATE_EMAIL` | 409 | 이메일 중복 |
| `TOKEN_EXPIRED` | 401 | 토큰 만료 |
| `INTERNAL_ERROR` | 500 | 서버 내부 오류 |

---

## 5. 예외 처리

```java
// BusinessException (domain에서 던짐)
public class BusinessException extends RuntimeException {
    private final String code;
    private final Object[] args;   // 다국어 messageSource args
}

// GlobalExceptionHandler (shared/exception)
@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handle(BusinessException e) {
        return ResponseEntity
            .status(e.getHttpStatus())
            .body(ApiResponse.error(e.getCode(), e.getArgs()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handle(MethodArgumentNotValidException e) {
        // @Valid 검증 실패 → 400
    }
}
```

---

## 6. 테스트

```
src/test/java/com/shplatform/
├── auth/
│   ├── domain/
│   │   └── AuthServiceTest.java         ← JUnit 5, Mockito (Spring 미기동)
│   └── infrastructure/
│       └── UserRepositoryTest.java      ← @DataJpaTest
└── shared/
    └── config/
        └── SecurityConfigTest.java      ← @WebMvcTest
```

- 도메인 테스트: Spring context 없이 순수 JUnit
- `@SpringBootTest`는 통합 테스트에만 사용, 최소화

---

## 7. 커밋 컨벤션

```
feat:      새 기능 추가
fix:       버그 수정
docs:      문서 수정
refactor:  리팩토링 (기능 변경 없음)
test:      테스트 코드
chore:     빌드/설정 변경
style:     코드 포맷팅
```

예시:
```
feat: 회원가입 API 구현
docs: auth-api.md에 로그인 엔드포인트 추가
refactor: AuthService 분리 (AuthService → AuthService + TokenService)
```

---

## 8. 다국어 (i18n)

- Spring `MessageSource` 사용
- 메시지 파일: `messages_ko.properties`, `messages_en.properties`
- 클라이언트에서 `Accept-Language` 헤더로 언어 전달
- 상세: `docs/i18n.md` 참조

---

## 9. 환경 설정

```yaml
# application.yml
spring:
  profiles:
    active: ${SPRING_PROFILES_ACTIVE:local}
---
# application-local.yml (로컬 개발)
# application-prod.yml (WAS 배포)
```

| Profile | 용도 | DB |
|---------|------|----|
| `local` | 로컬 개발 | H2 |
| `prod` | WAS 배포 | MariaDB (VM2) |

---

## 10. 참고 자료

- [Spring Boot 공식 문서](https://docs.spring.io/spring-boot/reference/using/structuring-your-code.html)
- [Spring Boot Package Structure Best Practices (2026)](https://thelinuxcode.com/best-practices-for-structuring-a-spring-boot-application-in-2026/)
- [Hexagonal Architecture with Spring Boot](https://dev.to/paszekdev/how-i-structure-every-spring-boot-application-as-a-senior-developer-5ack)
- [MSA-Standard-Template (국내)](https://github.com/MinKyeom/MSA-Standard-Template)
