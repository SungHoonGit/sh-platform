# 011-260821 이력서·포트폴리오 모듈 통합 설계 문서

## 개요

- **목적**: resume/portfolio 2개 모듈을 resume 1개 모듈로 통합하고, 잡코리아/사람인 벤치마킹 기반의 "온라인 이력 관리" 기능을 설계한다
- **범위**: 모듈 구조, DB 스키마, API 설계, 인프라 정리, 구현 계획
- **작성일**: 2026-08-21
- **작성자**: AI Assistant / 사용자

---

## 1. 배경 및 이유

### 1.1 기존 분리 구조의 문제

- resume(8082) / portfolio(8083) 2개 모듈로 분리되어 있으나 **분리 근거가 문서에 존재하지 않음**
  - `platform-architecture-design.md`: auth/user/tenant/monitoring만 설계, 이력서/포트폴리오 언급 없음
  - `monorepo-restructuring.md`: 초기에 각각 만들어진 디렉토리를 옮긴 것뿐
  - 그 외 문서는 전부 인프라 설정/장애 기록
- 두 도메인은 **같은 사용자, 같은 데이터 흐름, 같은 화면**에서 사용 → MSA 분리 기준(독립 확장/독립 팀/독립 배포)에 해당하지 않음
- OCI 프리티어(RAM 6GB)에서 서비스 1개 감소 = 이력서 모듈 가용 메모리 증가 + 운영 오버헤드 감소 (systemd/nginx/DB/deploy 워크플로우 각 1개씩 제거)

### 1.2 사용자 니즈 (제품 관점)

- **현행 pain point**: 이력서를 iCloud Drive에서 AI로 수정 관리 중 → 외출 시 동기화 불안정, 어디서든 열람/수정 불가
- **목표**: 항목 기반으로 내 이력을 한 번 등록해두면 **언제 어디서든 웹으로 열람/수정 가능 + PDF 출력**
- **벤치마킹**: 잡코리아/사람인 — 포트폴리오는 별도 서비스가 아니라 "온라인 이력서"의 하위 구성요소(첨부/작업물)

### 1.3 결정

> **portfolio 모듈(8083)을 폐지하고 resume 모듈(8082)로 통합한다.**
> 포트폴리오는 resume 모듈 내 하위 도메인(작업물/첨부)이 된다.

---

## 2. 요구 사항

### 2.1 기능 요구 사항

- [ ] FR-001: 이력 기본 정보(인적사항) 등록/수정/조회
- [ ] FR-002: 경력 학력 스킬 자격증 프로젝트 자기소개 항목별 CRUD
- [ ] FR-003: 포트폴리오 작업물(파일 링크) 등록/관리
- [ ] FR-004: 등록된 전체 이력을 조립한 이력서 뷰 화면 제공
- [ ] FR-005: 이력서 PDF 출력 (v1: 브라우저 인쇄 CSS, v2: 서버 사이드 생성)
- [ ] FR-006: 본인 소유 데이터만 접근 가능 (JWT 기반 소유권 검증)

### 2.2 비기능 요구 사항

- 보안: JWT 검증(auth 모듈 공개키), 소유자 외 접근 차단
- 성능: 항목 CRUD는 단순 쿼리, 목록은 user_id 인덱스
- 파일 저장: v1은 로컬 디스크(`/home/ubuntu/sh-platform/uploads/resume/`), 용량 제한 10MB/파일

---

## 3. 설계

### 3.1 모듈 구조 (통합 후)

```
modules/resume/
├── backend/                      # Spring Boot (8082 유지)
│   └── com.shplatform.resume    # 패키지 교정 (기존 com.resume.platform)
│       ├── api/
│       │   ├── ResumeProfileController      # 인적사항
│       │   ├── CareerController             # 경력
│       │   ├── EducationController          # 학력
│       │   ├── SkillController              # 스킬
│       │   ├── CertificateController        # 자격증
│       │   ├── ProjectController            # 프로젝트
│       │   ├── IntroductionController       # 자기소개
│       │   ├── PortfolioItemController      # 포트폴리오 작업물
│       │   └── dto/
│       ├── domain/
│       │   ├── {Domain}Service.java         # 인터페이스
│       │   └── {Domain}ServiceImpl.java
│       └── infrastructure/
│           ├── {Domain}Entity.java
│           └── {Domain}Repository.java
└── frontend/                     # React SPA (백엔드가 서빙)
```

- URL 프리픽스: `/resume/*` (8082, nginx 라우팅 기존 유지)
- 폐지 대상: `modules/portfolio/backend` 전체, 포트 8083, `portfolio_platform` DB

### 3.2 데이터 모델

모든 테이블은 `user_id` FK 기반 (users.id 참조, ON DELETE CASCADE).

```sql
-- 인적사항 (user당 1행)
CREATE TABLE resume_profiles (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id         BIGINT NOT NULL UNIQUE,
    name            VARCHAR(50),
    email           VARCHAR(100),
    phone           VARCHAR(30),
    address         VARCHAR(200),
    birth_date      DATE,
    photo_url       VARCHAR(300),
    headline        VARCHAR(100),               -- 한 줄 소개 (예: "3년차 백엔드 개발자")
    created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- 경력
CREATE TABLE resume_careers (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id         BIGINT NOT NULL,
    company         VARCHAR(100) NOT NULL,
    title           VARCHAR(100),               -- 직무/직급
    start_date      DATE,
    end_date        DATE,                       -- NULL = 재직중
    is_current      BOOLEAN NOT NULL DEFAULT FALSE,
    description     TEXT,
    display_order   INT NOT NULL DEFAULT 0,
    created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_resume_careers_user (user_id),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- 학력
CREATE TABLE resume_educations (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id         BIGINT NOT NULL,
    school          VARCHAR(100) NOT NULL,
    major           VARCHAR(100),
    degree          VARCHAR(20),                -- HIGH_SCHOOL/ASSOCIATE/BACHELOR/MASTER/DOCTOR
    start_date      DATE,
    end_date        DATE,
    status          VARCHAR(20),                -- ENROLLED/GRADUATED/LEAVE
    display_order   INT NOT NULL DEFAULT 0,
    created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_resume_educations_user (user_id),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- 스킬
CREATE TABLE resume_skills (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id         BIGINT NOT NULL,
    name            VARCHAR(50) NOT NULL,       -- 예: Java, Spring Boot
    level           VARCHAR(20),                -- BEGINNER/INTERMEDIATE/ADVANCED/EXPERT
    category        VARCHAR(50),                -- LANGUAGE/FRAMEWORK/DB/TOOL 등
    display_order   INT NOT NULL DEFAULT 0,
    created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_resume_skills_user (user_id),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- 자격증
CREATE TABLE resume_certificates (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id         BIGINT NOT NULL,
    name            VARCHAR(100) NOT NULL,
    issuer          VARCHAR(100),               -- 발행기관
    acquired_at     DATE,
    display_order   INT NOT NULL DEFAULT 0,
    created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_resume_certificates_user (user_id),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- 프로젝트/경험
CREATE TABLE resume_projects (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id         BIGINT NOT NULL,
    name            VARCHAR(100) NOT NULL,
    role            VARCHAR(100),               -- 담당 역할
    start_date      DATE,
    end_date        DATE,
    description     TEXT,
    tech_stack      VARCHAR(300),               -- 콤마 구분 또는 JSON
    link_url        VARCHAR(300),
    display_order   INT NOT NULL DEFAULT 0,
    created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_resume_projects_user (user_id),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- 자기소개 (항목별 다수)
CREATE TABLE resume_introductions (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id         BIGINT NOT NULL,
    title           VARCHAR(100) NOT NULL,      -- 예: "지원동기", "성장과정"
    content         TEXT NOT NULL,
    display_order   INT NOT NULL DEFAULT 0,
    created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_resume_introductions_user (user_id),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- 포트폴리오 작업물 (파일/링크) ← 기존 portfolio 모듈 흡수
CREATE TABLE resume_portfolio_items (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id         BIGINT NOT NULL,
    title           VARCHAR(100) NOT NULL,
    item_type       VARCHAR(20) NOT NULL,       -- FILE/LINK
    file_path       VARCHAR(300),               -- FILE 타입: 저장 경로
    link_url        VARCHAR(300),               -- LINK 타입: 외부 URL
    description     VARCHAR(500),
    display_order   INT NOT NULL DEFAULT 0,
    created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_resume_portfolio_user (user_id),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);
```

### 3.3 API 설계

공통 규칙:
- 인증: Bearer JWT 필수, userId는 토큰에서 추출 (요청 파라미터로 받지 않음)
- 응답: `ApiResponse<T>` 공통 포맷
- 정렬: 목록은 `display_order ASC, id ASC`

| Method | Path | 설명 |
|--------|------|------|
| GET | `/resume/api/v1/profile` | 내 인적사항 조회 |
| PUT | `/resume/api/v1/profile` | 인적사항 등록/수정 (upsert) |
| GET | `/resume/api/v1/careers` | 경력 목록 |
| POST | `/resume/api/v1/careers` | 경력 추가 |
| PUT | `/resume/api/v1/careers/{id}` | 경력 수정 |
| DELETE | `/resume/api/v1/careers/{id}` | 경력 삭제 |
| ... | `educations` `skills` `certificates` `projects` `introductions` | 동일 패턴 |
| GET | `/resume/api/v1/portfolio-items` | 작업물 목록 |
| POST | `/resume/api/v1/portfolio-items` | 작업물 추가 (multipart 또는 JSON 링크) |
| DELETE | `/resume/api/v1/portfolio-items/{id}` | 작업물 삭제 |
| GET | `/resume/api/v1/view` | 전체 이력 조립 조회 (이력서 뷰용) |

### 3.4 보안

- SecurityConfig(JWT 검증 필터) 적용 — common의 JwtTokenValidator 활용 (scraper와 동일 패턴)
- 모든 엔드포인트 본인 데이터만 접근: Service 계층에서 `entity.userId == tokenUserId` 검증
- 파일 업로드: 확장자 화이트리스트(pdf, png, jpg, zip 등), 10MB 제한

---

## 4. 구현 계획

| 단계 | 내용 | 비고 |
|------|------|------|
| Phase 1 | 인프라 정리: portfolio 모듈/서비스/DB/nginx/deploy 워크플로우 제거, AGENTS.md 갱신 | 서버 작업 포함 |
| Phase 2 | 스켈레톤 교정: 패키지 `com.shplatform.resume`, JWT 보안 설정, HealthController | scraper 패턴 복제 |
| Phase 3 | DDL + 항목 CRUD 백엔드 (profile → careers → 나머지) | Javadoc + 단위 테스트 필수 |
| Phase 4 | 이력서 뷰 (`/view` 조립 API) + React SPA (잡코리아식 이력서 화면 + 인쇄 CSS) | |
| Phase 5 | 포트폴리오 파일 업로드 + PDF 출력 고도화 (서버 사이드) | v2 범위 |

## 5. 참고 자료

- 잡코리아 온라인 이력서: 항목 기반 구성 (인적사항/경력/학력/스킬/자기소개/첨부파일)
- 사람인 온라인 이력서: 동일 구조 + 포트폴리오 링크 영역
- 기존 문서: `docs/architecture/erd.md`, `docs/architecture/db-design-standard.md`

---
*작성일: 2026-08-21*
