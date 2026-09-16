# 031-260916-company-manage-design 설계 문서

## 개요
- **목적**: 회사 단위 관리 페이지 신설 — 내 별점·크롤링 평점 표시, MD 분석 메모 저장/열람, 북마크/차단 관리, 우측 슬라이드 패널 UI
- **범위**: scraper 백엔드(신규 테이블·API) + scraper 프론트(신규 `/companies` 라우트·슬라이드오버)
- **작성일**: 2026-09-16
- **작성자**: AI Assistant / 사용자
- **결정 사항(사용자 확정)**: ① 내 별점(1~5) + 크롤링 평점 함께 표시 ② MD는 DB TEXT 저장 + .md 내보내기 ③ 스크래퍼 SPA `/companies` ④ 전체/북마크/차단 탭

## 1. 배경 및 이유
- 기업 평점은 `company_ratings`에 수집되고 있으나 **사용자 관점의 평가(내 별점)·메모·북마크**를 둘 곳이 없음.
- 차단은 `company_blacklist`로 관리되나 목록·해제 UI가 분산되어 있어 회사별 판단 근거(분석 메모)와 연결되지 않음.
- 팝업이 아닌 **노션식 우측 슬라이드**로 목록 맥락을 유지한 채 상세(MD 렌더)를 보고 편집하려는 요구.

## 2. 요구 사항
### 2.1 기능 요구 사항
- [ ] FR-001: 회사 목록 — 탭 필터 (전체 / 북마크 / 차단). 전체=내 메모+평점 보유 회사, 차단은 blacklist 연동
- [ ] FR-002: 행 표시 — 회사명, 크롤링 평균(출처별 점수 툴팁), 내 별점(★ 1~5), 북마크 여부, 차단 여부, 메모 존재 뱃지, 업데이트 일시
- [ ] FR-003: 행 클릭 → 우측 슬라이드 패널 — 평점 블록 + MD 렌더 뷰 + 편집 탭(편집/미리보기) + 별점 입력 + 북마크 토글 + 차단 토글
- [ ] FR-004: MD 저장 — DB MEDIUMTEXT, optimistic UI 불필요(저장 버튼 명시), 저장 후 updated_at 갱신
- [ ] FR-005: .md 내보내기 — 파일명 `{회사명}-analysis.md`, 마크다운 원문 다운로드
- [ ] FR-006: 계정 격리 — 모든 조회/수정은 본인 `account_id` 범위 (SecurityUtils.currentAccountId)
- [ ] FR-007(후속): viewer 공고 목록의 회사명 클릭 → 동일 슬라이드 패널 오픈

### 2.2 비기능 요구 사항
- 성능: 목록 페이징(기본 50개), 회사명 정규화 컬럼 인덱스
- 보안: MD 렌더 시 raw HTML 비허용(react-markdown 기본, rehype-raw 금지) → XSS 차단
- 원칙: 회사명 정규화 규칙은 `CompanyBlacklistService.normalize` 단일 소스 재사용 (중복 구현 금지)

## 3. 설계
### 3.1 아키텍처
```
scraper frontend (/platform/scraper/companies)
  ├── Companies.tsx (목록 + 탭)
  └── components/CompanySlideOver.tsx (우측 패널: 보기/편집/별점/북마크/차단/내보내기)
        └── react-markdown (신규 의존성)
              ↕ /scraper/company-notes (+ 기존 /company-ratings, /company-blacklist)
scraper backend
  ├── controller/CompanyNoteController.java — /company-notes CRUD + /export/{id}
  ├── service/CompanyNoteService.java — 정규화·계정격리·북마크/별점/MD 저장
  ├── model/CompanyNote.java + repository/CompanyNoteRepository.java
  └── DDL docs/scraper/ddl-v10.sql (신규)
```

### 3.2 데이터 모델
신규 `company_notes` (계정별 1회사 1행, UK(account_id, company_name_normalized)):
| 컬럼 | 타입 | 설명 |
|---|---|---|
| id | BIGINT PK | |
| account_id | BIGINT NOT NULL | 소유 계정 (FK 아님 — auth DB 분리 가정, 기존 blacklist와 동일 패턴) |
| company_name_normalized | VARCHAR(200) | 정규화 회사명 (normalize 재사용) |
| company_name_display | VARCHAR(200) | 화면 표시용 원문 (최초 입력값) |
| my_stars | TINYINT NULL | 내 별점 1~5 (NULL=미지정) |
| is_bookmarked | BOOLEAN DEFAULT FALSE | 북마크 |
| note_md | MEDIUMTEXT NULL | 분석 마크다운 원문 |
| created_at / updated_at | DATETIME | |
인덱스: `idx_company_notes_account(account_id)`, UK `(account_id, company_name_normalized)`
- 차단 상태는 별도 테이블(`company_blacklist`)에서 조인 조회 — 중복 저장 금지.
- 크롤링 평점은 `company_ratings` 조인(전역, company_name 기준 매칭 — 정규화 불일치 시 표시 생략).

### 3.3 API 설계
| 메서드 | 경로 | 설명 |
|---|---|---|
| GET | /company-notes?tab=all\|bookmarked\|blocked&q= | 내 목록 (평점·차단여부 조인, 페이징) |
| GET | /company-notes/{id} | 상세 (note_md 포함) |
| POST | /company-notes | 생성 {companyName, myStars?, isBookmarked?, noteMd?} — 멱등(normalized 중복 시 갱신) |
| PUT | /company-notes/{id} | 별점/북마크/MD 수정 (본인 검증) |
| DELETE | /company-notes/{id} | 삭제 (본인 검증) |
| GET | /company-notes/{id}/export | .md 다운로드 (text/markdown, Content-Disposition) |
- 차단 토글은 기존 `/company-blacklist` API 재사용 (신규 API에 중복 구현 금지).
- 응답은 `ApiResponse` 래핑 (scraper 컨트롤러 관례).

### 3.4 프론트 설계
- 라우트: `/companies` 추가 — `App.tsx` + nginx는 `/platform/scraper/` try_files로 SPA 처리됨 (SpaFallbackController에 `/companies` 추가 필요).
- `CompanySlideOver`: `fixed right-0 top-0 h-full w-[520px] max-w-[90vw]`, backdrop 클릭·ESC 닫기, 탭 [보기 | 편집]. 보기는 react-markdown 렌더, 편집은 textarea + 미리보기 분할.
- 별점: lucide Star 5개 클릭 입력. 북마크: Bookmark 아이콘 토글.
- 목록 행 우측에 차단 뱃지 — 차단 탭에서는 해제 버튼 제공.

## 4. 구현 계획
| 단계 | 내용 | 예상 |
|---|---|---|
| Phase 1 | DDL v10 + deploy 라인, Entity/Repo/Service/Controller + Service 테스트 | 백엔드 |
| Phase 2 | react-markdown 설치, Companies.tsx + CompanySlideOver + SpaFallback 라우트 | 프론트 |
| Phase 3 | viewer 회사명 클릭 연동(FR-007), .md 내보내기 | 확장 |
| Phase 4 | 빌드·배포·실서버 확인, 일지/AGENTS 반영 | 검증 |

## 5. 참고 자료
- 기존: `model/CompanyRating.java`, `model/CompanyBlacklist.java`, `service/CompanyBlacklistService.java` (normalize 재사용)
- 마크다운 렌더: react-markdown (rehype-raw 미사용 = XSS 안전)
- DDL 버전 규칙: `docs/scraper/ddl-v{번호}.sql` + deploy-backend.yml 라인 추가

---
*작성일: 2026-09-16*
