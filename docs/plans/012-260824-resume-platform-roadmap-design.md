# 012-260824-resume-platform-roadmap 설계 문서

## 개요
- **목적**: 단일 이력서 CRUD 수준인 현 resume 모듈을 "이력서 관리 플랫폼"(잡코리아/사람인 수준의 등록관리 + 지원관리)으로 확장하기 위한 기획·로드맵 확정
- **범위**: resume 모듈 전체 + scraper 모듈과의 공고 연동
- **작성일**: 2026-08-24
- **배경**: Phase 3~4(CRUD API + 단일 뷰/편집 UI) 완료 시점에 사용자가 플랫폼 방향성 피드백 — 다중 이력서, 템플릿, 파일첨부(pptx), 공고 연동 지원관리, 사람인식 레이아웃(사진·목차·섹션 순서 변경) 요구

---

## 1. 벤치마킹 정리

### 1.1 국내 포털 (사람인 / 잡코리아)

| 항목 | 사람인 | 잡코리아 | 우리 플랫폼 적용 |
|------|--------|----------|------------------|
| 이력서 수 | **최대 10개**, 대표(공개) 1개 | 자유양식 다본 | 다중 이력서 도입 (Phase 6) |
| 구조 | 필수 항목 + 추가 항목 | 항목 교체 가능(수상경력→다른항목) | 섹션 include/exclude + 순서 편집 |
| 사진 | 10MB, jpg/gif, 100x140px, 크기조절도구 | 동일 수준 | 프로필 사진 업로드 + 리사이즈 (Phase 5) |
| 경력기술서(IT) | - | 회사/주요사업, 근무기간, 부서/직위, **개발규모·환경**, 주요업무·성과 | career/project description 가이드 문구 제공 |
| 지원관리 | 입사지원 이력 (포털 내) | 포털 내 지원 | **공고 연동 지원 트래커** (Phase 7, 차별화 포인트) |

### 1.2 글로벌 빌더 (Novoresume/Canva/WPS류)

- 흐름 표준: **템플릿 선택 → 정보 입력 → PDF 내보내기**
- 채용담당자 평균 7.4초 스캔 → 시각적 계층구조(이름·직책·핵심자격 즉시 노출) 중요
- ATS(지원자추적시스템) 호환: 깔끔한 서식, 표준 섹션 제목, 공고 키워드 반영
- 우리 적용: 템플릿 프리셋(클래식/모던/심플), 인쇄 CSS 템플릿별 분기, (원격) 공고 키워드 매칭 표시

### 1.3 결론 — 차별화 축

> 잡코리아/사람인이 못 하는 것: **내가 수집한 공고와 내 이력서를 한 곳에서 연결**하는 것.
> scraper 모듈(공고 수집)을 보유한 우리만의 결합점 = "공고 ↔ 이력서 ↔ 지원상태" 트래커.

---

## 2. 요구 사항 (신규)

### 2.1 기능 요구 사항

- [ ] FR-101: 포트폴리오 첨부파일 업로드 (pdf/pptx/ppt/docx/png/jpg, 10MB/파일)
- [ ] FR-102: 프로필 사진 업로드 (jpg/png, 리사이즈 200x280 저장, 원본 보존)
- [ ] FR-201: 다중 이력서 문서 (기본 3개, 대표 문서 지정)
- [ ] FR-202: 이력서 템플릿 선택 (v1: 클래식/모던 2종 — 레이아웃·폰트 프리셋)
- [ ] FR-203: 문서별 섹션 편성 (포함/제외 + 순서 변경, 드래그앤드롭)
- [ ] FR-204: 뷰 목차(TOC) 사이드바 — 사람인식 우측 앵커 내비게이션
- [ ] FR-301: 지원 관리 — 공고 수동등록 및 scraper 공고 연결
- [ ] FR-302: 지원 기록 필수정보 — 공고/회사/직무 URL, 지원일, 지원경로(플랫폼/홈페이지 링크), 상태
- [ ] FR-303: 지원 시 사용 이력서 매칭 (문서 참조)
- [ ] FR-304: 상태 파이프라인 — 준비중/지원완료/서류통과/면접/최종합격/불합격/보류
- [ ] FR-305: 지원 목록 뷰 — 상태별 필터 + 타임라인(최근 활동순)

### 2.2 비기능 요구 사항

- 파일 저장: v1 로컬 디스크 `/home/ubuntu/sh-platform/uploads/resume/{userId}/` (설계 011 §2.2 준용)
- 업로드 검증: 확장자 화이트리스트 + Content-Type 확인 + 크기 10MB 제한
- 다운로드: 인증된 본인만 (JWT) — 정적 서빙 금지, 컨트롤러 스트리밍
- 데이터 이행: 기존 항목 데이터는 그대로 유지(마스터 데이터 방식이므로 무손실)

---

## 3. 설계

### 3.1 핵심 아키텍처 결정: 마스터 데이터 + 문서(뷰 정의)

```
[마스터 데이터]                      [문서 = 어떻게 보여줄까]
profile (1행/user)   ─┐              resume_documents
careers              │  모든 문서가   ├─ id, user_id, title
educations           ├── 공유 참조 ──→ ├─ template_code (classic|modern)
skills               │               ├─ is_primary (대표 1개)
certificates         │               ├─ section_config JSON:
projects             │               │   [{"key":"careers","included":true,"order":1}, ...]
introductions        │               └─ created_at, updated_at
portfolio_items      │
```

- **A안(채택)**: 항목 테이블은 `user_id` 직속 유지. 문서는 섹션 편성(참조)만 정의.
  - 장점: 기존 8테이블·API 무변경, 데이터 중복 없음, 잡코리아의 "마스터 이력서 → 지원용 복사" UX와 동일한 정신
  - 단점: 문서별 내용 차등 불가(향후 필요 시 문서-항목 override 테이블로 확장)
- B안(기각): 항목을 document_id 소속으로 → 마이그레이션 비용 + 중복 입력 UX

### 3.2 지원 관리 데이터 모델

```sql
CREATE TABLE IF NOT EXISTS resume_applications (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id         BIGINT NOT NULL,
    -- scraper_platform.job_postings 참조(FK 없음, MSA 경계)
    posting_id      BIGINT NULL,
    -- 공고 스냅샷 (공고 삭제/변경 대비)
    company_name    VARCHAR(100) NOT NULL,
    posting_title   VARCHAR(200) NOT NULL,
    posting_url     VARCHAR(500),
    apply_channel   VARCHAR(20) NOT NULL DEFAULT 'LINK',  -- PLATFORM|LINK|EMAIL
    applied_at      DATE,
    status          VARCHAR(20) NOT NULL DEFAULT 'PREPARING',
    -- PREPARING|APPLIED|SCREEN_PASSED|INTERVIEW|OFFER|REJECTED|ON_HOLD
    document_id     BIGINT NULL,          -- 사용한 이력서 문서
    memo            TEXT,
    created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_resume_applications_user_status (user_id, status),
    INDEX idx_resume_applications_user_date (user_id, applied_at)
);
```

- `apply_channel=PLATFORM`: 향후 플랫폼 내 지원(Phase 원격). `LINK`: 홈페이지 지원 링크 기록.
- `posting_id` 연결 시 scraper 내부 API(`GET /scraper/api/v1/postings/{id}`)로 제목/회사 자동채움, 실패 시 수동 입력 폴백.

### 3.3 파일 업로드 설계

```
POST /api/v1/files            (multipart) → {fileId, url}  — portfolio FILE 타입용
POST /api/v1/profile/photo    (multipart) → 리사이즈 후 저장, profile.photo_url 갱신
GET  /api/v1/files/{id}/download  — JWT 본인 검증 후 스트리밍
```

- 저장 경로: `{base}/{userId}/{yyyyMM}/{uuid}.{ext}` — 유저별 격리, 날짜 버킷
- 허용 확장자: `pdf, pptx, ppt, docx, png, jpg` (사진은 `jpg, png` 한정)
- Spring `MultipartFile` + `Files.copy`, 바이러스스캔은 v2 백로그

### 3.4 프론트 구조 (확장)

```
/resume/
├── #view          문서 뷰 (템플릿 분기 + TOC 사이드바)
├── #edit          편집 (현 CrudSection 유지)
├── #documents     문서 관리 (목록/생성/대표지정/템플릿/섹션편성)
└── #applications  지원 관리 (상태 필터 리스트 + 카운트 요약)
```

- 섹션 순서 변경: HTML5 drag & drop (`draggable` 속성, 라이브러리 없이 시작)
- TOC: IntersectionObserver로 현재 섹션 하이라이트

### 3.5 API 신규 목록

| Method | Path | 설명 |
|--------|------|------|
| GET/POST | `/api/v1/documents` | 문서 목록/생성 |
| GET/PUT/DELETE | `/api/v1/documents/{id}` | 문서 조회/수정(템플릿·섹션편성)/삭제 |
| PUT | `/api/v1/documents/{id}/primary` | 대표 문서 지정 |
| POST | `/api/v1/files` | 파일 업로드 |
| GET | `/api/v1/files/{id}/download` | 인증 다운로드 |
| POST | `/api/v1/profile/photo` | 프로필 사진 업로드 |
| GET/POST | `/api/v1/applications` | 지원 목록(상태필터)/등록 |
| GET/PUT/DELETE | `/api/v1/applications/{id}` | 지원 상세/수정/삭제 |

---

## 4. 구현 계획 (Phase 재편)

| Phase | 내용 | 선행 | 산출물 |
|-------|------|------|--------|
| **5** | 파일 업로드: 포트폴리오 FILE + 프로필 사진 | 완료된 Phase 3~4 | files API, 업로드 UI, DDL v2 |
| **6** | 다중 이력서: documents CRUD + 템플릿 2종 + 섹션 편성(드래그) + TOC | Phase 5 | documents API/UI, DDL v2 |
| **7** | 지원 관리: applications CRUD + scraper 공고 연결 + 상태 파이프라인 UI | Phase 6 | applications API/UI, DDL v2 |
| **8** | 뷰 고도화: 사람인식 레이아웃(사진 배치), 인쇄 CSS 템플릿 분기 | Phase 6 | 템플릿별 뷰 |
| **9** | 공유 링크 (기존 Phase 6이던 것): read-only public token URL | Phase 6 | share API |
| 백로그 | 서버 사이드 PDF 생성, 공고 키워드-이력서 매칭 점수, AI 자소 첨삭, 이메일 알림(common_notification) | - | - |

**DDL v2** (`docs/resume/ddl-resume-v2.sql`): `resume_documents`, `resume_applications` 2테이블 추가 (기존 8+5 테이블 무변경 — 마스터 방식 덕분에 마이그레이션 불필요).

## 5. 리스크 / 열린 질문

- 파일 업로드 용량 정책: OCI 디스크 여유 확인 필요 → 초과 시 S3(OSS) 전환 검토
- scraper↔resume 연동: 서비스 간 호출은 내부 API(JWT service token) vs DB 직접조회 → API 방식 권장(MSA 경계 유지)
- 다국어(영문 이력서)는 백로그 — 데이터 모델에 locale 컬럼 추가 여지만 열어둠

## 6. 참고 자료

- 사람인 이력서 양식 안내: 필수+추가항목 구조, 이력서 10개/공개 1개 제한
- 사람인 사진 기준: 10MB, jpg/gif, 100x140px 권장, 크기조절 도구 제공
- 잡코리아 경력기술서(IT): 개발규모·환경/주요업무·성과 구조
- WPS/Novoresume류: 템플릿→입력→PDF 표준 흐름, 7.4초 스캔·ATS 호환 논점

---
*작성일: 2026-08-24*
