# 013-260824-다중 이력서 메뉴구조 설계 문서

## 개요
- **목적**: resume 앱에 메뉴 탭(이력서 관리/지원 관리)과 다중 이력서(문서) 관리를 도입한다
- **범위**: resume 백엔드 documents 도메인 + 프론트 라우팅/헤더 개편
- **작성일**: 2026-08-24
- **상위 문서**: `012-260824-resume-platform-roadmap-design.md` §3.1 (A안 채택)

## 1. 배경

사용자 피드백 (2026-08-24):
1. "여러 이력서를 설정하는 메뉴가 없어서 취지가 안 맞는다"
2. "이력서 관리, 지원 관리(지원내역) 등 여러 메뉴가 필요하다"
3. "신규 등록할 때 기존 이력서 불러오기 기능"
4. "현재 명확한 것은 여러 이력서 등록 관리"

→ 로드맵 012의 Phase 6(다중 이력서)을 우선 착수, A안 확정.

## 2. 설계 결정

### 2.1 아키텍처: A안 확정 (012 §3.1)
- 항목 데이터(profile~portfolio_items 8테이블)는 **마스터**로 user_id 직속 유지
- `resume_documents` = 뷰 정의(title, template_code, is_primary, section_config JSON)
- 모든 문서가 마스터 데이터를 공유 참조 → 문서별 차등은 섹션 포함/순서로 표현
- **"불러오기" = 기존 문서의 섹션 편성을 복제하여 신규 생성** (fromDocumentId)

### 2.2 메뉴 구조 (사용자 확정안)
```
CommonHeader: SH Platform | 이력서 관리 | 지원 관리 | (우측) 로그인/로그아웃
```

| 탭 | 경로 | 내용 |
|----|------|------|
| 이력서 관리 | `#/resumes` | 문서 카드 목록 + [새 이력서 만들기(불러오기 select)] |
| (문서) 보기 | `#/r/{id}` | 마스터 데이터를 해당 문서의 섹션편성으로 렌더 |
| (문서) 편집 | `#/r/{id}/edit` | 섹션 CRUD + 상단 문서 전환 드롭다운 |
| 지원 관리 | `#/applications` | Phase 7 — v1은 준비중 플레이스홀더 |

### 2.3 기존 사용자 마이그레이션
- `GET /api/v1/documents` 호출 시 목록이 비어 있으면 **"내 이력서" 자동 생성**
  (기본 섹션편성 7개 전부 포함, is_primary=true) → 별도 SQL 마이그레이션 불필요

## 3. API 설계

| Method | Path | 설명 |
|--------|------|------|
| GET | `/api/v1/documents` | 목록 (없으면 기본 문서 자동 생성) |
| POST | `/api/v1/documents` | 생성 `{title, fromDocumentId?}` |
| PUT | `/api/v1/documents/{id}` | 수정 `{title?, templateCode?, sectionConfig?, isPrimary?}` |
| PUT | `/api/v1/documents/{id}/primary` | 대표 지정 (나머지 해제) |
| DELETE | `/api/v1/documents/{id}` | 삭제 (마지막 1개는 삭제 불가) |

section_config JSON 예시:
```json
[
  {"key": "careers",        "included": true, "order": 1},
  {"key": "projects",       "included": true, "order": 2},
  {"key": "educations",     "included": true, "order": 3},
  {"key": "skills",         "included": true, "order": 4},
  {"key": "certificates",   "included": true, "order": 5},
  {"key": "introductions",  "included": true, "order": 6},
  {"key": "portfolioItems", "included": true, "order": 7}
]
```
섹션 key는 `ResumeView` 필드명과 일치 (프론트에서 filter+sort에 사용).

## 4. 구현 계획

| 단계 | 내용 |
|------|------|
| DDL v3 | `resume_documents`, `resume_applications` (012 §3.2 DDL 그대로) |
| 백엔드 | ResumeDocumentEntity/Repository, DocumentService(+Impl), DocumentsController, DTO, Mockito 테스트 |
| 프론트 | 헤더 탭, App 라우팅 개편, ResumesPage(목록/생성), 뷰·편집에 섹션편성 적용 |
| 이후 | applications CRUD(Phase 7), 템플릿 분기·TOC(Phase 8) |

## 5. 참고
- 012 §3.1~3.5, §4 Phase 6~8
---
*작성일: 2026-08-24*
