# 010-260820-local-dev-docker-design

## 개요
- **목적**: 로컬 개발 환경에서 모든 백엔드 모듈(auth/scraper/resume/portfolio)을 실행할 수 있도록 **Docker 기반 로컬 DB**를 구성한다
- **범위**: 루트 docker-compose + 각 모듈 `application-local.yml` + 실행 가이드
- **작성일**: 2026-08-20
- **작성자**: AI Assistant

## 1. 배경 및 이유

### 1.1 현재 문제
모든 백엔드 모듈의 `application.yml`이 **프로덕션 내부망 DB**(`jdbc:mariadb://10.0.0.39:3306`)를 기본값으로 하드코딩하고 있다.

- 로컬에서 `./gradlew :modules:scraper:backend:bootRun` 실행 시
  `Socket fail to connect to address=(host=10.0.0.39)(port=3306). Connection timed out` 발생
- 내부망 IP(10.0.0.39)는 로컬에서 접근 불가 (OCI 내부 네트워크 전용)
- auth 모듈만 `application-local.yml`(localhost:3307)이 존재하고, scraper/resume/portfolio는 로컬 설정이 전무

### 1.2 왜 Docker MariaDB인가
- **프로덕션과 동일한 DB 엔진**(MariaDB 10.11)을 로컬에서 사용 → `ddl-auto: validate`도 그대로 통과
- H2 인메모리 대비 SQL 방언 차이(파티션, 인덱스, 함수)로 인한 불일치 위험 없음
- MSA 모듈 전부 하나의 로컬 DB 컨테이너로 커버 가능
- Docker Desktop만 있으면 OS 무관(Windows/macOS/Linux) 동일 실행

## 2. 요구 사항

### 2.1 기능 요구 사항
- [ ] FR-001: 루트에 `docker-compose.yml`로 로컬 MariaDB 10.11 컨테이너 기동
- [ ] FR-002: auth/scraper/resume/portfolio 4개 DB 스키마 자동 생성 (컨테이너 초기화 스크립트)
- [ ] FR-003: 각 모듈에 `application-local.yml` 추가 → `localhost:3306` 접속 + `ddl-auto: update`
- [ ] FR-004: 로컬 실행 시 `--spring.profiles.active=local`로 시작하면 prod DB에 절대 접속하지 않음
- [ ] FR-005: 프로덕션 배포 환경(`application-prod.yml`)과 완전 분리 (기본값은 prod 유지)

### 2.2 비기능 요구 사항
- **보안**: 로컬 DB 자격증명은 프로덕션과 별도 (비밀번호 분리, `.env` 미사용)
- **성능**: 컨테이너 1개로 4개 DB 스키마 서비스 (포트 3306)
- **유지보수**: `docker compose down`으로 전체 초기화 가능, 데이터는 named volume으로 유지

## 3. 설계

### 3.1 아키텍처

```
[로컬 PC]                                [Docker Desktop]
┌────────────────────┐                  ┌─────────────────────────────┐
│ auth  (8080)       │── jdbc ──┐       │ MariaDB:10.11               │
│ scraper(8081)      │── jdbc ──┼──────▶│  port 3306                  │
│ resume (8082)      │── jdbc ──┤       │  DB 4개:                     │
│ portfolio(8083)    │── jdbc ──┘       │  ├ sh_pass                  │
│  (bootRun -P local)│                   │  ├ scraper_platform         │
└────────────────────┘                   │  ├ resume_platform          │
                                         │  └ portfolio_platform       │
                                         │  volume: mariadb-data        │
                                         └─────────────────────────────┘
```

### 3.2 docker-compose.yml (루트)

```yaml
services:
  mariadb:
    image: mariadb:10.11
    container_name: sh-local-mariadb
    ports:
      - "3306:3306"
    environment:
      MARIADB_ROOT_PASSWORD: root
      MARIADB_DATABASE: sh_pass
      MARIADB_USER: sh_local
      MARIADB_PASSWORD: sh_local_pass
    volumes:
      - mariadb-data:/var/lib/mysql
      - ./scripts/local-db-init.sql:/docker-entrypoint-initdb.d/init.sql:ro
    healthcheck:
      test: ["CMD", "healthcheck.sh", "--connect", "--innodb_initialized"]
      interval: 5s
      timeout: 5s
      retries: 10

volumes:
  mariadb-data:
```

### 3.3 local-db-init.sql (scripts/)

컨테이너 최초 기동 시 자동 실행되는 DB 초기화 스크립트.

```sql
-- scraper
CREATE DATABASE IF NOT EXISTS scraper_platform
  CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
-- resume
CREATE DATABASE IF NOT EXISTS resume_platform
  CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
-- portfolio
CREATE DATABASE IF NOT EXISTS portfolio_platform
  CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- 로컬 사용자에 전체 DB 권한
GRANT ALL PRIVILEGES ON *.* TO 'sh_local'@'%';
FLUSH PRIVILEGES;
```

> `MARIADB_DATABASE: sh_pass`는 auth용. 나머지 3개는 init.sql로 생성.
> 스키마 테이블은 `ddl-auto: update`가 자동 생성하므로 DDL 파일 불필요.

### 3.4 모듈별 application-local.yml

**auth** (기존 `application-local.yml` 존재 → 포트만 3306으로 정리):
```yaml
spring:
  datasource:
    url: jdbc:mariadb://localhost:3306/sh_pass
    driver-class-name: org.mariadb.jdbc.Driver
    username: sh_local
    password: sh_local_pass
  jpa:
    hibernate:
      ddl-auto: update
```

**scraper / resume / portfolio** (신규 추가, 동일 패턴):
```yaml
spring:
  datasource:
    url: jdbc:mariadb://localhost:3306/{db명}
    driver-class-name: org.mariadb.jdbc.Driver
    username: sh_local
    password: sh_local_pass
  jpa:
    hibernate:
      ddl-auto: update
```

| 모듈 | application-local.yml DB명 | 기본 port |
|------|---------------------------|-----------|
| auth | `sh_pass` | 8080 |
| scraper | `scraper_platform` | 8081 |
| resume | `resume_platform` | 8082 |
| portfolio | `portfolio_platform` | 8083 |

### 3.5 프로필 활성화 전략

- **기본값(application.yml)**: prod DB(10.0.0.39) 유지 → 배포 환경 영향 없음
- **로컬 실행**: `--spring.profiles.active=local` 명시 → `application-local.yml` 적용
  ```bash
  ./gradlew :modules:scraper:backend:bootRun --args='--spring.profiles.active=local'
  ```
- auth는 이미 `spring.profiles.active: ${SPRING_PROFILES_ACTIVE:local}`로 기본 local → **주의**: 서버 배포 시 `SPRING_PROFILES_ACTIVE=prod` 환경변수 필수

### 3.6 예상 파일 목록

```
sh-platform/
├── docker-compose.yml                        # (신규) 로컬 MariaDB
├── scripts/
│   └── local-db-init.sql                     # (신규) 로컬 DB 4개 생성
├── modules/
│   ├── auth/backend/src/main/resources/
│   │   └── application-local.yml             # (수정) DB만 localhost:3306으로
│   ├── scraper/backend/src/main/resources/
│   │   └── application-local.yml             # (신규)
│   ├── resume/backend/src/main/resources/
│   │   └── application-local.yml             # (신규)
│   └── portfolio/backend/src/main/resources/
│       └── application-local.yml             # (신규)
└── docs/plans/
    └── 010-260820-local-dev-docker-design.md # 본 문서
```

## 4. 구현 계획

| 단계 | 내용 | 예상 시간 |
|------|------|-----------|
| Phase 1 | docker-compose.yml + local-db-init.sql 작성 | 10분 |
| Phase 2 | 각 모듈 application-local.yml 추가 (4개) | 15분 |
| Phase 3 | docker compose up + DB 초기화 검증 | 10분 |
| Phase 4 | auth/scraper 부팅 검증 (`bootRun -P local`) | 15분 |
| Phase 5 | resume/portfolio 부팅 검증 | 10분 |

## 5. 참고 자료

- [MariaDB 공식 이미지](https://hub.docker.com/_/mariadb) — 10.11 LTS
- AGENTS.md "DB: MariaDB 10.11.14 (prod), H2 (local)" — 본 설계는 H2 대신 Docker MariaDB 사용
- 기존 `modules/auth/backend/.../application-local.yml` — localhost:3307 참조 패턴

---
*작성일: 2026-08-20*