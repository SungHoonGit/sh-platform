# 007-260820-local-dev-guide

## 개요
- **목적**: 로컬 개발 환경에서 각 모듈을 실행하고 확인하는 방법 안내
- **대상**: 신규 개발자 / 로컬 실행이 필요한 모든 개발자
- **작성일**: 2026-08-20

## 1. 아키텍처 개요

### 1.1 로컬 구성

```
Windows 개발 PC
├── Docker Desktop (WSL2)
│   └── MariaDB 컨테이너 1개 (포트 3307) ← DB만 Docker
└── Spring Boot 모듈 4개 (Windows에서 직접 실행)
    ├── auth       (8080)
    ├── scraper    (8081)
    ├── resume     (8082)
    └── portfolio  (8083)
```

- **DB만 Docker**로 실행하고, **앱은 로컬에서 직접 실행** (개발 중 수정→재시작이 빨라서 표준 방식)
- 운영은 nginx가 URL 경로로 라우팅하지만, 로컬은 개발자가 포트를 직접 접속

### 1.2 React 프론트 서빙 방식

**React를 별도 실행할 필요 없음** — Spring Boot가 정적 파일로 통째로 서빙:

```
build.gradle.kts의 copyFrontendDist
    modules/<module>/frontend/dist  →  build/resources/main/static
Spring Boot가 static/을 자동 서빙
```

| 모듈 | 프론트 포함 | 확인 방법 |
|------|------------|-----------|
| auth | ✅ (dist 존재) | `http://localhost:8080/` → 로그인 화면 |
| scraper | ✅ | `http://localhost:8081/` |

## 2. 사전 준비

### 2.1 Docker Desktop + WSL2 (최초 1회)
- Docker Desktop 설치 → WSL2 백엔드 사용
- 가상화 미활성화 시: 관리자 PowerShell로 `dism /enable-feature:VirtualMachinePlatform` + BIOS에서 VT-x 활성화
- 상세: `docs/learnings/004-260820-wsl2-docker-windows-learning.md`

### 2.2 STS4 실행 환경 (Lombok)
- STS4는 **Java 21**로 실행해야 Lombok이 동작 (내장 Java 25는 Lombok 1.18.36 미지원)
- ini에서 `-vm`을 시스템 Java 21로 지정:
  ```
  -vm
  C:/Program Files/Eclipse Adoptium/jdk-21.0.12.8-hotspot/bin/javaw.exe
  -vmargs
  -javaagent:C:/Users/KIM/lombok.jar
  ```
- lombok.jar 위치: `C:/Users/KIM/lombok.jar` (한글 경로가 있으면 인코딩 깨짐 → ASCII 경로 필수)

## 3. DB 시작

### 3.1 컨테이너 기동
```bash
docker compose up -d
```

### 3.2 DB 정보
| 항목 | 값 |
|------|-----|
| 호스트 포트 | **3307** (Windows 기존 MySQL이 3306 점유 중이라 우회) |
| 컨테이너 내부 | 3306 |
| 계정 | `sh_local` / `sh_local_pass` |
| DB | `sh_pass`, `scraper_platform`, `resume_platform`, `portfolio_platform` |
| 초기화 | `scripts/local-db-init.sql` (DB 4개 생성, 컨테이너 최초 생성 시 1회 실행) |

> 테이블은 **JPA `ddl-auto: update`가 자동 생성** — 별도 DDL 파일 불필요. 코드와 스키마가 항상 동기화.

## 4. 모듈 실행

### 4.1 CLI 실행 (권장)

```bash
# auth
./gradlew :modules:auth:backend:bootRun --args='--spring.profiles.active=local'

# scraper
./gradlew :modules:scraper:backend:bootRun --args='--spring.profiles.active=local'

# resume
./gradlew :modules:resume:backend:bootRun --args='--spring.profiles.active=local'

# portfolio
./gradlew :modules:portfolio:backend:bootRun --args='--spring.profiles.active=local'
```

> `--spring.profiles.active=local` 필수. 없으면 prod DB(10.0.0.39)로 접속 시도 → 실패.

### 4.2 STS4 실행
1. 프로젝트 우클릭 → Refresh (F5)
2. Run Configuration → Program arguments: `--spring.profiles.active=local`
3. 실행

> STS4에서 실행 전 `build/resources/main`에 리소스가 있는지 확인. 없으면 `./gradlew :modules:auth:backend:processResources` 실행.

## 5. 확인

| 서비스 | 주소 | 확인 포인트 |
|--------|------|-------------|
| auth | `http://localhost:8080/` | React 로그인 화면 (200) |
| auth API | `http://localhost:8080/api/health` | `{"status":"UP"}` |
| scraper | `http://localhost:8081/` | 스크래퍼 화면 |
| resume | `http://localhost:8082/` | 이력서 화면 |
| portfolio | `http://localhost:8083/` | 포트폴리오 화면 |
| Swagger | `http://localhost:8080/swagger-ui/` | API 문서 |

> `localhost:8080/` 403이 뜨면 SecurityConfig `permitAll()`에 static 경로(`/`, `/index.html`, `/assets/**`)가 포함되어 있는지 확인.

## 6. 문제 해결

| 문제 | 원인 | 해결책 |
|------|------|--------|
| DB Connection refused | Docker 미기동 | `docker compose up -d` |
| Access denied sh_local | 기존 MySQL이 3306 점유 | 3307 사용 (이미 설정됨) |
| Port 808x already in use | 이전 프로세스 잔존 | `Get-NetTCPConnection -LocalPort 8080 -State Listen` → 해당 PID 종료 |
| `getSite()` undefined (STS) | Lombok 미처리 | STS를 Java 21로 실행 + lombok.jar javaagent |
| 빌드 시 static 비어있음 | copyFrontendDist 미실행 | `./gradlew :modules:auth:backend:processResources` |
| `/` 403 | SecurityConfig permitAll 누락 | static 경로 permitAll 추가 |

## 7. 부팅이 오래 걸릴 때

- **최초 1회만** 컴파일+의존성 다운로드로 느림 (수 분~수십 분)
- 이후엔 캐시되어 **10~15초** 내 부팅
- 느낌상 "1시간" 걸리는 경우 대부분 **8080 포트를 이전 프로세스가 점유** → 시작이 실패/재시도
  ```bash
  Get-NetTCPConnection -LocalPort 8080 -State Listen   # 점유 확인
  Stop-Process -Id <PID> -Force                        # 종료
  ```

## 8. 참고 자료
- `docs/plans/010-260820-local-dev-docker-design.md` — 로컬 Docker 설계
- `docs/learnings/004-260820-wsl2-docker-windows-learning.md` — WSL2/Docker 학습
- `docker-compose.yml`, `scripts/local-db-init.sql` — DB 구성

---
*작성일: 2026-08-20*