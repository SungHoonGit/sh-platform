# Swagger (SpringDoc) API 문서 가이드

## 1. 개념

Swagger는 코드에 붙인 어노테이션을 읽어서 **브라우저에서 API를 테스트할 수 있는 UI**를 자동 생성한다.

```
Controller에 @Tag, @Operation 붙임
  → 실행 시 /v3/api-docs/ 에 JSON 문서 자동 생성
  → /swagger-ui/index.html 에서 GUI로 조회 + 테스트
```

Javadoc과의 차이:

| 항목 | Javadoc | Swagger |
|------|---------|---------|
| 출력 | HTML (정적) | 웹 UI (동적) |
| API 테스트 | 불가능 | "Try it out" 버튼으로 직접 호출 가능 |
| 정보 출처 | `/** */` 주석 | `@Operation`, `@Parameter` 어노테이션 |

---

## 2. 적용 방법

### 2.1 의존성

`sh-platform-auth/build.gradle.kts`:

```kotlin
implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.8.6")
```

딱 한 줄. Spring Boot가 자동으로 `@RestController`를 스캔해서 문서를 만든다.

### 2.2 Security 예외 경로

Swagger UI가 인증 안 받고 접근할 수 있도록 `SecurityConfig.java`에 추가:

```java
.requestMatchers(
    "/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**"
).permitAll()
```

### 2.3 Config 클래스 (선택)

```java
@Bean
public OpenAPI openAPI() {
    return new OpenAPI()
            .info(new Info()
                    .title("sh-platform API")
                    .description("설명")
                    .version("1.0.0"))
            .servers(List.of(
                    new Server().url(serverUrl).description("현재 서버")
            ));
}
```

---

## 3. 접속 주소

| 환경 | URL |
|------|-----|
| 운영 (nginx) | https://sunghoonyk.duckdns.org/swagger-ui/ |
| 로컬 | http://localhost:8080/swagger-ui/ |

---

## 4. 어노테이션 가이드

```java
@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Auth", description = "인증 API")     // 컨트롤러 그룹명
public class AuthController {

    @PostMapping("/signup")
    @Operation(summary = "회원가입", description = "이메일 인증 후 회원 생성")
    @ApiResponse(responseCode = "201", description = "회원가입 성공")
    public ResponseEntity<ApiResponse<Map<String, Object>>> signup(
            @Valid @RequestBody SignupRequest request
    ) { ... }
}
```

- `@Tag` — 컨트롤러 단위 그룹명
- `@Operation` — API 요약 + 설명
- `@ApiResponse` — 응답 코드별 설명
- `@Parameter` — 파라미터 설명 (생략해도 DTO 필드는 자동 표시)

---

## 5. nginx 설정

```nginx
location /swagger-ui/ {
    proxy_pass http://127.0.0.1:8080/swagger-ui/;
    proxy_set_header Host $host;
    proxy_set_header X-Real-IP $remote_addr;
}

location /v3/ {
    proxy_pass http://127.0.0.1:8080/v3/;
    proxy_set_header Host $host;
    proxy_set_header X-Real-IP $remote_addr;
}
```

---

## 6. 자주 묻는 것

**Q: 컨트롤러마다 어노테이션 다 달아야 함?**
필수는 아님. 의존성만 추가해도 `@RestController` 기준으로 문서 자동 생성됨. 어노테이션은 설명을 보강할 때만.

**Q: Swagger UI에 민감 정보 노출되나?**
현재 설정은 `permitAll`이라 누구나 접근 가능. 운영에서 막으려면 `.requestMatchers(...).hasRole("ADMIN")`으로 변경.
