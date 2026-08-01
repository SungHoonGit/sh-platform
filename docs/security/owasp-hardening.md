# OWASP 웹 취약점 대응 (Web Security Hardening)

> 상태: 검토 문서 (초안)
> 기준: OWASP Top 10 (2021) — A05 Security Misconfiguration 중심
> 대상: nginx + Spring Boot(4개 모듈) + React SPA(3개 앱)
> 관련 문서: [에러 페이지 전략](./error-pages.md), [보안 아키텍처](../architecture/architecture.md)

---

## 1. 목표

웹 서비스에서 흔히 지적되는 **정보 노출(Information Disclosure)** 를 차단한다:

1. 서버·프레임워크 **버전 노출** 제거 (nginx `Server` 헤더, Spring/Tomcat)
2. 에러 응답에서 **스택 트레이스 / 내부 정보 미노출**
3. **보안 헤더** 적용 (X-Frame-Options, CSP, HSTS 등)
4. **운영 로그·상세 설정 노출** 정리 (SQL 로그, DEBUG 레벨, health 상세)

---

## 2. 현재 상태 (검토 결과)

### 2.1 버전 노출

| 항목 | 현재 상태 | 위험 |
|------|-----------|:---:|
| nginx `Server` 헤더 | `server_tokens` 설정 없음 → 버전 노출 가능 | 중 |
| Spring/Tomcat `Server` 헤더 | `server.server-header` 미설정 | 중 |
| Spring Whitelabel 에러 페이지 | 기본 활성 (`server.error.whitelabel.enabled` 미설정) | 중 |
| Actuator | auth: `health,info,prometheus,metrics` / scraper: `health,info,prometheus` 노출 | 낮~중 |
| Swagger | `/swagger-ui/`, `/v3/api-docs` 공개 (운영에서도) | 낮 |

### 2.2 보안 헤더

| 헤더 | auth (Spring Security O) | scraper/resume/portfolio (Security X) | nginx |
|------|:---:|:---:|:---:|
| `X-Content-Type-Options: nosniff` | Spring Security 기본 | ❌ | ❌ |
| `X-Frame-Options: DENY` | Spring Security 기본 | ❌ | ❌ |
| `Content-Security-Policy` | ❌ | ❌ | ❌ |
| `Strict-Transport-Security` (HSTS) | ❌ | ❌ | ❌ |
| `Referrer-Policy` | ❌ | ❌ | ❌ |
| `Permissions-Policy` | ❌ | ❌ | ❌ |

> scraper/resume/portfolio는 Spring Security 미사용 → 헤더가 **전혀 없음**.
> 통일된 정책은 nginx 한 곳에서 적용하는 것이 유지보수에 유리.

### 2.3 로그·상세 설정 노출

| 항목 | auth | scraper | 위험 |
|------|:---:|:---:|:---:|
| `spring.jpa.properties.hibernate.show_sql` | false | **true** | 상 |
| `logging.level.<모듈>` | INFO | **DEBUG** | 중 |
| `management.endpoint.health.show-details` | when-authorized | **always** | 중 |

> scraper `application.yml` (운영 포함): `show-sql: true` + `com.scraper.platform: DEBUG` +
> `health.show-details: always` → SQL·상세 DB 정보가 응답/로그에 노출될 수 있음.

### 2.4 에러 응답

- `GlobalExceptionHandler`가 `ApiResponse.error(code, message)`로 통일, 스택 트레이스 미노출. ✅
- 미처리 케이스(404/405/타입 불일치)는 [에러 페이지 전략](./error-pages.md)에서 다룸.

### 2.5 기타

- nginx 설정 파일이 **서버에만 존재**하고 저장소에 없음 → 변경 이력·배포 자동화 불가.
- `/` 는 nginx가 정적 파일로 서빙 (auth 프론트) — nginx 캐시 헤더 설정 여부 확인 필요.

---

## 3. 대안 검토

### 3.1 버전 숨김

| 대안 | 내용 | 판단 |
|------|------|:---:|
| **A. nginx `server_tokens off;`** | `Server: nginx` (버전 생략)로 변경 | ⭐ 권고 |
| B. nginx `server_tokens off` + Spring `server.server-header:`(빈값) | 양쪽 모두 숨김 | ⭐ 권고 (함께) |
| C. 커스텀 오류 페이지에서 버전 출력 | 반대 방향(권장 안 함) | ❌ |

### 3.2 Whitelabel 에러 페이지

| 대안 | 내용 | 판단 |
|------|------|:---:|
| **A. `server.error.whitelabel.enabled: false`** | API 오류는 GlobalExceptionHandler가 처리, 나머지는 404/500 JSON | ⭐ 권고 |
| B. Whitelabel 유지 | 버전·스타일 노출 위험 | ❌ |

### 3.3 보안 헤더 적용 위치

| 대안 | 내용 | 판단 |
|------|------|:---:|
| **A. nginx `add_header` 일괄 적용** | 모든 location에 공통 헤더 1곳에서 | ⭐ 권고 |
| B. Spring Security 헤더로 개별 적용 | auth만 가능, scraper 등 불가 | 보조 |
| C. 앱별(프론트) meta 태그 | script 영역 헤더 미적용, 우회 가능 | ❌ |

> 주의: auth 모듈은 Spring Security가 동일 헤더를 이미 내려줌 → nginx와 중복 가능.
> 헤더 중복은 보통 무해하지만, 통일성을 위해 **nginx 단일 적용**을 원칙으로 하고
> 추후 auth Spring Security 헤더는 비활성화(`headers().frameOptions().disable()` 등) 검토.

### 3.4 CSP (Content-Security-Policy)

| 대안 | 내용 | 판단 |
|------|------|:---:|
| **A. Vite 프로덕션 빌드 기준 CSP** | 외부 스크립트·스타일만 허용, 인라인 제한 | ⭐ 권고 |
| B. `default-src 'self'` 최소 정책 | 가장 안전하나 기능 충돌 위험 | 상황 따라 |
| C. 적용 안 함 | 현재 상태 | ❌ |

Vite React 프로덕션 권고 예시:

```nginx
add_header Content-Security-Policy "default-src 'self'; script-src 'self'; style-src 'self' 'unsafe-inline'; img-src 'self' data:; font-src 'self' data:; connect-src 'self'; frame-ancestors 'none'; base-uri 'self'; form-action 'self'; upgrade-insecure-requests" always;
```

> 적용 전 반드시 각 SPA에서 콘솔 CSP 위반 경고 확인 필요 (모듈별로 다를 수 있음).

### 3.5 HSTS

| 대안 | 내용 | 판단 |
|------|------|:---:|
| **A. `Strict-Transport-Security` 적용** | HTTPS만 사용 중이므로 안전 | ⭐ 권고 |
| B. 미적용 | - | ❌ |

```nginx
add_header Strict-Transport-Security "max-age=31536000; includeSubDomains" always;
```

### 3.6 로그·상세 설정

| 대안 | 내용 | 판단 |
|------|------|:---:|
| **A. 프로덕션 전용 설정 분리** | `show-sql:false`, 로그 INFO, health when-authorized | ⭐ 권고 |
| B. 현재 상태 유지 | 정보 노출 위험 | ❌ |

---

## 4. 권고안 (요약)

| 영역 | 적용 내용 |
|------|-----------|
| nginx | `server_tokens off;` |
| nginx | `add_header` 일괄: `X-Content-Type-Options: nosniff`, `X-Frame-Options: DENY`, `Referrer-Policy: strict-origin-when-cross-origin`, CSP, HSTS, `Permissions-Policy` |
| nginx | 설정 파일 저장소 버전 관리 (`infra/nginx/`) + 배포 워크플로 반영 |
| Spring(4개 모듈) | `server.server-header:`(빈값), `server.error.whitelabel.enabled: false`, `server.error.include-stacktrace: never` (기본 확인) |
| scraper 프로덕션 | `show_sql: false`, 로그 `INFO`, `health.show-details: when-authorized` |
| Actuator | `info` 노출 불필요 시 제외, 노출 범위 최소화 |
| 공통 | 에러 응답은 `ApiResponse.error(code, message)` 유지 (스택 트레이스 절대 미포함) |

---

## 5. 구현 계획 (체크리스트)

- [ ] nginx `server_tokens off` + 보안 헤더 `add_header` 적용
- [ ] nginx 설정 파일 `infra/nginx/sh-platform.conf` 로 저장소에 추가 + 배포 워크플로 반영
- [ ] 4개 모듈 `application.yml`: `server.server-header`, `server.error.whitelabel.enabled` 설정
- [ ] scraper `application.yml`: `show_sql: false`, 로그 INFO, `health.show-details: when-authorized`
- [ ] auth `management.endpoints` 노출 범위 재검토 (info 제외 검토)
- [ ] CSP 적용 전 각 SPA 콘솔 위반 경고 점검
- [ ] `curl -I`로 응답 헤더 검증 (아래 검증 시나리오)

---

## 6. 검증 시나리오

```bash
# nginx/서버 버전 헤더 (버전 없어야 함)
curl -sI https://sunghoonyk.duckdns.org/ | grep -i '^server:'

# 보안 헤더 확인 (각 앱)
curl -sI https://sunghoonyk.duckdns.org/            | grep -iE 'x-content|x-frame|content-security|strict-transport'
curl -sI https://sunghoonyk.duckdns.org/scraper/    | grep -iE 'x-content|x-frame|content-security'
curl -sI https://sunghoonyk.duckdns.org/platform/   | grep -iE 'x-content|x-frame|content-security'

# 에러 응답에 스택 트레이스 없는지
curl -s https://sunghoonyk.duckdns.org/api/v1/auth/nonexistent
curl -s -X POST https://sunghoonyk.duckdns.org/scraper/docs/crawlers   # 405 JSON 확인

# Whitelabel HTML이 아닌 JSON인지
curl -s https://sunghoonyk.duckdns.org/scraper/존재하지않는경로 | grep -i whitelabel
```

---

## 7. 기대 효과 (OWASP Top 10 매핑)

| OWASP Top 10 (2021) | 대응 항목 |
|---------------------|-----------|
| A05 Security Misconfiguration | server_tokens, Whitelabel off, 보안 헤더, 프로덕션 로그/상세 설정 정리 |
| A09 Security Logging & Monitoring Failures | 오류는 서버 로그에만 기록 (클라이언트 미노출) |
| A03 Injection | SQL은 JPA 파라미터 바인딩 사용 (유지), `show_sql` off |
| A02 Cryptographic Failures | TLS 1.2+ (Let's Encrypt) + HSTS |
