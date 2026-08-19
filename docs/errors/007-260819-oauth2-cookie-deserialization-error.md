# 007-260819-oauth2-cookie-deserialization-error

## 개요
- **발생일**: 2026-08-19
- **환경**: Linux(OCI), Java 21, Spring Boot 3.4.4, Spring Security 6.4.4
- **심각도**: 🟡 Warning (간편로그인이 확률적으로 실패)

## 1. 오류 현상

### 1.1 에러 메시지

```
[OAUTH2] failed to load authorization request from cookie: The class with java.util.Collections$UnmodifiableSet and name of java.util.Collections$UnmodifiableSet is not in the allowlist. If you believe this class is safe to deserialize, please provide an explicit mapping using Jackson annotations or by providing a Mixin. If the serialization is only done by a trusted source, you can also enable default typing. See https://github.com/spring-projects/spring-security/issues/4370 for details
[OAUTH2] authentication failed: [authorization_request_not_found]
```

### 1.2 재현 단계

1. 플랫폼에서 카카오/네이버/구글/GitHub 간편로그인 클릭
2. 브라우저 → Provider 승인 페이지 → 다시 `/login/oauth2/code/{provider}`로 리다이렉트
3. **가끔** 실패 → `/auth/error?message=...`로 이동
4. **2~3번 다시 시도하면 성공** (확률적 실패 — 실패와 성공이 섞임)

## 2. 원인 분석

### 2.1 근본 원인

**직렬화는 성공, 역직렬화는 실패**하는 문제.

- `OAuth2AuthorizationRequest`(인가 요청)를 쿠키에 JSON으로 저장할 때, 내부 `scopes`(Set) 필드가 `java.util.Collections$UnmodifiableSet` 타입으로 직렬화됨.
- Spring Security 6.4의 `OAuth2ClientJackson2Module`은 `SecurityJackson2Modules.enableDefaultTyping()`으로 **기본 타입 직렬화(DefaultTyping.NON_FINAL)** 를 활성화하고, 역직렬화 시 허용 목록(allowlist)을 검사함.
- **allowlist에 `java.util.Collections$UnmodifiableSet`이 없어서** 쿠키 복원(역직렬화)이 실패 → `loadAuthorizationRequest()`가 null 반환 → Spring Security가 `authorization_request_not_found` 예외 발생 → 로그인 실패.
- **왜 확률적 실패였나**: 요청마다 쿠키에 직렬화되는 값 구조가 조금씩 달라져(scope 순서, 추가 파라미터 유무 등) `UnmodifiableSet`이 아닌 형태로 직렬화되는 경우는 통과, 그렇지 않은 경우는 실패 → "두세번 하면 되는" 증상이 됨.

> 참고: `java.util.ImmutableCollections`(`Set.of`)는 allowlist에 있지만 `java.util.Collections$UnmodifiableSet`(`Collections.unmodifiableSet`)은 없음. scopes가 어느 타입으로 만들어지느냐에 따라 성공/실패가 갈림.

### 2.2 관련 코드

- 파일: `modules/auth/backend/src/main/java/com/shplatform/shared/config/CookieAuthorizationRequestRepository.java`
- 코드:
```java
private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
        .registerModule(new OAuth2ClientJackson2Module());   // ⚠ 기본 allowlist 사용 → UnmodifiableSet 차단
```

## 3. 해결 방법

### 3.1 해결 과정

Spring Security가 쓰는 방식과 동일하게 ObjectMapper의 **default typing을 명시적으로 재설정**하고, allowlist에 `java.util.*`을 추가했다.

- `ObjectMapper.DefaultTyping.EVERYTHING` → **`NON_FINAL`** 로 변경 (Spring Security 기본값과 동일, `readTree()`의 JsonNode 파싱도 정상 동작)
- `BasicPolymorphicTypeValidator`에 `org.springframework.security.` + `java.util.` 하위 타입 허용 추가

### 3.2 최종 코드 변경

```java
// 변경 전
private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
        .registerModule(new OAuth2ClientJackson2Module());

// 변경 후
private static final ObjectMapper OBJECT_MAPPER = createObjectMapper();

private static ObjectMapper createObjectMapper() {
    ObjectMapper objectMapper = new ObjectMapper();
    objectMapper.registerModule(new OAuth2ClientJackson2Module());
    objectMapper.activateDefaultTyping(
            BasicPolymorphicTypeValidator.builder()
                    .allowIfSubType("org.springframework.security.")
                    .allowIfSubType("java.util.")
                    .build(),
            ObjectMapper.DefaultTyping.NON_FINAL,
            JsonTypeInfo.As.PROPERTY);
    return objectMapper;
}
```

### 3.3 검증

- 회귀 테스트 추가: `modules/auth/backend/src/test/java/com/shplatform/shared/config/CookieAuthorizationRequestRepositoryTest.java`
  - `UnmodifiableSet scopes도 역직렬화된다 (allowlist 함정 회귀 테스트)` 포함 4개
- 수정 전 테스트 실패 → 수정 후 전체 통과 확인
- 커밋: `34b67e0` (서버 배포 완료, 로그인 정상 확인)

## 4. 예방 방법

- **Spring Security OAuth2 직렬화 오류가 나면**: 먼저 allowlist 확인. `Collections$UnmodifiableSet` 등 `java.util.*` 컬렉션이면 위 방식으로 validator에 허용 추가
- `authorization_request_not_found`가 나오면 쿠키 역직렬화 로그(`failed to load authorization request from cookie`)를 먼저 확인
- 확률적 실패("가끔 되는데 자꾸 실패") = **직렬화 포맷이 요청마다 달라지는 근본 원인**을 의심 (일관적 실패가 아님)
- 쿠키에 전체 OAuth2AuthorizationRequest를 저장하는 방식은 유지하되, 역직렬화 호환성을 테스트로 보호

## 5. 참고 자료

- [spring-security GitHub issue #4370](https://github.com/spring-projects/spring-security/issues/4370) — allowlist 관련 원본 이슈
- Spring Security `SecurityJackson2Modules.AllowlistTypeIdResolver` — allowlist 정의 (java.util.ArrayList, Collections$EmptyList, UnmodifiableRandomAccessList 등)

---
*작성일: 2026-08-19*