# 022-260830-회사 블랙리스트 카테고리 시스템 설계

## 개요
- **목적**: 회사 차단 시 등록하는 차단 사유/회사유형을 **DB 마스터 코드화**하고, 사용자가 입력한 새 카테고리가
  자동으로 마스터에 승격되어 다음부터 추천되도록 하는 **실사용 데이터 → 마스터 축적** 설계.
- **범위**: scraper backend(등록/수정/삭제/검색 API) + packages/ui-shared(공용 다이얼로그/목록 모달) + 3개 앱 연동
- **작성일**: 2026-08-30
- **작성자**: AI Assistant / 사용자

## 1. 배경 및 이유
사용자가 회사 공고를 숨길(차단) 때 사유를 남기고자 했다. 처음엔 자유 메모만 있었으나,
요구가 "카테고리화 + 자동완성 + 여러 개 등록 + 이전에 쓴 항목이 추천에 나오는" 방식으로 진화했다.
프로젝트의 **하드코딩 지양(최우선) 원칙**에 따라 회사유형/사유 같은 값은 코드에 두지 않고 **DB 마스터로** 둔다.
또한 사용자가 직접 입력한 새 카테고리를 마스터로 승격시킴으로써 "시드 + 실사용 축적"이 함께 이뤄져
별도 관리 없이도 카테고리가 자연스럽게 확장된다(Notion 태그 방식).

## 2. 요구 사항
### 2.1 기능 요구 사항
- [x] FR-001: 회사 차단 시 카테고리를 **자유 입력 + 제안 드롭다운**으로 등록 (복수, Notion 태그식)
- [x] FR-002: 입력 중 기존 카테고리가 디바운스 검색 제안으로 나온다
- [x] FR-003: 새로 입력한 카테고리는 마스터에 **자동 승격(멱등 upsert)** 되어 다음부터 제안된다
- [x] FR-004: 회사유형/사유/사용자 카테고리를 구분해 목록에 **태그 배지**로 표시
- [x] FR-005: 기존 차단 항목의 카테고리를 **수정** 가능
- [x] FR-006: 자유 메모 필드는 UI에서 제거 (기존 DB 데이터는 보존)

### 2.2 비기능 요구 사항
- 하드코딩 지양: 카테고리 목록을 코드 배열로 두지 않음, 전부 DB + 검색 API
- scraper는 `ddl-auto: validate` 이므로 DDL 선실행 필수 (배포 시 DDL 라인)
- 다대다로 개인별 카테고리 선택 보존, 멱등 갱신

## 3. 설계
### 3.1 데이터 모델
```
block_reasons (마스터)
├── id            BIGINT PK
├── name          VARCHAR(50)  UNIQUE  (카테고리명, 예: 스타트업, 연봉·복지 협상 불가)
├── category      VARCHAR(20)  (company_type / reason / user)
├── sort_order    INT          (정렬)
└── active        TINYINT

blacklist_block_reason (연결 테이블, 다대다)
├── blacklist_id   FK → company_blacklist (CASCADE)
└── block_reason_id FK → block_reasons (CASCADE)

company_blacklist (기존)
├── account_id, company_name_normalized (uk_account_company)
├── reason         VARCHAR(200)  자유 메모 — UI 제거됨, 기존 데이터 보존용으로 컬럼 유지
└── blockReasons   @ManyToMany → blacklist_block_reason
```
- `category` 값: `company_type`(회사유형 시드 8개: 스타트업 X·스타트업·대기업·중견기업·외국계·공공기관·아웃소싱·블라인드),
  `reason`(차단 사유 시드 7개), `user`(사용자가 직접 입력해 승격된 카테고리)
- DDL: `docs/scraper/ddl-v7.sql`(block_reasons + 사유 시드), `docs/scraper/ddl-v8.sql`(category 컬럼 멱등 ALTER + 회사유형 시드 + 연결테이블)

### 3.2 API 설계
| 메서드 | 경로 | 설명 |
|--------|------|------|
| GET | `/company-blacklist/reasons` | 활성 카테고리 전체 (초기 로드) |
| GET | `/company-blacklist/reasons/search?q=` | 활성 카테고리 검색(자동완성), 이름 포함 Top20 sort_order 정렬 |
| GET | `/company-blacklist` | 내 블랙리스트 목록 |
| POST | `/company-blacklist` | 차단 등록 (중복이면 갱신). `companyName, reasonIds, categoryNames, reason` |
| PUT | `/company-blacklist/{id}` | 기존 항목 카테고리 수정 (본인 항목만, 메모 보존). `reasonIds, categoryNames` |
| DELETE | `/company-blacklist/{id}` | 차단 해제 |

- `categoryNames` = 사용자가 Enter로 입력한 **새 텍스트** → `BlockReasonService.ensureCategory(name)`이
  `category='user'`로 멱등 upsert → 이후 검색 제안에 포함.

### 3.3 컴포넌트
- `packages/ui-shared/BlockConfirmDialog.tsx`: 자유 input + 디바운스(250ms) 검색 제안 드롭다운 + 복수 태그 칩.
  Enter/선택으로 추가, Backspace 제거. `initialTags`(수정 프리필) + `confirmLabel`("차단"/"수정") 지원. 3개 앱 공용.
- `packages/ui-shared/BlacklistManagerModal.tsx`: 차단 목록, 카테고리 태그 배지(company_type=violet/reason=slate),
  "수정"/"해제" 버튼 (`onEdit`/`onUnblock`).
- 호출부: scraper `Search.tsx`, `Viewer.tsx` + resume `PostingsBrowsePage.tsx` — `editTarget` 상태로 수정 다이얼로그 연결.

### 3.4 UX 흐름 (최종 결정 — 3차 재정의 후)
1. 회사 차단 클릭 → 다이얼로그에 빈 입력 상자
2. "스타트업" 입력 중 → 이전에 쓰인 카테고리 + 시드가 검색 제안으로 표시
3. Enter 또는 선택 → 태그 추가 (복수 가능), Backspace로 제거
4. 신규 텍스트는 `categoryNames`로 전달 → 백엔드가 마스터 승격 → **다음 차단 시 제안에 자동 등장**
5. "차단" → POST 등록. 목록에서 "수정" → 같은 다이얼로그가 기존 카테고리 프리필 → PUT 갱신.

## 4. 구현 계획 (이력)
| 단계 | 내용 | 상태 |
|------|------|------|
| 1 | block_reasons 마스터 + 사유 시드(ddl-v7) + 검색 API + 자동완성 | ✅ 배포 |
| 2 | category 컬럼 + 회사유형 시드 + 연결테이블(다대다, ddl-v8) + 태그 배지 | ✅ 배포 |
| 3 | UX 재정의: 체크박스 → 자유입력+제안 드롭다운 + ensureCategory 마스터 승격 | ✅ 배포 |
| 4 | 불변 리스트 병합 500 버그 수정(가변 ArrayList + 단일 save) | ✅ 배포 |
| 5 | 자유 메모 UI 제거 + 차단 리스트 **수정**(PUT, 수정용 다이얼로그) | ✅ 배포 |
| 6 | CSP에 다음 우편번호 CDN 허용 + 워크플로우 infra/** 트리거 | ✅ 배포 |
| 7 | 데이터 마이닝: 카테고리별 차단 통계 (`GET /company-blacklist/stats` + 대시보드 차단 회사 유형/사유) | ✅ 배포 |

## 5. 테스트
- `CompanyBlacklistServiceTest`: 등록(카테고리/중복 갱신/빈 목록/사용자 승격) + update(교체+메모보존/타인 ignored)
- `BlockReasonServiceTest`: 검색/빈 키워드
- 프론트: scraper/resume/platform 3개 `tsc -b` 통과

## 6. 참고 자료
- 원칙: `AGENTS.md` 하드코딩 지양(DB 코드화 + 질의형 검색 API)
- DDL: `docs/scraper/ddl-v7.sql`, `docs/scraper/ddl-v8.sql`
- 작업일지: `docs/daily/2026-08-30-work-log.md`

---
*작성일: 2026-08-30*
