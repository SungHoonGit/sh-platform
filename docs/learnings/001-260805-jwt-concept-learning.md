# 001-260805 JWT 인증 개념 학습 기록

## 개요
- **주제**: JWT(JSON Web Token) 인증 개념
- **학습일**: 2026-08-05
- **수준**: 초급/중급

## 1. 개념 설명

### 1.1 JWT란?
JWT(JSON Web Token)는 두 애플리케이션 간에 안전하게 정보를 주고받기 위한 **표준 토큰 포맷**입니다.

**한마디로: 서버가 발급한 "디지털 신분증"**

### 1.2 왜 필요한가?
- HTTP는 **상태lessness** (요청마다 독립적) → "이전에 로그인했는지" 알 수 없음
- 매번 이메일/비밀번호를 보낼 수 없음 (보안 문제)
- **JWT = 로그인 상태를 증명하는 토큰**

### 1.3 관련 개념

| 개념 | 설명 |
|------|------|
| Access Token | API 접근용 토큰 (예: 1시간 만료) |
| Refresh Token | Access Token 만료 시 새 토큰 발급용 (예: 14일) |
| Bearer Token | "이 토큰 소유자를 인증했다"는 의미 |
| RS256 | 비대칭키 암호화 (공개키/개인키) |
| HS256 | 대칭키 암호화 (하나의 비밀키) |

## 2. JWT 구조

### 2.1 토큰 형식
```
eyJhbGciOiJSUzI1NiJ9.eyJzdWIiOiI0Iiwicm9sZSI6IkFETUlOIiwiZXhwIjoxNzg1OTA1MjcwLCJpYXQiOjE3ODU5MDE2NzAsImVtYWlsIjoiYWRtaW5AYWRtaW4uY29tIn0.서명
```

### 2.2 3개 파트 (마침표로 구분)

| 파트 | 내용 | 예시 |
|------|------|------|
| **Header** | 알고리즘, 토큰 타입 | `{"alg":"RS256","typ":"JWT"}` |
| **Payload** | 사용자 정보, 만료시간 | `{"sub":"4","email":"admin@admin.com","exp":1785905270}` |
| **Signature** | 위변조 검증용 서명 | `RSA-SHA256(Header + Payload, 개인키)` |

### 2.3 Payload 주요 필드

| 필드 | 설명 |
|------|------|
| `sub` | 사용자 ID (subject) |
| `email` | 이메일 |
| `role` | 권한 (USER, ADMIN 등) |
| `iat` | 발급 시간 (issued at) |
| `exp` | 만료 시간 (expiration) |

## 3. 인증 흐름

### 3.1 로그인 시

```
┌──────────┐    ┌──────────┐    ┌──────────┐
│ 프론트엔드 │    │ 백엔드    │    │ DB       │
│ (React)  │    │ (Spring) │    │(MariaDB) │
└────┬─────┘    └────┬─────┘    └────┬─────┘
     │               │               │
     │ 1. 로그인 요청  │               │
     │ email, password│               │
     ├──────────────>│               │
     │               │ 2. 비밀번호 검증 │
     │               │──────────────>│
     │               │ 3. 사용자 정보  │
     │               │<──────────────│
     │               │               │
     │               │ 4. JWT 토큰 발급│
     │ 5. 토큰 반환   │               │
     │<──────────────│               │
     │               │               │
     │ 6. 토큰 저장   │               │
     │ localStorage  │               │
```

### 3.2 API 호출 시

```
┌──────────┐    ┌──────────┐
│ 프론트엔드 │    │ 백엔드    │
│ (React)  │    │ (Spring) │
└────┬─────┘    └────┬─────┘
     │               │
     │ 1. API 요청    │
     │ Authorization: │
     │ Bearer eyJ... │
     ├──────────────>│
     │               │ 2. JWT 검증
     │               │ (서명 확인)
     │               │
     │               │ 3. 인증 성공
     │               │ → 요청 처리
     │ 4. 응답 반환   │
     │<──────────────│
```

### 3.3 토큰 만료 시

```
┌──────────┐    ┌──────────┐
│ 프론트엔드 │    │ 백엔드    │
└────┬─────┘    └────┬─────┘
     │               │
     │ 1. API 요청    │
     ├──────────────>│
     │               │ 2. JWT 검증
     │               │ → 만료됨!
     │ 3. 401 응답    │
     │<──────────────│
     │               │
     │ 4. 로그인 페이지│
     │    이동       │
```

## 4. 프론트엔드 구현

### 4.1 토큰 저장
```typescript
// 로그인 성공 시
localStorage.setItem('accessToken', response.data.accessToken);
localStorage.setItem('refreshToken', response.data.refreshToken);
```

### 4.2 API 호출 시 토큰 전달
```typescript
async function request<T>(path: string): Promise<T> {
  const token = localStorage.getItem('accessToken');
  
  const res = await fetch(`/api${path}`, {
    headers: {
      'Authorization': `Bearer ${token}`,  // ← 토큰 포함
      'Content-Type': 'application/json',
    },
  });
  
  // 401 에러 시 로그인 페이지로
  if (res.status === 401) {
    window.location.href = '/login';
  }
  
  return res.json();
}
```

### 4.3 자동 갱신 (선택사항)
```typescript
// 토큰 만료 임박 시 자동 갱신
const refreshToken = localStorage.getItem('refreshToken');
const response = await fetch('/api/auth/refresh', {
  method: 'POST',
  body: JSON.stringify({ refreshToken }),
});
```

## 5. 백엔드 구현 (Spring Security)

### 5.1 JWT 발급
```java
// 로그인 성공 시
String accessToken = JwtTokenValidator.generateAccessToken(
    user.getId(), user.getEmail(), user.getRole()
);
```

### 5.2 JWT 검증 필터
```java
// JwtAuthenticationFilter.java
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    @Override
    protected void doFilterInternal(HttpServletRequest request, ...) {
        String token = request.getHeader("Authorization");
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
            JwtClaims claims = jwtTokenValidator.validate(token);
            // SecurityContext에 인증 정보 저장
        }
    }
}
```

### 5.3 Security 설정
```java
// SecurityConfig.java
.authorizeHttpRequests(auth -> auth
    .requestMatchers("/api/public/**").permitAll()  // 인증 불필요
    .anyRequest().authenticated()  // 그 외에는 인증 필요
)
```

## 6. 보안 고려사항

| 항목 | 설명 |
|------|------|
| HTTPS | JWT는 평문 전송 → 반드시 HTTPS 사용 |
| 만료 시간 | Access Token은 짧게 (1시간 권장) |
| Refresh Token | DB 저장 + Rotation (기존 토큰 폐기) |
| 서명 검증 | RS256 (비대칭키) 권장 |
| XSS | localStorage는 XSS에 취약 → HttpOnly 쿠키 고려 |

## 7. 에러 처리

| 상황 | HTTP 상태 | 처리 |
|------|-----------|------|
| 토큰 없음 | 401 | 로그인 페이지로 이동 |
| 토큰 만료 | 401 | Refresh Token으로 갱신 시도 |
| 토큰 위변조 | 401 | 로그인 페이지로 이동 |
| 권한 부족 | 403 | 접근 불가 페이지 표시 |

## 8. 실전 적용

### 8.1 이 프로젝트에서의 적용
- auth 서비스 (port 8080)에서 JWT 발급
- scraper, resume, portfolio 서비스에서 JWT 검증
- 프론트엔드에서 `Authorization: Bearer {token}` 헤더 전달

### 8.2 관련 코드
- 필터: `common/src/main/java/com/shplatform/common/security/JwtAuthenticationFilter.java`
- 검증: `common/src/main/java/com/shplatform/common/security/JwtTokenValidator.java`
- 보안 설정: `modules/scraper/backend/src/main/java/com/scraper/platform/config/SecurityConfig.java`

## 9. 참고 자료
- [JWT 공식 사이트](https://jwt.io/)
- [Spring Security 공식 문서](https://docs.spring.io/spring-security/reference/servlet/authentication/)
- [ OWASP JWT 가이드](https://cheatsheetseries.owasp.org/cheatsheets/JSON_Web_Token_for_Java_Cheat_Sheet.html)

---
*작성일: 2026-08-05*