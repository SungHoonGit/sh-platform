# 011-260821 이력서·포트폴리오 모듈 통합 설계 문서

## 개요

- **목적**: resume/portfolio 2개 모듈을 resume 1개 모듈로 통합하고, 잡코리아/사람인 벤치마킹 기반의 "온라인 이력 관리" 기능을 설계한다. 아울러 트렌드 조사를 반영한 AI/RAG 기반 문서·PPT 생성 확장 로드맵(§6)을 포함한다
- **범위**: 모듈 구조, DB 스키마, API 설계, 인프라 정리, 구현 계획, AI 확장 로드맵
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
| Phase 6+ | AI 확장: 경력 데이터 RAG + 문서/PPT 생성 (§6 참조) | 별도 설계 후 착수 |

## 6. AI 확장 로드맵 (Phase 6+, 트렌드 조사 반영)

> 2026-08-21 트렌드 조사 결과를 반영한 후속 로드맵. Phase 1~5(통합)가 선행 조건.

### 6.1 배경: 2026 트렌드 조사 요약

| 축 | 핵심 발견 |
|----|-----------|
| AI 이력서/경력기술서 | 원티드는 AI가 합격자 이력서와 비교·피드백 + 포지션 추천("서류 합격률 2배"). 잡코리아 조사: 국내 기업 **69.2%가 채용 시 AI 역량 고려**. 경력기술서 트렌드 = 구체성·임팩트·데이터(숫자 증명) |
| RAG 개인 지식베이스 | "Second Brain" 패턴 성숙: 문서/노트 수집 → 벡터DB → 대화형 질의. 개인 커리어 데이터의 지식베이스화 흐름과 일치 |
| AI PPT 생성 | Gamma가 사실상 표준(문서→덱 1분, PPT 내보내기, API 제공). Plus AI(PowerPoint 네이티브), PopAI(다중 모델 GPT-4o/Claude/DeepSeek). 공통 워크플로우: **문서 업로드 → 아웃라인 생성 → 덱 디자인 → PPTX/링크 공유** |

### 6.2 제품 비전

> 사용자가 이력 항목(§3.2의 8개 테이블)을 등록해두면,
> **① 스크래퍼가 수집한 JD와 매칭하여 맞춤 경력기술서 생성 → ② 면접/발표용 PPT 자료 자동 생성**

차별화 포인트:
- 기존 AI 이력서 도구는 "빈 양식에서 AI 작성" — 우리는 **구조화된 내 이력 데이터 + 실시간 JD 데이터**를 재료로 사용
- §3.2 데이터 모델이 곧 RAG 지식베이스의 원천 (별도 수집 파이프라인 불필요)

### 6.3 아키텍처 옵션

| 옵션 | 방식 | 장점 | 단점 |
|------|------|------|------|
| A | Gamma API 연동 | 최고 품질 덱, 빠른 구현 | API 비용, 외부 종속, 한국어 디자인 한계 |
| **B (권장)** | LLM이 슬라이드 구조(JSON) 생성 → Apache POI(PPTX) 또는 reveal.js(HTML) 렌더링 | 제어권·무종속, 비용=LLM 토큰만, 스크래퍼 모듈과 동일 아키텍처 패턴 | 렌더링 품질 직접 책임 |
| C | 하이브리드 (LLM 콘텐츠 + 외부 렌더링) | 품질과 통제의 절충 | 두 의존성 관리 |

B안 권장 사유: 이미 DeepSeek 등 LLM 연동 경험 보유, OCI 프리티어 예산 제약, PPTX 템플릿 기반 렌더링은 Apache POI로 검증된 패턴.

### 6.4 규제 고려사항

- **한국 AI 기본법 제31조**: 생성형 AI 결과물은 AI 생성임을 명시해야 함 → 생성 문서/PPT에 "AI 생성 초안" 워터마크/표기 필수
- EU AI Act는 고용 분야 AI를 고위험군으로 분류 — 해외 진출 시 투명성 의무 검토
- 개인 커리어 데이터를 LLM에 전송하는 행위 자체에 대한 동의 고지 필요 (개인정보 처리방침 갱신)

### 6.5 단계별 계획 (예시)

| 단계 | 내용 | 선행 조건 |
|------|------|-----------|
| AI-1 | 경력 데이터 → LLM 프롬프트 조립, 경력기술서 텍스트 생성 (JD 매칭 없이) | Phase 3 완료 |
| AI-2 | 스크래퍼 JD 데이터 결합: 공고 요구역량 vs 내 스킬 갭 분석 + 맞춤 강조점 생성 | AI-1 + scraper API |
| AI-3 | 슬라이드 JSON 스키마 정의 + PPTX 렌더러(Apache POI) 프로토타입 | AI-1 |
| AI-4 | 대화형 질의(RAG): "내 경력 중 클라우드 경험 요약해줘" 스타일 질의응답 | AI-1, 벡터DB 도입 검토 |

## 7. 참고 자료

- 잡코리아 온라인 이력서: 항목 기반 구성 (인적사항/경력/학력/스킬/자기소개/첨부파일)
- 사람인 온라인 이력서: 동일 구조 + 포트폴리오 링크 영역
- 기존 문서: `docs/architecture/erd.md`, `docs/architecture/db-design-standard.md`
- 트렌드 조사 (2026-08-21):
  - 원티드 AI 이력서 (합격자 비교 분석, 포지션 추천): wanted.co.kr/cv/intro
  - 잡코리아 "채용 시 AI 역량 고려 69.2%" 조사 (대한상공회의소 인용)
  - Gamma 등 AI 프레젠테이션 도구 비교 (gamma.com.ai, Plus AI, PopAI, Beautiful.ai)
  - RAG 개인 지식베이스 "Second Brain" 패턴
  - 한국 AI 기본법 제31조 (생성형 AI 표기 의무), EU AI Act 고용 분야 고위험 분류

---
*작성일: 2026-08-21*
