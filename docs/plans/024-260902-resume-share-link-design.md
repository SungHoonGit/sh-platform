# 024-260902-resume-share-link 설계 문서

## 개요
- **목적**: resume 로드맵(012)의 마지막 미구현 Phase 9인 **공유 링크**를 설계한다. 로그인 없이 이력서를 읽기 전용으로 열람할 수 있는 public token URL을 제공한다.
- **범위**: resume 모듈만. (프론트 공유 화면 + 백엔드 공유 링크 API)
- **작성일**: 2026-09-02
- **선행**: Phase 6 (다중 문서·템플릿·섹션 편성), Phase 8 (뷰/PDF 고도화) 완료

---

## 1. 배경 및 이유- 채용 공고에 이력서를 첨부하거나 지인에게 검토를 받을 때 **로그인 요구 없이 보여주는** URL이 필요하다.
- 기존 `GET /api/v1/view`, `GET /api/v1/view/pdf`는 JWT 인증 필수 → 공유 불가.
- 잡코리아/사람인도 "비공개" 이력서 화면 링크가 있으며, read-only + 만료 설정이 표준이다.

> **구현 상태(2026-09-02)**: Phase 9 백엔드 + 프론트 구현 완료, 로컬 테스트(11건) 통과, 커밋 대기. 상세 구현 사항은 `docs/daily/2026-09-02-work-log.md` 참고.



## 2. 요구 사항

### 2.1 기능 요구 사항
- [ ] FR-901: 소유자가 문서(document) 단위로 공유 링크를 생성한다 (문서 미지정 시 대표 문서)
- [ ] FR-902: 공유 링크는 랜덤 토큰 기반 read-only URL이며 로그인 없이 이력서 뷰/PDF 열람 가능
- [ ] FR-903: 소유자는 링크를 해제할 수 있고, 해제 즉시 접근 불가
- [ ] FR-904: 만료일 선택 가능 (없으면 무기한)
- [ ] FR-905: 공유 화면은 테마(templateCode)·섹션 편성(section_config)을 반영한 표시
- [ ] FR-906: 공유 화면에 소유자 정보(이름)와 열람 타이틀 노출

### 2.2 비기능 요구 사항
- 보안: 토큰은 하드추측 불가(UUID v4 122bit) → URL을 아는 사람만 접근, 인덱싱/검색 대상 제외(`X-Robots-Tag: noindex`)
- 성능: 공유 조회도 기존 뷰 조립을 재사용 (중복 구현 금지)
- 다운로드: PDF는 공유 토큰으로도 가능 (read-only, 정적 서빙 금지 — 컨트롤러 스트리밍 유지)

## 3. 설계

### 3.2 데이터 모델 (구현)

```sql
CREATE TABLE IF NOT EXISTS resume_share_links (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    document_id   BIGINT NOT NULL,
    token         VARCHAR(64) NOT NULL,
    expires_at    DATETIME NULL,
    created_at    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uq_resume_share_links_token (token),
    UNIQUE KEY uq_resume_share_links_document (document_id),
    INDEX idx_resume_share_links_document (document_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

```sql
CREATE TABLE IF NOT EXISTS resume_share_links (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    document_id   BIGINT NOT NULL,             -- resume_documents.id
    token         VARCHAR(64) NOT NULL UNIQUE, -- UUID v4 하이픈 제거
    expires_at    DATETIME NULL,               -- NULL = 무기한
    created_at    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_resume_share_links_document (document_id)
);
```

- 문서당 공유 링크는 **0 또는 1개** (유니크를 document_id로 두고 upsert 방식으로 단순화)
- `resume_documents.user_id`로 소유자 판별 (share 테이블에 user_id 중복 저장하지 않음)

### 3.2 API 설계

| Method | Path | 인증 | 설명 |
|--------|------|------|------|
| POST | `/api/v1/documents/{id}/share` | JWT(소유자) | 공유 링크 생성/재발급 (만료일 선택) |
| GET | `/api/v1/documents/{id}/share` | JWT(소유자) | 현재 공유 링크 조회 (없으면 null) |
| DELETE | `/api/v1/documents/{id}/share` | JWT(소유자) | 공유 해제 |
| GET | `/share/{token}` | **없음(public)** | 공유 이력서 뷰 (HTML/JSON) |
| GET | `/share/{token}/pdf` | **없음(public)** | 공유 이력서 PDF 다운로드 |

### 3.3 보안 처리

- `GET /share/{token}(/pdf)`를 **security whitelist**에 추가 (`/share/*`).
- 토큰 검증: 존재 + (만료일이 있으면) 만료일 초과 시 404 (존재 여부 무관하게 동일 응답 — 열거 공격 완화)
- 공개 조회는 **문서별 어셈블리**: `ResumeViewService`에 documentId 기반 조립 메서드를 추가해 재사용.
  - 뷰: `ResumeViewService.getDocumentView(documentId)` (섹션 편성 반영)
  - PDF: `ResumePdfService.generatePdf(userId, documentId)`는 userId 필요 → share 조회에서 소유자 userId를 토큰으로 찾아 호출
- 응답 헤더 `X-Robots-Tag: noindex` — 검색 엔진 색인 차단.

### 3.4 프론트

```
/resume/#/s/{token}        → 공유 전용 뷰 (ShareViewPage: AppShell 없이 public, 편집/로그인 불필요)
/resume/#/resumes          → 문서 카드에 "공유 링크 만들기" 토글 + 링크 복사/해제 버튼 (ResumesPage 확장)
```

- 공유 페이지는 백엔드 `GET /resume/share/{token}` 응답의 document(테마·섹션 편성) + view를 받아 기존 템플릿 컴포넌트로 렌더.
- PDF 버튼은 `GET /resume/share/{token}/pdf`로 다운로드 (Content-Disposition 서버측 파일명).
- 공개 전용 API 헬퍼: `client.ts`의 `apiGetShare`/`apiDownloadShare` (인증 헤더 없이 `/resume/share` 진입).

## 4. 구현 계획

| 단계 | 내용 | 산출물 |
|------|------|--------|
| 1 | DDL v8: `resume_share_links` 테이블 + 배포 워크플로우 DDL 라인 | `docs/resume/ddl-resume-v8.sql` ✅ |
| 2 | Entity/Repository + support | `ResumeShareLinkEntity`, `ResumeShareLinkRepository` ✅ |
| 3 | 도메인: `ResumeShareService` (create/get/revoke/resolve/getPublicView) + 소유자 검증 | `domain/ResumeShareService(+Impl)` ✅ |
| 4 | 공개 뷰 조립: 기존 `ResumeViewService.getMyResumeView` + 문서 메타 재사용 (별도 조립 메서드 추가 없이 서비스 조합) | `ResumeShareServiceImpl.getPublicView` ✅ |
| 5 | API: 공유 관리 3종(`POST/GET/DELETE documents/{id}/share`) | `DocumentsController` 확장 + DTO ✅ |
| 6 | 공개: `ShareController` (`GET /share/{token}`, `GET /share/{token}/pdf`) + whitelist + noindex | `api/ShareController` + security 설정 ✅ |
| 7 | 테스트: 생성/재발급/조회/해제/만료/타소유자 NOT_FOUND/공개 조회·PDF | `ResumeShareServiceImplTest` (11건) ✅ |
| 8 | 프론트: 공유 토글+링크 복사 (ResumesPage), 공유 뷰 (`ShareViewPage`) | `ResumesPage.tsx`, `ShareViewPage.tsx` ✅ |

## 5. 참고 자료

- 로드맵: `docs/plans/012-260824-resume-platform-roadmap-design.md` §4 (Phase 9)
- 기존 뷰: `ResumeViewService`, `ResumeViewController`
- 기존 문서: `DocumentsController`/`ResumeDocumentService`
- PDF 재사용: `ResumePdfService.generatePdf(userId, documentId)`

---
*작성일: 2026-09-02*