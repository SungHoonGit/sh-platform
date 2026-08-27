# 021-260827-resume-benchmark-design

# 이력서 편집기 벤치마킹 개선 설계

## 개요
- **목적**: JobKorea/사람인 등 타사 이력서 등록 UX 대비 우리 resume 편집기의 부족분을 파악하고 설계·보완
- **범위**: 학력(GPA/전공/학교검색), 자기소개 편집형, 우측 네비게이션, 첨부 다운로드, 사진(미리보기/용량), 다중 이력서 정체성
- **작성일**: 2026-08-27
- **작성자**: AI Assistant + 사용자

## 1. 배경 및 이유
JobKorea 다운로드 `이력서_20260827.doc`를 우리 시스템에 시딩하며 발견한 차이와,
사용자가 제기한 "벤치마킹 후 재기획" 요청. 현재 시스템은 이미 상당 부분 완성 (사진업로드/첨부/자기소개 다중/전 섹션 +추가)이나
타사 대비 편집 UX·유효성·정체성에서 부족.

## 2. 요구 사항 (사용자 제기 + 조사)
### 2.1 기능 요구
- [ ] FR-001: 학력 **학점(GPA)** 필드 (예: 3.9 / 4.5)
- [ ] FR-002: **전공 검색·선택** (학교처럼 자동완성)
- [ ] FR-003: 학교 검색 시 **선택된 학교 유형(고등/대학/대학원)에 맞게만** 제안 — 현재 유형 무관 전체 노출(버그)
- [ ] FR-004: **자기소개/전체 목록도 인라인 편집형** (목록+수정 버튼 → 바로 편집 폼)
- [ ] FR-005: **우측 고정 네비게이션(목차)** — 섹션 이동 편의
- [ ] FR-006: **첨부파일 다운로드** — 현재 Classic만 버튼 존재, Modern/Saramin 템플릿에 버튼 없음(버그)
- [ ] FR-007: **사진 미리보기** + **사진 1MB 유효성** (현재 10MB 공용 multipart, 1MB 구분 없음)
- [ ] FR-008: **이력서 섹션순서/포함 토글** (다중 이력서 다양화 — 우측 네비와 연계)

### 2.2 질문: 학교/전공 데이터 소스 (DB 연동 vs 하드코딩)
- **결론**: 잡코리아·사람인은 **코드화 테이블 + 검색 API**를 사용 (학교: 교육부 공시 4만+곳, 전공: 한국표준교육분류). 현재 우리는 프론트 하드코딩 `schools.ts`(50곳) — **부족**.
- **제안**: `schools`(id, name, school_type) · `majors`(id, name) 테이블 + `GET /schools/search?q=` · `GET /majors/search?q=` API. DB 마스터 데이터는 첫 배포 시 1회 시드. 프론트는 서버 검색 사용(자유입력 폴백).

## 3. 설계
### 3.1 데이터 모델 (resume_platform)
```sql
-- 테이블 코드화 (신규)
CREATE TABLE schools (id BIGINT PK AUTO, name VARCHAR(100) NOT NULL,
    school_type VARCHAR(20) NOT NULL, /* 고등학교/대학교/대학원 */
    INDEX idx_schools_name (name));
CREATE TABLE majors (id BIGINT PK AUTO, name VARCHAR(100) NOT NULL,
    major_type VARCHAR(20) NULL, /* 학사/석사/박사 대분류 등 */
    INDEX idx_majors_name (name));

-- 학력 컬럼 추가
ALTER TABLE resume_educations ADD COLUMN gpa VARCHAR(20) NULL AFTER degree;  /* 예: "3.9 / 4.5" */
```
프론트 `schools.ts`는 제거 → `school`/`major` 필드를 `school` 타입 자동완성(서버 검색)으로 통합.

### 3.2 자기소개/전체 편집형 (FR-004)
- CrudSection에 `inline?: boolean` 추가 → inline일 땐 목록 행 자체가 편집 폼(제목+필드 인라인), 저장 시 닫힘.
- 자기소개 섹션을 inline 편집형으로 전환하며, 타사처럼 "첫문장 = 항목 제목" 구조 유지.

### 3.3 우측 네비게이션 (FR-005)
- EditPage 레이아웃: 좌측 편집폼 + 우측 `position: sticky` 섹션 목차(클릭 → 섹션 scrollIntoView).
- 동시에 FR-008 섹션 순서/표시 토글 UI를 네비에 배치 (section_config 갱신).

### 3.4 첨부 다운로드 (FR-006, 실제 버그)
- ModernTemplate·SaraminTemplate의 FILE 포트폴리오에 Classic과 동일한 다운로드 버튼 추가
  (`import { apiDownload, fileDownloadPath }` + `void apiDownload(fileDownloadPath(pi.filePath!), pi.title)`).
- client.ts `fileDownloadPath` 방어 로직 보강 (중복 prefix 정규화).

### 3.5 사진 (FR-007)
- 미리보기: 업로드 전 FileReader 미리보기 + 저장 후 `/resume/api/v1/files/{id}/download` 이미지 렌더(shared.tsx ProfilePhoto 재사용).
- 1MB 유효성: 프론트 `file.size > 1MB` 사전 차단 + 백엔드 사진 경로(`/profile/photo`)에 1MB 체크 추가(일반 첨부는 10MB 유지).

### 3.6 다중 이력서 정체성 (FR-008 / 사용자 "중복" 의견)
- **현재 설계**: 이력서(document) = 하나의 마스터 데이터(profile/education/...)의 **뷰**. 여러 이력서가 같은 데이터를 공유 → "추가해도 동일하게 보여 중복처럼 느껴짐". 이는 버그가 아니라 의도된 설계.
- **정리 방향**: 이력서 = "서식(템플릿)+포함 섹션+순서" 조합. UX상으로는
  - 문서 생성 시 "기존 문서 복사(fromDocumentId)" 명시화
  - "새 이력서"가 아니라 "이력서/서식 추가" 의미를 UI 문구로 명확히
  - (장기) 각 이력서가 독립 데이터를 가질 필요가 있다면 별도 설계 — 사용자 확인 필요

## 4. 구현 계획
| 단계 | 내용 | 비고 |
|------|------|------|
| Phase 1 (버그 선수) | Modern/Saramin 다운로드 버튼, 학교검색 유형 필터, 학점 필드, 사진 미리보기+1MB | 즉시 배포 가능 |
| Phase 2 | schools/majors 테이블+검색API+시드, 프론트 서버검색 연결 | DB 코드화 |
| Phase 3 | 우측 네비 + 섹션 편집형 + 섹션 순서/토글 | UX 개편 |
| Phase 4 | 다중 이력서 정체성 정리 (문구/복사 명시) | 설계 확정 후 |

## 5. 참고 자료
- `이력서_20260827.doc` (JobKorea 다운로드)
- `docs/plans/*-resume-platform-roadmap-design.md`
- nginx prefix 규칙: `infra/nginx/sh-platform.conf` (/resume/ → strip)

---
*작성일: 2026-08-27*
