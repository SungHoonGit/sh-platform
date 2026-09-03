# AGENTS.md — AI 개발 규칙

이 파일은 AI 코딩 에이전트(opencode, cursor, copilot 등)가 프로젝트 규칙을 자동 인식하도록 합니다.
이 파일을 수정하면 AI 모델이 다음 세션부터 변경된 규칙을 따릅니다.

> **⚠️ 진행 중인 작업**: 계정 설정 비밀번호 변경 UX 개선 구현 완료 — `WRONG_CURRENT_PASSWORD`(400) 신설, 프론트 규칙/로딩/명확한 오류 문구. 커밋/푸시 대기.
> **세션 시작 시 반드시 `docs/daily/2026-09-03-work-log.md`를 먼저 읽고 이어서 작업할 것.**
> 최근 완료: Phase 9 공유 링크 구현·배포·검증(설계 024, `95ae899`) + 계정 설정 비밀번호 오류 UX 개선(본 세션).
> 다음: 계정 설정 개선 배포 확인, 공유 링크·이력서 PDF 실사용자 최종 확인, 로드맵 백로그(공고-이력서 키워드 매칭/AI 자소 첨삭/이메일 알림).

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
│   ├── resume/backend/              # 이력서 서비스 (port 8082, 포트폴리오 통합)
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
| 8081 | scraper | `/scraper/swagger-ui/index.html` | `/scraper/*` |
| 8082 | resume | `/resume/swagger-ui/index.html` | `/resume/*` |
| 9090 | Prometheus | - | `/prometheus/` |
| 3000 | Grafana | - | `/grafana/` |

## systemd 서비스

```bash
# 상태 확인
sudo systemctl status sh-platform-{auth,scraper,resume}

# 개별 재시작
sudo systemctl restart sh-platform-auth

# 전체 재시작 (순서 중요: common 영향 받는 것들)
sudo systemctl stop sh-platform-{resume,scraper,auth}
sudo fuser -k 8080/tcp 8081/tcp 8082/tcp 2>/dev/null
sudo systemctl start sh-platform-auth && sleep 20 && \
sudo systemctl start sh-platform-scraper sh-platform-resume
```

**포트 충돌 해결**: `ss -tlnp | grep 8080` → `sudo fuser -k 8080/tcp`

**중요: 배포 시 JAR 복사 필요**

Gradle 빌드 출력 → systemd 실행 경로가 다름:

| 모듈 | 빌드 출력 | systemd 경로 |
|------|-----------|-------------|
| auth | `modules/auth/backend/build/libs/sh-platform-auth-*.jar` | `builds/sh-platform-auth.jar` |
| scraper | `modules/scraper/backend/build/libs/sh-platform-scraper-*.jar` | `builds/sh-platform-scraper.jar` |
| resume | `modules/resume/backend/build/libs/sh-platform-resume-*.jar` | `builds/sh-platform-resume.jar` |

**수동 배포 시**: `cp modules/scraper/backend/build/libs/sh-platform-scraper-*.jar builds/sh-platform-scraper.jar`

**인프라 설정 단일 소스 (SSOT)**: 서비스명/포트는 `infra/services.yml`이 원본. 수정 후 반드시
`python scripts/render_config.py` 실행 (prometheus/promtail/systemd/nginx 생성물 갱신, 가이드: docs/guides/008-260821-infra-ssot-guide.md).

**`ddl-site-search-mapping.sql`** 실행 필요: `mysql -h 10.0.0.39 -u sh_user -p'SHpass1234!' scraper_platform < docs/scraper/ddl-site-search-mapping.sql`

DB명은 `scraper_platform` (sh_platform 아님).

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

## 코드 규칙

> # 🚫 하드코딩 지양 (최우선 원칙)
> **어떤 기능·작업이든 시작 전/후에 반드시 이 원칙을 체크한다 (항상 적용).**
> '지금은 괜찮지 않을까'는 예외가 아니다. 지금 DB화해 두면 나중에 추가/변경이 배포 없이 끝나고,
> 미뤄두면 하드코딩 제거(마이그레이션) 비용이 훨씬 커진다.
>
> **기준/마스터 데이터에 해당하는 값 — 절대 코드에 둘 수 없다:**
> 학교, 전공, 지역·법정동코드, 직무·카테고리 코드, 메뉴, 공통코드, 옵션(드롭다운 목록) 등.
> → 반드시 **DB 테이블(코드화) + 조회/검색 API**(`GET .../search?q=`)로 제공한다.
>
> **매 작업에 적용되는 필수 행동 체크리스트:**
> - [ ] 내가 만드는 값(코드·옵션·매핑·드롭다운·상수)이 "나중에 추가/변경될 가능성"이 있는가? → 있으면 DB화.
> - [ ] 프론트 하드코딩 배열(`schools.ts`, 지역 목록, 코드 목록 등)을 만들고 있는가? → 만들지 말고 DB+API로.
> - [ ] 백엔드 `if/switch/map` 으로 카테고리·코드·직무를 하드코딩 매핑하고 있는가? → DB 테이블로.
> - [ ] 환경값(DB URL·키·경로·주소)을 코드에 박고 있는가? → `application-{env}.yml`/`.env`로 분리.
> - [ ] 새 기준 데이터 추가 시 절차: (1) `docs/.../ddl-resume-vN.sql`에 테이블+시드(멱등 INSERT), (2) Entity+Repository, (3) 조회/검색 API, (4) 프론트는 그 API 호출, (5) 배포 워크플로우에 DDL 라인 추가.
> - [ ] 코드에 상수를 둘 수 있는 예외는 오직 (1) 절대 바뀌지 않는 물리 상수(예: 1MB=1048576), (2) DB화 실익이 없는 자잘한 표시 문자열뿐. 기준 데이터·코드류는 예외 없음.
> - [ ] 기존 하드코딩 잔재 발견 시 → 즉시 `docs/plans/` 에 전환 과제로 기록하고 진행.

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

---

## 문서 규칙

### 중요성

> **문서는 코드와 동일하게 중요합니다.**
> - 문서 없이는 유지보수가 불가능합니다
> - AI 에이전트는 문서를 통해 프로젝트 맥락을 이해합니다
> - 좋은 문서 = 낮은 버그 발생률 + 빠른 온보딩

### 문서 구조 (도서관식)

```
docs/
├── daily/              # 📅 작업 일지
├── plans/              # 📋 설계/기획 (산출물)
├── guides/             # 📚 가이드/개념서
├── errors/             # 🔴 오류/이슈 기록
├── learnings/          # 📖 배움/학습 기록
├── architecture/       # 🏗️ 아키텍처
├── scraper/            # 🔍 스크래퍼
├── auth/               # 🔐 인증
├── database/           # 💾 데이터베이스
├── infra/              # 🖥️ 인프라
└── security/           # 🛡️ 보안
```

### 파일 명명 규칙 (네이밍 컨벤션)

| 유형 | 형식 | 예시 |
|------|------|------|
| **작업 일지** | `YYYY-MM-DD-work-log.md` | `2026-08-04-work-log.md` |
| **할 일** | `YYYY-MM-DD-todo.md` | `2026-08-04-todo.md` |
| **설계 문서** | `NNN-YYMMDD-<주제>-design.md` | `001-260804-job-analytics-design.md` |
| **가이드 문서** | `NNN-YYMMDD-<주제>-guide.md` | `001-260804-sse-guide.md` |
| **오류 기록** | `NNN-YYMMDD-<주제>-error.md` | `001-260804-build-error-fix.md` |
| **학습 기록** | `NNN-YYMMDD-<주제>-learning.md` | `001-260804-sse-concept.md` |

> `NNN`: 3자리 넘버링 (001부터 시작)
> `YYMMDD`: 날짜 (연도2자리+월2자리+일2자리)

### 파일 저장 위치

| 구분 | 경로 |
|------|------|
| DDL | `docs/scraper/` |
| 아키텍처 | `docs/architecture/` |
| **작업 일지** | `docs/daily/` |
| **설계 문서** | `docs/plans/` |
| **가이드 문서** | `docs/guides/` |
| **오류 기록** | `docs/errors/` |
| **학습 기록** | `docs/learnings/` |
| API 문서 | Swagger 자동 생성 |

**모든 MD 파일은 `docs/` 디렉토리 아래에 저장한다.**

---

### 템플릿: 작업 일지 (`YYYY-MM-DD-work-log.md`)

```markdown
# YYYY-MM-DD 작업 일지

## 작업 환경
- 날짜: YYYY-MM-DD (요일)
- 시간: HH:MM 기준
- 브랜치: branch-name

## 오늘 완료된 작업
1. [작업1]: 간략한 설명
2. [작업2]: 간략한 설명

## 변경된 파일 목록
- `파일경로`: 변경 내용

## 이슈 및 해결
- [이슈1]: 원인 + 해결 방법

## 커밋 내역
| 시간 | 커밋 | 내용 |
|------|------|------|
| HH:MM | abc1234 | 커밋 메시지 |

## 향후 작업
1. [작업1]
2. [작업2]
```

### 템플릿: 설계 문서 (`NNN-YYMMDD-<주제>-design.md`)

```markdown
# NNN-YYMMDD-<주제> 설계 문서

## 개요
- **목적**: 이 문서의 목적
- **범위**: 적용 범위
- **작성일**: YYYY-MM-DD
- **작성자**: AI Assistant / 사용자

## 1. 배경 및 이유
이 기능/문제가 필요한 이유

## 2. 요구 사항
### 2.1 기능 요구 사항
- [ ] FR-001: 기능1
- [ ] FR-002: 기능2

### 2.2 비기능 요구 사항
- 성능: 
- 보안: 

## 3. 설계
### 3.1 아키텍처
### 3.2 데이터 모델
### 3.3 API 설계

## 4. 구현 계획
| 단계 | 내용 | 예상 기간 |
|------|------|-----------|
| Phase 1 | | |

## 5. 참고 자료
- [링크1](url)

---
*작성일: YYYY-MM-DD*
```

### 템플릿: 가이드 문서 (`NNN-YYMMDD-<주제>-guide.md`)

```markdown
# NNN-YYMMDD-<주제> 가이드

## 개요
- **목적**: 이 가이드의 목적
- **대상**: 누구를 위한 가이드인지
- **작성일**: YYYY-MM-DD

## 1. 개념
### 1.1 정의
### 1.2 왜 필요한가

## 2. 설정 방법
### 2.1 사전 조건
### 2.2 설치/설정 단계

## 3. 사용법
### 3.1 기본 사용
### 3.2 고급 사용

## 4. 문제 해결
| 문제 | 원인 | 해결책 |
|------|------|--------|

## 5. 참고 자료
- [링크1](url)

---
*작성일: YYYY-MM-DD*
```

### 템플릿: 오류 기록 (`NNN-YYMMDD-<주제>-error.md`)

```markdown
# NNN-YYMMDD-<주제> 오류 기록

## 개요
- **발생일**: YYYY-MM-DD
- **환경**: Windows/Linux, Java 21, Spring Boot 3.4.4
- **심각도**: 🔴 Critical / 🟡 Warning / 🟢 Low

## 1. 오류 현상
### 1.1 에러 메시지
```
(에러 로그 전체)
```

### 1.2 재현 단계
1. 단계1
2. 단계2

## 2. 원인 분석
### 2.1 근본 원인
(어디서, 왜 발생했는지)

### 2.2 관련 코드
- 파일: `path/to/file.java:줄번호`
- 코드: `(해당 코드 snippet)`

## 3. 해결 방법
### 3.1 해결 과정
(어떻게 해결했는지)

### 3.2 최종 코드 변경
```java
// 변경 전
// 변경 후
```

## 4. 예방 방법
-이후 동일 오류를 방지하기 위한 방법

## 5. 참고 자료
- 관련 링크

---
*작성일: YYYY-MM-DD*
```

### 템플릿: 학습 기록 (`NNN-YYMMDD-<주제>-learning.md`)

```markdown
# NNN-YYMMDD-<주제> 학습 기록

## 개요
- **주제**: 학습한 기술/개념
- **학습일**: YYYY-MM-DD
- **수준**: 초급 / 중급 / 고급

## 1. 개념 설명
### 1.1 정의
(이것이 무엇인가)

### 1.2 왜 필요한가
(어떤 문제를 해결하는가)

### 1.3 관련 개념
- 개념1: 설명
- 개념2: 설명

## 2. 사용법
### 2.1 기본 사용
```java
// 기본 사용 예시
```

### 2.2 고급 사용
```java
// 고급 사용 예시
```

## 3. 주의사항
- 주의1
- 주의2

## 4. 실전 적용
### 4.1 이 프로젝트에서의 적용
(어떻게 적용했는지)

### 4.2 관련 코드
- 파일: `path/to/file.java`

## 5. 참고 자료
- [공식 문서](url)
- [관련 블로그](url)

---
*작성일: YYYY-MM-DD*
```

### 템플릿: 할 일 (`YYYY-MM-DD-todo.md`)

```markdown
# YYYY-MM-DD 할 일

## 오늘 목표
1. [목표1]

## 작업 목록
### 🔴 높은 우선순위
- [ ] 작업1

### 🟡 중간 우선순위
- [ ] 작업2

### 🟢 낮은 우선순위
- [ ] 작업3

## 완료된 작업
- [x] 작업4

## 메모
- 
```

---

### 문서 분류

| 문서 유형 | 예시 | 위치 |
|-----------|------|------|
| DDL | `ddl-job-postings.sql` | `docs/scraper/` |
| 아키텍처 | `erd.md`, `sql-ddl.md` | `docs/architecture/` |
| API 문서 | Swagger 자동 생성 | 서버에서 자동 |
| **작업 일지** | `2026-08-04-work-log.md` | `docs/daily/` |
| **설계 문서** | `001-260804-*-design.md` | `docs/plans/` |
| **가이드 문서** | `001-260804-*-guide.md` | `docs/guides/` |
| **오류 기록** | `001-260804-*-error.md` | `docs/errors/` |
| **학습 기록** | `001-260804-*-learning.md` | `docs/learnings/` |

---

### AI 에이전트 주의 사항

> **이 규칙을 반드시 따를 것:**
> 1. 새 문서 생성 시 반드시 위 템플릿 사용
> 2. 파일명은 네이밍 컨벤션 준수
> 3. 문서 작성 후 AGENTS.md의 파일 목록 업데이트
> 4. 기존 문서 수정 시에도 형식 유지
> 5. 오류 해결 시 `docs/errors/`에 기록
> 6. 새 기술 학습 시 `docs/learnings/`에 기록
