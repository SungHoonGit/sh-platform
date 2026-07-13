# 개발 가이드

## 시작하기

### 1. 저장소 구조

```
sh-platform/
├── sh-platform-auth/          # 인증 모듈 (현재 개발 중)
├── sh-platform-common/        # 공통 라이브러리
├── sh-platform-core/          # 코어 모듈
├── frontend/                  # 프론트엔드 (Vite + React + TS)
└── docs/                      # 프로젝트 문서
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
./gradlew :sh-platform-auth:test  # auth 모듈만 테스트
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

### 5. 필수 문서 체크리스트

새 기능 추가 시 다음 문서가 관련되면 함께 업데이트한다:

| 문서 | 갱신 대상 |
|------|-----------|
| `docs/architecture/standards.md` | 표준 위반 시 |
| `docs/architecture/erd.md` | 새 테이블 추가 시 |
| `docs/architecture/sql-ddl.md` | 새 테이블 추가 시 |
| `docs/auth/api-auth.md` | 새 API 엔드포인트 추가 시 |
| `docs/auth/frontend-auth-guide.md` | 프론트 영향 있는 경우 |
| `docs/development-guide.md` | (본 문서 - 필요 시) |

### 6. 참조 문서 인덱스

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
| 테스트 리포트 & Javadoc 활용 | `docs/guides/test-report-guide.md` |
| Swagger API 문서 | `docs/guides/swagger-guide.md` |
