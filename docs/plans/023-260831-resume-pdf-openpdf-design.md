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

> 처음 1차 배포는 테마와 무관한 통일 서식이었으나, 사용자 확정으로 **테마(CLASSIC/MODERN/SARAMIN)별 레이아웃**을
> OpenPDF로 별도 구현해 문서의 `templateCode`에 따라 다른 서식을 출력한다.
> (화면 CSS와 픽셀 단위까지 동일하진 않지만 각 테마의 구조·색상을 따르는 제출용 인쇄 서식)

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
- [x] FR-011: **테마별 PDF 레이아웃** — 문서 `templateCode`로 레이아웃 선택 (미지정/미상 코드 → CLASSIC)
- [x] FR-012: **다운로드 파일명 = `(테마명) 문서제목.pdf`** — `pdfFilename(userId, documentId)` 서비스 메서드로 결정
  (documentId 없으면 `(클래식) 이력서.pdf`)
- [x] FR-013: **페이지 분리/사진 표준화** — 섹션 시작 잔여 공간 부족 시 다음 페이지 이동(`ensureRoom`, 고아 제목 방지),
  SARAMIN 섹션 박스 `KeepTogether`, 프로필 사진 프레임 24×32mm·0.8pt 회색 테두리로 테마 간 통일

### 2.2 비기능 요구
- 성능: 생성 1회 ~50ms 내 (스레드 안전 — 요청마다 인스턴스 생성), 결정적 출력
- 보안: 소유자 JWT 필수, 기존 `/view` 보안 정책 그대로
- 파일 크기: 폰트 2개 ≈ 2.3MB jar 포함 (repo 커밋 허용 수준)
- DB/DDL/인프라 변경 없음

## 3. 설계
### 3.1 아키텍처
```
ResumeViewPage(버튼) → GET /api/v1/view/pdf?documentId= (JWT)
   → ResumeViewController      (@GetMapping "/pdf", byte[] 응답)
   → ResumePdfService.generatePdf(userId, documentId)
       ├─ ResumeViewService.getMyResumeView(userId)   ← 기존 조립 재사용
       └─ resolveDocumentOption: 문서의 sectionConfig(순서) + templateCode(테마)
       └─ ResumePdfLayout 디스패치 (templateCode → CLASSIC/MODERN/SARAMIN)
            ├─ ClassicPdfLayout   (단일 컬럼 + 굵은 가이드 라인)
            ├─ ModernPdfLayout    (다크 사이드바 + teal 액센트)
            └─ SaraminPdfLayout   (박스 섹션 + 연락처 표)
            └─ Document(A4) + Font(Spoqa Han Sans) + Paragraph/Table
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

### 3.4 렌더 명세 (feat: 테마별 PDF 레이아웃)
- 페이지: A4(595×842), 좌우 18mm / 상하 16mm — 모든 테마 공통
- 섹션 순서: **문서 sectionConfig(포함·order) 반영**. 미지정 시 기본 편성 —
  경력 → 프로젝트 → 학력 → 스킬 → 자격증 → 자기소개 → 포트폴리오 (빈 섹션 생략)
- 테마 선택: `templateCode` (CLASSIC/MODERN/SARAMIN). 문서 없음·미상 코드 → CLASSIC.
- 기간 포맷: `yyyy.MM` (포함–종료), 종료 null → `현재`
- 공통 폰트/색 팔레트는 `PdfLayoutSupport`(패키지 private)로 공유(정적 로드, 스레드 안전)
- **사진 프레임 표준**: 모든 테마 동일 — 24×32mm(68×90.6pt), 0.8pt 회색 테두리, 2pt 패딩, 중앙 정렬 (`photoCell`)
- **페이지 분리**: CLASSIC/SARAMIN은 섹션 시작 전 `ensureRoom`으로 남은 공간 < 70pt면 `newPage()`
  (페이지 끝에 제목/박스만 남아 잘려 보이는 고아 방지), SARAMIN 섹션 박스는 `KeepTogether` 설정.

#### CLASSIC (CSS ClassicTemplate 대응, 단일 컬럼)
- 헤더: 이름 24pt Bold 좌측 + headline + 연락처 한 줄 / 우측 프로필 사진 24×32mm(있으면, 테두리)
  뒤에 진한 가이드 라인(border-b-2 효과)
- 섹션 제목: 13pt Bold + 0.9pt 진한 밑줄 (항목 간 구분선 없음, 여백으로 나눔)
- 경력/프로젝트: `제목 · 기간`(제목 Bold, 기간 우측 정렬) + 개요, 스택/링크 회색
- 학력: 학교 Bold + `schoolType · major · degree · 학점 · status` 서브라인
- 스킬: `• name (level) · category` / 자격증: name + 취득일 우측 + issuer 회색
- 포트폴리오: `[파일]/[링크]` 접두 + 제목 + 설명/LINK

#### MODERN (CSS ModernTemplate 대응, 다크 사이드바 2단)
- 페이지 2열(30/70): 좌측 다크 사이드바(slate-800) + 우측 메인
- 사이드바: 사진(72pt, 테두리) → 이름 중앙(흰색) → headline(teal) → 연락처 라벨-값
  → **학력/스킬/자격증 섹션**(라벨 소문자 회색 + 얇은 구분선, 흰색 계열 텍스트)
- 메인: 경력/프로젝트/자기소개/포트폴리오 — 섹션 제목 앞 **teal 액센트 바(13pt 높이)**,
  역할·기술스택은 teal 계열, 항목 제목 slate-800 Bold + 기간 우측(회색)
- 사진 로드 실패/미등록 시 사이드바는 사진만 생략, 나머지 정상 렌더

#### SARAMIN (CSS SaraminTemplate 대응, 박스형)
- 상단 프로필 박스(진한 테두리 2px): 좌측 사진(85×120pt, 테두리) + 우측 이름 20pt →
  headline → **연락처 라벨-값 표**(이메일/전화번호/생년월일/주소, 라벨 셀 bg-slate-50, 빈 값 `-`)
- 섹션: 회색 테두리 박스 + `bg-slate-100` 제목 바(밑줄) + 본문
- 경력: **회사명/직무/기간 3열 표**(헤더 행) + 업무 `· ` 불릿 문단
- 학력: 기간 열 + 학교/schoolType·major·degree·status 세부 2열 표
- 스킬: 스킬/숙련도·분류 2열 표 / 자격증: name + 취득일 우측 + issuer
- 프로젝트·포트폴리오: 항목 사이 점선 구분선(0.5pt), LINK 표기

### 3.5 폰트
- **Spoqa Han Sans** (SIL OFL 1.1) `subset` 정적 TTF:
  - `src/main/resources/fonts/SpoqaHanSansRegular.ttf` (1.1MB)
  - `src/main/resources/fonts/SpoqaHanSansBold.ttf` (1.1MB)
- static holder로 classpath byte[]를 읽어 `BaseFont.createFont(path, IDENTITY_H, EMBEDDED, false, bytes, null)` 로드 (스레드 안전)
- 라이선스 파일 `LICENSE_OFL.txt` 동봉 (오픈소스 준수)

### 3.6 테스트 (JUnit 5, Mockito)
- `ResumePdfServiceImplTest` (11건):
  - 전체 항목 포함 view → `%PDF` 헤더 + 길이 > 1KB, 추출 텍스트에 이름/섹션/회사/스택 포함
  - 프로필 null + 빈 목록 → 여전히 유효 PDF
  - `PdfTextExtractor`로 추출해 섹션 제목("경력")·회사명 포함 검증 (임베드 폰트 ToUnicode 지원)
  - null 날짜(현재 재직) 렌더 오류 없음
  - 문서 sectionConfig에 포함된 섹션만 그 순서대로 렌더 (제외 섹션 텍스트 부재 검증)
  - **MODERN 테마** → 연락처·스킬·경력·학교 존재 (사이드바+메인 분할 렌더)
  - **SARAMIN 테마** → 이메일 라벨·값, 회사명/직무/기간 표 헤더 존재 (박스형 렌더)
  - **미상 templateCode** → CLASSIC 폴백 (경력·이름 렌더)
  - **pdfFilename** → `(모던) 2026 포트폴리오.pdf` / documentId 없음 → `(클래식) 이력서.pdf`
  - 프로필 사진 파일 임베드(디스크 download 호출 검증) + 사진 파일 없어도 실패하지 않음
- `ResumePdfMultiPageTest` (1건): 40개 경력+장문으로 강제 다중 페이지 — 모든 테마에서 항목 유실 없음 회귀 검증

## 4. 구현 계획
| 단계 | 내용 | 비고 |
|------|------|------|
| 1 | 폰트 리소스 추가 + OpenPDF 의존성(`com.github.librepdf:openpdf`) | ✅ build.gradle.kts 1.3.30 |
| 2 | `ResumePdfService`(+Impl) 구현 — 레이어 규칙 준수, Javadoc 필수 | ✅ domain |
| 3 | `ResumeViewController`에 `GET /view/pdf` 추가 | ✅ api |
| 4 | `ResumePdfServiceImplTest` 작성 | ✅ domain test (3건) |
| 5 | 프론트 `ResumeViewPage` "PDF 다운로드" 버튼 (`apiDownload`) | ✅ resume frontend |
| 6 | 백엔드 테스트 + 프론트 `tsc -b`/build 검증 → 커밋/푸시 | ✅ 전체 백엔드 테스트 통과·프론트 build 성공 |
| 7 | 테마별 PDF 레이아웃 — `PdfLayoutSupport` + `ResumePdfLayout` 3종(Classic/Modern/Saramin) + 레이아웃 디스패치 | ✅ domain (테마 테스트 3건 추가, PDF 테스트 9건) |
| 8 | 추가 개선 — 파일명 `(테마) 문서제목.pdf`·페이지 분리(`ensureRoom`/KeepTogether)·사진 프레임 통일·다중 페이지 회귀 테스트 | ✅ domain (PDF 테스트 12건) |
| 9 | 프론트 검증 + 서버 배포 확인 (테마 전환 → PDF 3종·잘림/파일명 확인) | ⏳ 배포 후 사용자 확인 |

## 5. 참고 자료
- OpenPDF: https://github.com/LibrePDF/OpenPDF (Apache-2.0/LGPL/MPL, iText 4.0.0 포크)
- Spoqa Han Sans: SIL OFL 1.1 — https://github.com/spoqa/spoqa-han-sans
- 내부: `docs/plans/011-260821-resume-portfolio-integration-design.md` (§3.3 API, Phase 5)

---
*작성일: 2026-08-31*