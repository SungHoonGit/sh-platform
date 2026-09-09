# 025-260909-포트폴리오 고도화 설계 문서

## 개요
- **목적**: 포트폴리오 작업물을 이력서와 **독립된 저장소**로 관리하고(독립 관리 페이지), 이력서 작성 시 등록된 작업물을 **프로젝트에 불러오기(복사)**로 활용한다. 동시에 작업물 필드를 시장 표준(썸네일 · GitHub/데모/영상 링크)으로 고도화하고 이력서 보기 화면·PDF에 반영한다.
- **범위**: ① 독립 "포트폴리오 관리" 페이지(`#/portfolio`) ② 이력서 포트폴리오 섹션 편집 유지(같은 데이터) ③ 프로젝트 폼의 "작업물에서 가져오기"(복사 스냅샷) ④ 작업물 필드 고도화(썸네일·다중링크) ⑤ 보기 화면/PDF 표시 고도화.
- **작성일**: 2026-09-09
- **작성자**: AI Assistant (사용자 방향 확인 후 작성)

## 1. 배경 및 이유
- 기존 `resume_portfolio_items`는 `user_id` 기준 **계정 단위 공유 데이터**지만 편집 진입점이 이력서 편집 화면뿐이라, 이력서와 무관하게 작업물을 모아두고 관리할 수단이 없음.
- 사람인/잡코리아는 '포트폴리오 및 기타문서'를 별도 항목으로 관리하고, GitHub는 저장소 단위 + README, 노션·개인웹은 **썸네일 + 제목 + 한 줄 소개 + 링크** 카드형. 개발자 포트폴리오에서 **대표 이미지**와 **GitHub/데모/영상 링크**는 사실상 표준.
- 사용자 결정: ① 독립 포트폴리오 관리 + ② 이력서 작성 시 등록 작업물 일부(제목·상세 등)를 프로젝트에 선택해 채우기 + ③ 불러오기는 **복사(스냅샷)** 방식 + ④ 이력서 포트폴리오 섹션 **편집 유지 + 독립 페이지 추가**.

## 2. 요구 사항
### 2.1 기능 요구 사항
- [ ] FR-001: **독립 "포트폴리오 관리" 페이지** (`#/portfolio`, 서브네비 "포트폴리오 관리")에서 계정 단위 작업물 CRUD·순서 정렬이 가능하다.
- [ ] FR-002: 작업물 1건에 **썸네일 이미지**(jpg/png) 업로드·미리보기가 가능하다.
- [ ] FR-003: 작업물 1건에 **GitHub / 데모(배포) / 시연 영상** 링크를 각각 선택 저장할 수 있다 (다중 링크).
- [ ] FR-004: 기존 **첨부파일 1개**(pdf/pptx/docx/png/jpg)와 **설명** 필드는 유지한다. `item_type` 분기는 화면·PDF 렌더링에서 제거하고 "값이 존재하는 필드" 기준으로 표시한다 (기존 데이터 호환).
- [ ] FR-005: 이력서 편집의 **프로젝트 추가/수정 폼**에 **"작업물에서 가져오기"** 셀렉트가 있다. 작업물 선택 시 제목→프로젝트명, 설명→설명, GitHub(또는 데모) 링크→관련 링크로 **복사**되어 채워진 뒤 자유 수정(스냅샷, 이후 작업물 수정은 영향 없음).
- [ ] FR-006: 이력서 편집의 **포트폴리오 섹션**은 기존처럼 그대로 편집 가능하고, 동일 데이터를 독립 페이지와 공유한다.
- [ ] FR-007: 보기 화면(템플릿 3종)에서 포트폴리오를 **이미지 카드**로 표시: 썸네일 → 제목 → 설명 → 링크 버튼(GitHub/데모/영상) → 첨부 다운로드.
- [ ] FR-008: PDF(테마 3종)에 **썸네일 이미지**(가능 시) + 제목 + 링크/첨부 안내를 표시한다.
- [ ] FR-009: 공유 링크(비로그인) 뷰에서도 썸네일이 보인다.

### 2.2 비기능 요구 사항
- 보안: 파일 조회는 본인(인증) 또는 공유 토큰 검증 경유로만 가능.
- 용량: 썸네일은 프론트에서 5MB 이하 제한, 서버는 기존 `/files` 허용 목록(png/jpg) 사용.
- 호환: 기존 데이터(linkUrl/filePath/item_type)가 그대로 렌더링되어야 하며, 마이그레이션 용이.
- 하드코딩 금지: 링크 라벨 등 기준값은 코드 하드코딩 없이 필드 기반 처리(DB 컬럼 확장, UI 설정으로 분리).

## 3. 설계
### 3.1 데이터 모델 (DDL — `docs/resume/ddl-resume-v10.sql`, 멱등 ALTER)
`resume_portfolio_items`에 컬럼 추가 (성능: 신규 인덱스 불필요, 기존 `idx_resume_portfolio_user` 활용):

| 컬럼 | 타입 | 비고 |
|------|------|------|
| `thumbnail_path` | VARCHAR(300) NULL | 썸네일 저장 경로 (`/api/v1/files/{id}/download`) |
| `github_url` | VARCHAR(300) NULL | GitHub 저장소 링크 |
| `demo_url` | VARCHAR(300) NULL | 데모/배포 링크 |
| `video_url` | VARCHAR(300) NULL | 시연 영상 링크 |

- 기존 컬럼(`item_type`, `file_path`, `link_url`, `description`, `display_order`) 유지. 신규 저장 시 `item_type='LINK'` 기본값.
- `resume_projects`는 변경 없음 (가져오기는 복사 방식이므로 스키마 불변).

### 3.2 API
`PortfolioItemRequest` / `PortfolioItemResponse`에 신규 선택 필드 추가 (기존 필드 유지):
```
String title (필수), String itemType (기본 LINK, 호환), String thumbnailPath,
String githubUrl, String demoUrl, String videoUrl,
String filePath, String linkUrl, String description, Integer displayOrder
```
- 검증: URL 필드 최대 300자, 선택. 기존 엔드포인트 그대로, 스키마만 확장.
- 목록 조회 **`GET /api/v1/portfolio-items`**는 이미 존재 → 독립 페이지·가져오기 셀렉트에서 재사용.
- 공유 썸네일: **`GET /share/{token}/files/{fileId}`** 신규 — token 검증 → 소유자 userId 확정 → `fileStorageService.download(userId, fileId)` 반환(`Content-Disposition: inline`).

### 3.3 독립 포트폴리오 관리 페이지
- **라우트**: `App.tsx`에 `{ name: "portfolio" }` 추가, hash `#/portfolio`. 서브네비에 "포트폴리오 관리" 항목 추가 (`subnavItems`, 아이콘 e.g. FolderOpen).
- **페이지**: `PortfolioPage.tsx` 신규 — `CrudSection` 재사용. 섹션 설정(필드 정의)은 **`EditPage`의 포트폴리오 설정과 공유**하도록 별도 모듈(`src/config/portfolioConfig.ts`)로 추출.
- **필드** (기존 itemType 셀렉트 제거):
  - `thumbnailPath`: file, accept `.jpg,.jpeg,.png`, 라벨 "썸네일 이미지 (jpg/png, 5MB 이하)"
  - `githubUrl` / `demoUrl` / `videoUrl`: text, placeholder https 예시
  - `filePath`: 첨부파일(기존), `description`: textarea(기존), `title`: 필수(기존)
- **CrudSection 이미지 미리보기**: file 필드 중 accept가 이미지면 업로드 후 Bearer 토큰 fetch → objectURL `<img>` 미리보기 (기존 `ProfilePhoto` 패턴 컴포넌트화 → `SecureImage`).

### 3.4 프로젝트 "작업물에서 가져오기" (복사 스냅샷)
- **CrudSection 확장**: 폼 상단에 `importOptions`(prop)가 제공되면 **"작업물에서 가져오기"** 셀렉트 표시. 항목 선택 시 `onImport(item)` 콜백으로 필드 값 채움.
- **EditPage 프로젝트 섹션 적용**:
  - `GET /api/v1/portfolio-items`로 목록 로드 (제목 기준 표시).
  - 매핑(복사): `title → name`, `description → description`, `githubUrl || demoUrl → linkUrl` (기존 `role/techStack/startDate/endDate`는 빈 채로 유지).
  - 불러오기 후 사용자가 폼에서 자유 수정 → 저장 시 `resume_projects`에 **독립 복사본**으로 기록. 작업물 수정·삭제는 이미 입력된 프로젝트에 영향 없음.

### 3.5 보기 화면 (템플릿 3종)
- `templates/shared.tsx`에 `PortfolioCard` 공용 컴포넌트 추가:
  - 썸네일(`thumbnailPath`) 있으면 `SecureImage`로 이미지, 없으면 플레이스홀더
  - 제목·설명, 존재하는 링크만 버튼(GitHub/데모/영상, target=_blank), 첨부(`filePath`) 있으면 기존 다운로드 버튼
  - 호환: 기존 `linkUrl` 항목은 링크, `filePath` 항목은 다운로드 표시 (itemType 분기 제거)
- 공유 뷰(`ShareViewPage`): 썸네일은 `GET /share/{token}/files/{fileId}` 로드.

### 3.6 PDF (OpenPDF 3종)
- 기존 `PdfLayoutSupport.loadPhoto` 패턴으로 썸네일 로드(실패 시 이미지만 생략, 텍스트 정상 출력).
- 출력: [썸네일] 제목 / 설명 / 링크 텍스트(GitHub·데모·영상) / 첨부 다운로드 안내. 이미지 가로 90px 내외 비율 유지.

## 4. 구현 계획
| 단계 | 내용 | 산출물 |
|------|------|--------|
| Phase 1 | DDL v10 + Entity/DTO(Req/Res)/Service/Controller 확장 + 단위 테스트 + 배포 워크플로우 v10 DDL 추가 | backend/CI |
| Phase 2 | 포트폴리오 폼 개편(썸네일·다중링크) + `SecureImage` 미리보기 + 설정 모듈 분리 | frontend/common |
| Phase 3 | 독립 `PortfolioPage`(`#/portfolio`) + 서브네비 항목 추가 | frontend |
| Phase 4 | 프로젝트 "작업물에서 가져오기"(복사) — CrudSection `importOptions` + EditPage 적용 | frontend |
| Phase 5 | 보기 화면 `PortfolioCard` 3종 반영 + `/share/{token}/files/{fileId}` + 공유 뷰 적용 | frontend/backend |
| Phase 6 | PDF 테마 3종 썸네일·링크 표시 + 테스트 보강 | backend |
| Phase 7 | 빌드 → 커밋 → 배포 → 실사용 검증(작업물 등록→프로젝트 가져오기→이력서/PDF 확인) | CI |

## 5. 참고 자료
- 사람인 '포트폴리오 및 기타문서'(파일/URL 2모드), GitHub README 포트폴리오 표준, 노션·개인웹 포트폴리오 구성(썸네일+상세+링크)
- 기존 코드: `PortfolioItemController`(목록 조회 이미 존재), `ResumePortfolioItemEntity`, `EditPage SECTIONS`, `templates/shared.tsx ProfilePhoto`, `ResumeShare(ShareController)`

---
*작성일: 2026-09-09*