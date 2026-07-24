---
title: Development Guide
description: Development Guide - general module documentation
category: development
created: 2026-07-13
updated: 2026-07-21
---

# 개발 가이드

## 시작하기

### 1. 저장소 구조

```
sh-platform/
├── common/                          # 공통 라이브러리
├── modules/
│   ├── auth/backend/                # 인증 서비스 (port 8080)
│   ├── auth/frontend/               # React 로그인/회원가입
│   ├── scraper/backend/             # 채용공고 수집 (port 8081)
│   ├── scraper/frontend/            # React 스크래퍼 SPA
│   ├── resume/backend/              # 이력서 서비스 (port 8082)
│   └── portfolio/backend/           # 포트폴리오 서비스 (port 8083)
├── platform/frontend/               # 플랫폼 프레임 (대시보드+관리자)
├── docs/                            # 프로젝트 문서
├── scripts/                         # DB 파티션 등 유틸
├── AGENTS.md                        # AI 개발 규칙
└── .env                             # 중앙 설정
```

### 2. 로컬 개발 환경

| 항목 | 요구사항 |
|------|---------|
| Java | 21 LTS |
| Gradle | Wrapper 사용 (`./gradlew`) |
| DB | H2 (local) / MariaDB 10.11 (prod) |
| IDE | IntelliJ IDEA 권장 |

```bash
./gradlew build -x test    # 테스트 제외 빌드
./gradlew test             # 전체 테스트 실행
./gradlew :modules:auth:backend:test  # auth 모듈만 테스트
```

### 3. 모듈 개발 사이클

```
1. docs/ 에서 요구사항/설계 확인
2. Javadoc-style 주석으로 인터페이스 정의 (domain/AuthService.java)
3. 기능 구현 (AuthServiceImpl)
4. 단위 테스트 작성/보강 (src/test/...)
5. ./gradlew test 로 검증
6. git commit → push → Actions 자동 배포
```

### 4. 새 서비스 모듈 추가 규칙

모든 새 서비스 모듈은 다음 구조를 따라야 한다:

```
sh-platform-{module}/
├── src/main/java/com/shplatform/{module}/
│   ├── api/              # Controller + DTO
│   │   ├── {Module}Controller.java
│   │   └── dto/
│   ├── domain/           # Service 인터페이스 + 구현 + 도메인 모델
│   │   ├── {Module}Service.java
│   │   ├── {Module}ServiceImpl.java
│   │   └── {Module}.java
│   └── infrastructure/   # JPA Entity + Repository + Mapper
│       ├── {Module}Entity.java
│       ├── {Module}Repository.java
│       └── {Module}Mapper.java
├── src/test/java/com/shplatform/{module}/
│   └── domain/
│       └── {Module}ServiceImplTest.java
└── build.gradle.kts
```

---

## Javadoc

### 개념

Java 소스 코드에 `/** ... */` 주석을 달면 Gradle이 HTML 문서를 자동 생성한다.

```java
/**
 * 회원 정보를 조회한다.
 *
 * @param userId 사용자 ID (PK)
 * @return User 객체 (email, name, role 포함)
 * @throws BusinessException NOT_FOUND - 사용자가 존재하지 않는 경우
 */
public User getUser(Long userId);
```

### 생성 명령어

```bash
./gradlew javadoc
# → build/docs/javadoc/index.html
```

### 웹에서 보기

```
https://sunghoonyk.duckdns.org/javadoc/
```

### CI 연동

GitHub Actions deploy 시 `./gradlew :modules:auth:backend:javadoc` 이 자동 실행되어 서버에 문서가 갱신된다.

> 상세: `docs/guides/javadoc-guide.md`

---

## JUnit 테스트 리포트

### 개념

JUnit 테스트 코드를 실행하면 Gradle이 결과를 HTML 리포트로 생성한다.

```bash
./gradlew test
  ↓
build/reports/tests/test/index.html   ← 브라우저용
build/test-results/test/*.xml          ← CI가 읽는 데이터
```

### 화면 구성

```
Test Summary
  tests: 29  failures: 0
  ─────────────────────
  Packages →
    com.shplatform.auth.domain (29 tests, 0 failures)
         ↓ 클릭
  각 테스트 메서드별 성공(초록) / 실패(빨강) / 실행시간
```

### 웹에서 보기

```
https://sunghoonyk.duckdns.org/test-reports/
```

### CI 연동

`.github/workflows/deploy-backend.yml` 에서:

```yaml
- name: Upload Test Report
  uses: actions/upload-artifact@v4
  if: success() || failure()
  with:
    name: test-report
    path: '**/build/reports/tests/test/'
```

실패해도 artifact는 업로드되므로 원인 분석 가능. GitHub Actions 화면 하단 **Artifacts** 섹션에서 `test-report.zip` 다운로드 가능.

---

## Swagger (SpringDoc) API 문서

### 개념

- Javadoc이 "코드 설명서"라면, Swagger는 "API 테스트 도구"
- `@RestController`를 자동 스캔해서 UI 생성
- **"Try it out"** 버튼으로 브라우저에서 직접 API 호출 가능

### 적용

`modules/auth/backend/build.gradle.kts`:

```kotlin
implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.8.6")
```

이 한 줄로 Spring Boot가 자동으로 모든 `@RestController`를 스캔한다.

### Security 예외 경로

`SecurityConfig.java` 에 Swagger UI 경로를 인증 예외 처리:

```java
.requestMatchers(
    "/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**"
).permitAll()
```

### Config 클래스

```java
@Bean
public OpenAPI openAPI() {
    return new OpenAPI()
            .info(new Info()
                    .title("sh-platform API")
                    .description("인증 · AI Housing 통합 플랫폼 API 문서")
                    .version("1.0.0"))
            .servers(List.of(
                    new Server().url(serverUrl).description("현재 서버")
            ));
}
```

### 웹에서 보기

```
https://sunghoonyk.duckdns.org/swagger-ui/
```

### 어노테이션 기본

```java
@Tag(name = "Auth", description = "인증 API")             // 컨트롤러 그룹
@Operation(summary = "회원가입", description = "...")       // API 설명
@ApiResponse(responseCode = "201", description = "성공")    // 응답 정의
```

필수는 아님. 어노테이션 없어도 DTO 필드 + HTTP 메서드 기준으로 자동 생성됨.

---

## 웹 접근 경로 총정리

| 경로 | 내용 | 인증 |
|------|------|:----:|
| `https://sunghoonyk.duckdns.org/swagger-ui/` | API 문서 + Try it out | ❌ |
| `https://sunghoonyk.duckdns.org/test-reports/` | JUnit 테스트 결과 (HTML) | ❌ |
| `https://sunghoonyk.duckdns.org/javadoc/` | Javadoc 코드 문서 | ❌ |
| `https://sunghoonyk.duckdns.org/api/` | REST API 프록시 | ✅ |
| `https://sunghoonyk.duckdns.org/webhook` | Webhook 수신 | ❌ |

> `test-reports/`와 `javadoc/`은 deploy 시 서버에서 `./gradlew test + javadoc` 실행 후 갱신된다.

## Actions Artifact 관리

| 항목 | 내용 |
|------|------|
| 업로드 대상 | `build/reports/tests/test/` (HTML 리포트) |
| 보관 기간 | **90일** (GitHub 기본 정책, 자동 삭제) |
| 다운로드 위치 | Actions 워크플로우 하단 **Artifacts** 섹션 |
| 용도 | 배포 실패 시 원인 분석, 로컬에서 열어보기 |

## 전체 플로우

```
git push
  → GitHub Actions 실행
      1. ./gradlew :modules:auth:backend:build  (테스트 포함)
      2. JUnit 테스트 29개 자동 실행
         └── 실패 시 → deploy 중단, artifact에서 원인 확인
         └── 성공 시 → 계속 진행
      3. 테스트 리포트 HTML artifact 업로드 (90일 보관)
      4. SSH로 서버 배포
         ├── git pull
         ├── ./gradlew build + test (서버에서 테스트 재실행)
         ├── ./gradlew javadoc (문서 생성)
         └── sudo systemctl restart sh-platform
  → 웹에서 즉시 확인 가능:
       ├── https://sunghoonyk.duckdns.org/swagger-ui/     ← API 명세 + 테스트
       ├── https://sunghoonyk.duckdns.org/test-reports/   ← JUnit 결과
       └── https://sunghoonyk.duckdns.org/javadoc/        ← 코드 문서
```

---

## 필수 문서 체크리스트

| 문서 | 갱신 대상 |
|------|-----------|
| `docs/architecture/standards.md` | 표준 위반 시 |
| `docs/architecture/erd.md` | 새 테이블 추가 시 |
| `docs/architecture/sql-ddl.md` | 새 테이블 추가 시 |
| `docs/auth/api-auth.md` | 새 API 엔드포인트 추가 시 |
| `docs/auth/frontend-auth-guide.md` | 프론트 영향 있는 경우 |
| `docs/infra/domain-ssl-setup-guide.md` | 인프라 변경 시 |
| `docs/roadmap.md` | 일정 변경 시 |
| `docs/development-guide.md` | (본 문서 - 프로세스 변경 시) |

## 참조 문서 인덱스

| 목적 | 문서 |
|------|------|
| 전반 아키텍처 | `docs/architecture/architecture.md` |
| 개발 표준 (Javadoc, JUnit, 패키지 구조, 네이밍) | `docs/architecture/standards.md` |
| ERD / 테이블 설계 | `docs/architecture/erd.md` |
| API 명세 | `docs/auth/api-auth.md` |
| OAuth2 설정 | `docs/auth/oauth2-registration-guide.md` |
| 프론트 연동 | `docs/front/integration-guide.md` |
| 배포 인프라 | `docs/infra/domain-ssl-setup-guide.md` |
| 프로젝트 로드맵 | `docs/roadmap.md` |

## 도구별 상세 가이드

| 도구 | 문서 | 주요 내용 |
|------|------|----------|
| **Javadoc** | `docs/guides/javadoc-guide.md` | 작성 규칙, 생성 명령어, Swagger와 차이점 |
| **JUnit** | `docs/guides/test-report-guide.md` | 리포트 구조, 로컬/CI 확인법, Artifact 관리 |
| **Swagger** | `docs/guides/swagger-guide.md` | 적용 방법, 어노테이션, Security 예외 처리 |
| **nginx** | `docs/guides/nginx-guide.md` | 설정 파일 위치, location 블록 설명, 자주 쓰는 명령어 |

---

## SchemaSpy (DB 문서 자동화)

### 개념

데이터베이스 메타데이터를 분석하여 **HTML 형태의 ERD, 테이블 관계, 컬럼 상세** 문서를 자동 생성한다.

### 생성 명령어

```bash
# 전체 DB 문서화
sudo /opt/schemaSpy/run-all.sh

# 특정 DB만 (sh_pass)
sudo /opt/schemaSpy/run-schemaSpy.sh
```

### 웹에서 보기

```
https://sunghoonyk.duckdns.org/schemaSpy/sh_pass/index.html
```

### 자동화

- **systemd timer**: 매주 일요일 03:00 자동 실행
- **수동 실행**: DB 변경 시 `sudo /opt/schemaSpy/run-all.sh`



### 자동 발견 방식

- sh_ 접두사가 있는 모든 DB를 자동으로 문서화
- 새 DB 생성 시 별도 설정 불필요
- 매주 일요일 03:00 자동 실행


---

## 모니터링 (Prometheus + Grafana)

### 구성 요소

| 도구 | 용도 | 포트 |
|------|------|------|
| Prometheus | 메트릭 수집 및 저장 | :9090 |
| Grafana | 대시보드 시각화 | :3000 |
| Node Exporter | 서버 메트릭 (CPU/RAM/Disk) | :9100 |
| Micrometer | Spring Boot 메트릭 | :8080/actuator |

### 접속 경로

```
Grafana:    https://sunghoonyk.duckdns.org/grafana/
Prometheus: https://sunghoonyk.duckdns.org/prometheus/
```

### 기본 로그인

- ID: admin
- PW: admin (초기 비밀번호)

### systemd 서비스

```bash
sudo systemctl status prometheus
sudo systemctl status grafana-server
sudo systemctl status prometheus-node-exporter
```

