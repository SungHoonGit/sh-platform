# 023-260831 이력서 서버사이드 PDF 생성 설계 (OpenPDF)

## 개요
- **목적**: 이력서의 브라우저 인쇄 CSS(v1)에 더해 **서버사이드 PDF 파일 생성**(v2) 기능을 추가한다.
  버튼 클릭 시 이력서 전체가 담긴 `이력서.pdf`가 즉시 다운로드되어 외부 제출에 사용 가능하게 한다.
- **범위**: resume 백엔드(8082) — PDF 렌더 서비스 + 다운로드 API / resume 프론트 — "PDF 다운로드" 버튼
- **작성일**: 2026-08-31
- **배경 문서**: `docs/plans/011-260821-resume-portfolio-integration-design.md` (Phase 5 v2)

## 1. 배경 및 이유
- 현재 PDF 출력 = 브라우저 인쇄 대화상자(`window.print()`) → 사용자가 "PDF로 저장"을 수동으로 조작해야 함.
  모든 기기(특히 모바일)에서 자연스럽지 않고, 파일 이름도 임의로 지정해야 함.
- 서버에서 직접 생성하면 **버튼 클릭 → 파일 다운로드**로 끝나고, 어떤 기기에서든 동일한 결과물을 보장한다.

### 방식 결정 (2026-08-31 사용자 확정)
| 방식 | 선정 | 사유 |
|------|------|------|
| **OpenPDF (순수 Java)** | ✅ | 서버 변경 전무, ARM64 무관, JUnit 테스트 가능, 결정적·무외부종속 |
| Headless Chromium | ❌ | 서버에 브라우저 설치(~300MB) + RAM 부담 + 첫 호출 지연 (프리티어 RAM 6GB) |
| jsPDF 클라이언트 | ❌ | "서버사이드" 요구와 부합하지 않음 |

> 서버 화면 템플릿(Modern/Saramin/Classic)과 100% 동일하지는 않지만,
> **제출용 표준 인쇄 서식**으로 깔끔하게 타입셋팅한다. 화면 동일성이 반드시 필요하면 추후 headless 브라우저 옵션을 재검토.

## 2. 요구 사항
### 2.1 기능 요구
- [x] FR-001: `GET /resume/api/v1/view/pdf` — 현재 사용자 기본 이력서(`/view`와 동일 데이터) PDF 바이트 반환
- [x] FR-002: 응답 `Content-Type: application/pdf`, `Content-Disposition: attachment` (파일명 `이력서.pdf`, RFC 5987 UTF-8)
- [x] FR-003: 한글 렌더 — **OFL 라이선스 한글 폰트 임베드** (Spoqa Han Sans Regular/Bold, classpath `fonts/`)
- [x] FR-004: 섹션 렌더 — **문서 sectionConfig의 포함 여부·순서 반영** (`?documentId=`, 미지정 시 기본 편성)
- [x] FR-005: 항목별 날짜 구간 우측 정렬, 기간 없음(null) 안전 처리
- [x] FR-006: 프로필 미등록·항목 전무여도 유효한 PDF 생성 (NPE 없음)
- [x] FR-007 (프론트): ResumeViewPage "PDF 다운로드" 버튼 → `apiDownload`로 즉시 파일 다운로드
- [x] FR-008: **프로필 사진 포함** — 헤더 우측 24mm 정사각, 파일 없음/손상 시 자동 생략
- [x] FR-009 (프론트): "PDF로 인쇄"(window.print) 버튼 제거 — 서버 PDF로 대체
- [x] FR-010 (파일명): Spring ContentDisposition 대신 **RFC 5987 + ASCII 폴백** 직접 헤더 생성
  (기존 `=?UTF-8?Q?=?=` RFC 2047 부호문 깨짐 해결, `ResumeFileController` 다운로드에도 적용)

### 2.2 비기능 요구
- 성능: 생성 1회 ~50ms 내 (스레드 안전 — 요청마다 인스턴스 생성), 결정적 출력
- 보안: 소유자 JWT 필수, 기존 `/view` 보안 정책 그대로
- 파일 크기: 폰트 2개 ≈ 2.3MB jar 포함 (repo 커밋 허용 수준)
- DB/DDL/인프라 변경 없음

## 3. 설계
### 3.1 아키텍처
```
ResumeViewPage(버튼) → GET /api/v1/view/pdf (JWT)
   → ResumeViewController      (@GetMapping "/pdf", byte[] 응답)
   → ResumePdfService.generatePdf(userId)
       └─ ResumeViewService.getMyResumeView(userId)  ← 기존 조립 재사용
       └─ OLED 랜더: Document(A4) + Font(Spoqa Han Sans) + Paragraph/Table
   → ResponseEntity<byte[]> (application/pdf, attachment)
```

### 3.2 데이터 모델
- 변경 없음. `ResumeViewResponse`(기존)의 8개 항목을 렌더에 사용.
- degree/status/level/GPA는 저장된 한글 문자열 그대로 표기 (프론트와 동일 데이터).

### 3.3 API 설계
| Method | Path | 응답 | 설명 |
|--------|------|------|------|
| GET | `/resume/api/v1/view/pdf` | `application/pdf` | 내 기본 이력서 PDF (Content-Disposition: attachment; filename=resume.pdf; filename*=UTF-8''%EC%9D%B4%EB%A0%A5%EC%84%9C.pdf) |
| GET | `/resume/api/v1/view/pdf?documentId={id}` | `application/pdf` | 해당 문서의 섹션 편성(포함·순서) 반영 |

- 오류: 401 UNAUTHORIZED(기존 필터), 500 등 — 공통 핸들러 그대로
- `ApiResponse` 래핑 없이 `ResponseEntity<byte[]>` (바이너리)
- 파일명 헤더는 `ContentDispositionSupport.attachment(filename)` 유틸 사용: RFC 5987 `filename*` + 비ASCII를 밑줄로 치환한 ASCII `filename` 폴백 (RFC 2047 부호문 사용 금지)

### 3.4 렌더 명세 (feat: PDF 레이아웃)
- 페이지: A4(595×842), 좌우 18mm / 상하 16mm
- 섹션 순서: **문서 sectionConfig(포함·order) 반영**. 미지정 시 기본 편성 —
  경력 → 프로젝트 → 학력 → 스킬 → 자격증 → 자기소개 → 포트폴리오 (빈 섹션 생략)
- 헤더: 이름(26pt Bold) + headline(11pt 회색) / 연락처 한 줄(email · phone · address · 생년월일, 10pt) / 밑줄
  / **프로필 사진(있으면) 우측 24mm**(photoUrl의 `/files/{id}/download` 파일을 디스크에서 읽어 임베드, 실패 시 생략)
- 섹션 제목: 14pt Bold + 좌측 아큐언트 바(2mm) + 얇은 구분선
- 경력/프로젝트: 왼쪽 `제목(회사·역할) · 기간` , 개요 문단 10.5pt(행간 1.4), 기술스택/링크 회색
- 기간 포맷: `yyyy.MM` (포함–종료), 종료 null → `현재`
- 학력: 학교( Bold ) + `schoolType · major · degree` 서브라인 + 기간/상태
- 스킬: `• name (level) · category` 행 단위
- 자격증: `name( Bold ) — issuer · yyyy.MM`
- 포트폴리오: `title( Bold )` + description + LINK면 linkUrl 표기 (FILE는 경로 길이상 제외)
- 빈 항목·미등록 프로필은 생략/널 안전

### 3.5 폰트
- **Spoqa Han Sans** (SIL OFL 1.1) `subset` 정적 TTF:
  - `src/main/resources/fonts/SpoqaHanSansRegular.ttf` (1.1MB)
  - `src/main/resources/fonts/SpoqaHanSansBold.ttf` (1.1MB)
- static holder로 classpath byte[]를 읽어 `BaseFont.createFont(path, IDENTITY_H, EMBEDDED, false, bytes, null)` 로드 (스레드 안전)
- 라이선스 파일 `LICENSE_OFL.txt` 동봉 (오픈소스 준수)

### 3.6 테스트 (JUnit 5, Mockito)
- `ResumePdfServiceImplTest` (6건):
  - 전체 항목 포함 view → `%PDF` 헤더 + 길이 > 1KB, 추출 텍스트에 이름/섹션/회사/스택 포함
  - 프로필 null + 빈 목록 → 여전히 유효 PDF
  - `PdfTextExtractor`로 추출해 섹션 제목("경력")·회사명 포함 검증 (임베드 폰트 ToUnicode 지원)
  - null 날짜(현재 재직) 렌더 오류 없음
  - 문서 sectionConfig에 포함된 섹션만 그 순서대로 렌더 (제외 섹션 텍스트 부재 검증)
  - 프로필 사진 파일 임베드(디스크 download 호출 검증) + 사진 파일 없어도 실패하지 않음

## 4. 구현 계획
| 단계 | 내용 | 비고 |
|------|------|------|
| 1 | 폰트 리소스 추가 + OpenPDF 의존성(`com.github.librepdf:openpdf`) | ✅ build.gradle.kts 1.3.30 |
| 2 | `ResumePdfService`(+Impl) 구현 — 레이어 규칙 준수, Javadoc 필수 | ✅ domain |
| 3 | `ResumeViewController`에 `GET /view/pdf` 추가 | ✅ api |
| 4 | `ResumePdfServiceImplTest` 작성 | ✅ domain test (3건) |
| 5 | 프론트 `ResumeViewPage` "PDF 다운로드" 버튼 (`apiDownload`) | ✅ resume frontend |
| 6 | 백엔드 테스트 + 프론트 `tsc -b`/build 검증 → 커밋/푸시 | ✅ 전체 백엔드 테스트 통과·프론트 build 성공 |

## 5. 참고 자료
- OpenPDF: https://github.com/LibrePDF/OpenPDF (Apache-2.0/LGPL/MPL, iText 4.0.0 포크)
- Spoqa Han Sans: SIL OFL 1.1 — https://github.com/spoqa/spoqa-han-sans
- 내부: `docs/plans/011-260821-resume-portfolio-integration-design.md` (§3.3 API, Phase 5)

---
*작성일: 2026-08-31*