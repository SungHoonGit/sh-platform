---
title: AGENTS
description: AI 코딩 에이전트 프로젝트 규칙 (프로젝트 구조, 코드 규칙, 문서 규칙)
category: config
created: 2026-07-17
updated: 2026-07-21
---

# AGENTS.md — AI 개발 규칙

이 파일은 AI 코딩 에이전트(opencode, cursor, copilot 등)가 프로젝트 규칙을 자동 인식하도록 합니다.
이 파일을 수정하면 AI 모델이 다음 세션부터 변경된 규칙을 따릅니다.

---

## 프로젝트 구조 (Monorepo)

```
sh-platform/
├── common/                          # sh-platform-common (공통 라이브러리)
├── modules/
│   ├── auth/backend/                # 인증 서비스 (port 8080)
│   ├── auth/frontend/               # React 로그인/회원가입
│   ├── scraper/backend/             # 채용공고 수집 (port 8081)
│   ├── scraper/frontend/            # React 스크래퍼 SPA
│   ├── resume/backend/              # 이력서 서비스 (port 8082)
│   └── portfolio/backend/           # 포트폴리오 서비스 (port 8083)
├── platform/frontend/               # 플랫폼 프레임 (대시보드+관리자, /platform/)
├── docs/                            # 프로젝트 전체 문서
├── scripts/                         # DB 파티션 등 유틸 스크립트
└── keys/                            # SSH/SSL 키 (git 제외)
```

### Gradle 모듈 경로

```
:common                           → common/
:modules:auth:backend             → modules/auth/backend/
:modules:scraper:backend          → modules/scraper/backend/
:modules:resume:backend           → modules/resume/backend/
:modules:portfolio:backend        → modules/portfolio/backend/
```

## 기술 스택

| 항목 | 버전 |
|------|------|
| Java | 21 LTS (OpenJDK ARM64) |
| Spring Boot | 3.4.4 |
| Gradle | Kotlin DSL, wrapper |
| DB | MariaDB 10.11.14 (prod), H2 (local) |
| Frontend | React 19 + Vite 8 + Tailwind 4 + TypeScript 6 |
| CI/CD | GitHub Actions → SSH 배포 |

## 서버 정보

| 항목 | 값 |
|------|-----|
| 도메인 | sunghoonyk.duckdns.org |
| IP | 140.245.95.162 |
| DB IP | 10.0.0.39 (internal) |
| SSH | `ssh oci-web` (alias) |
| SSH Key | ~/.ssh/oci/140.245.95.162/ssh-key-2026-07-11.key |
| 중앙 설정 | /home/ubuntu/sh-platform/.env |

## 포트 매핑

| 포트 | 서비스 | Swagger | URL 프리픽스 |
|------|--------|---------|-------------|
| 8080 | auth | `/swagger-ui/` | `/api/*` |
| 8081 | scraper | `/scraper/swagger-ui/` | `/scraper/*` |
| 8082 | resume | `/resume/swagger-ui/` | `/resume/*` |
| 8083 | portfolio | `/portfolio/swagger-ui/` | `/portfolio/*` |
| 9090 | Prometheus | - | `/prometheus/` |
| 3000 | Grafana | - | `/grafana/` |

## systemd 서비스

```bash
# 상태 확인
sudo systemctl status sh-platform-{auth,scraper,resume,portfolio}

# 개별 재시작
sudo systemctl restart sh-platform-auth

# 전체 재시작 (순서 중요: common 영향 받는 것들)
sudo systemctl stop sh-platform-{portfolio,resume,scraper,auth}
sudo fuser -k 8080/tcp 8081/tcp 8082/tcp 8083/tcp 2>/dev/null
sudo systemctl start sh-platform-auth && sleep 20 && \
sudo systemctl start sh-platform-scraper sh-platform-resume sh-platform-portfolio
```

**포트 충돌 해결**: `ss -tlnp | grep 8080` → `sudo fuser -k 8080/tcp`

## 개발 사이클 (중요)

모든 기능 개발은 다음 순서를 따릅니다:

```
1. 설계 문서 (docs/) 확인
2. DB 설계 (docs/architecture/erd.md, sql-ddl.md)
3. 인터페이스 정의 + Javadoc 작성
4. 구현 (api/ → domain/ → infrastructure/ 순서)
5. 단위 테스트 (JUnit 5)
6. 빌드 검증 → ./gradlew :modules:auth:backend:test
7. git commit → push
8. GitHub Actions 자동 배포
9. 산출물 자동 생성:
   ├── Swagger UI: https://sunghoonyk.duckdns.org/swagger-ui/
   ├── Javadoc:    https://sunghoonyk.duckdns.org/javadoc/
   ├── 테스트 리포트: https://sunghoonyk.duckdns.org/test-reports/
   └── DB 문서:     https://sunghoonyk.duckdns.org/schemaSpy/
```

## 문서 규칙

### Frontmatter 표준

모든 `.md` 파일은 YAML frontmatter를 포함해야 합니다:

```yaml
---
title: 문서 제목
description: 한 줄 요약
category: 카테고리
created: 2026-07-21
updated: 2026-07-21
---
```

- `title`: 파일명 기반 kebab-case → Title Case 변환
- `category`: 상위 디렉토리명 (architecture, auth, scraper, guide, infra, common, database, plan, daily, front, config, readme)
- `created` / `updated`: YYYY-MM-DD 형식
- 템플릿 참고: `docs/frontmatter-template.md`

### 문서 저장 위치

| 구분 | 경로 | 예시 |
|------|------|------|
| 프로젝트 문서 | `docs/` | `docs/auth/api-auth.md` |
| 설계 문서 | `docs/architecture/` | `docs/architecture/erd.md` |
| 가이드 | `docs/guides/` | `docs/guides/nginx-guide.md` |
| 작업 일지 | `docs/daily/` | `docs/daily/2026-07-21-work-log.md` |
| AI 참고용 외부 문서 | `~/iCloudDrive/0.memo/{프로젝트명}/docs/` | (agent 전용, git 미포함) |

- AI 에이전트 생성 문서는 iCloudDrive 경로 우선
- 프로젝트에 포함될 문서는 `docs/` 하위에 작성

## 코드 규칙

### 패키지 구조 (모든 모듈 동일)

```
com.shplatform.{module}/
├── api/              # @RestController + DTO
│   ├── {Module}Controller.java
│   └── dto/
├── domain/           # @Service + 도메인 모델 (Spring 의존 없음)
│   ├── {Module}Service.java     ← 인터페이스
│   └── {Module}ServiceImpl.java ← 구현
└── infrastructure/   # @Repository + Entity + Mapper
    ├── {Module}Entity.java
    └── {Module}Repository.java
```

### 레이어 규칙

- `domain/` → `api/`, `infrastructure/` import **가능**
- `api/` → `domain/` import 가능, `infrastructure/` import **금지**
- `infrastructure/` → `domain/` import 가능, `api/` import **금지**

### Javadoc 필수 대상

- public 인터페이스 메서드: 필수
- BusinessException ErrorCode: 필수
- DTO record: 권장
- `@Override` 메서드: 생략

```java
/**
 * (명령형) 회원가입을 처리한다.
 *
 * @param request 이메일, 비밀번호, 이름
 * @return 생성된 사용자 정보
 * @throws BusinessException EMAIL_NOT_VERIFIED
 */
User signup(SignupRequest request);
```

### 테스트

- Service 테스트: `@ExtendWith(MockitoExtension.class)` (Spring 미기동)
- Repository 테스트: `@DataJpaTest`
- Controller 테스트: `@WebMvcTest`
- 커버리지: Service public 메서드 100%

```bash
./gradlew :modules:auth:backend:test
./gradlew :modules:auth:backend:test --tests "*AuthServiceImplTest*"
```

### 커밋 컨벤션

```
feat:     새 기능
fix:      버그 수정
docs:     문서
refactor: 리팩토링
test:     테스트
chore:    빌드/설정
```

### Swagger 어노테이션

Controller에 권장 (없어도 동작하지만 명시 권장):

```java
@RestController
@RequestMapping("/api/v1/admin")
@Tag(name = "Admin", description = "관리자 API")
public class AdminController {

    @GetMapping("/stats")
    @Operation(summary = "대시보드 통계")
    public ResponseEntity<ApiResponse<AdminStatsResponse>> getStats() { ... }
}
```

## DB 표준

| 항목 | 규칙 |
|------|------|
| 테이블명 | snake_case 복수형 (`users`, `crawl_logs`) |
| 컬럼명 | snake_case (`email_verified`, `created_at`) |
| PK | `id` BIGINT AUTO_INCREMENT |
| FK | `{테이블명}_id` |
| 인덱스 | `idx_{테이블명}_{컬럼명}` |
| 로그 테이블 | MONTHLY RANGE 파티션 |

## 주의사항

- **포트 충돌**: 이전 모너리포 구조 잔존 프로세스가 포트를 점유할 수 있음 → `ss -tlnp | grep 8080` 확인
- **systemd 재시작**: `Restart=always` 설정 → 포트 충돌 시 무한 재시작 루프 → 먼저 기존 프로세스 종료
- **.env 파일**: `/home/ubuntu/sh-platform/.env` — systemd와 Spring Boot 모두 읽음
- **Swagger에 새 API 안 나올 시**: AdminController처럼 빈 생성 실패일 수 있음 → 로그 확인

## 유용한 커맨드

```bash
# 서버 접속
ssh oci-web

# 전체 서비스 상태
sudo systemctl status sh-platform-*

# 포트 사용 현황
ss -tlnp | grep 808

# 특정 서비스 로그
sudo journalctl -u sh-platform-auth --since "5 min ago" -f

# 전체 재빌드
cd /home/ubuntu/sh-platform && ./gradlew clean build

# DB 접속
mysql -h 10.0.0.39 -u sh_user -p sh_pass

# Swagger 확인
curl -s http://localhost:8080/v3/api-docs | python3 -m json.tool | head -20
```
