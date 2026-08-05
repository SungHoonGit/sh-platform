# 스크래퍼 스케줄링 요건 정리 (2026-08-01)

> 기존 문서에 흩어진 스케줄링 요건을 한 곳에 모으고, 현재 구현(as-is)과의 차이(gap)를 정리한 문서.
> 요건 출처: `sh-platform/docs/scraper/architecture-v2.md`, `docs/REACT-FRONTEND-DESIGN-V3.md`, `docs/OCI-PLATFORM-GUIDE.md`, `docs/common/modules.md`, `docs/scraper/roadmap.md`
> 설계 확정(2026-08-01): §1.5 설계 방향, §1.6 계정 격리 결정 참조

---

## 1. 요건 요약

사용자가 검색 조건(키워드/경력/지역/사이트)을 스케줄로 등록하면, 설정한 cron에 맞춰 주기적으로 크롤링을 수행하고 결과를 MD 파일로 축적하며, 수동 실행·이력 확인·수정·삭제가 가능해야 한다.

### 1.1 핵심 유스케이스

| # | 시나리오 |
|---|----------|
| UC-1 | 통합검색 조건을 그대로 스케줄로 등록 (Search → "이 조건으로 스케줄 등록") |
| UC-2 | 등록된 스케줄 목록 조회 (이름/스케줄/경로/사이트/마지막 실행) |
| UC-3 | 스케줄 실행 (cron 자동 + 수동 실행) |
| UC-4 | 실행 이력 조회 (성공/실패, 건수, 시각) |
| UC-5 | 스케줄 수정 / 삭제 / 활성·비활성 |
| UC-6 | 실행 결과 → 뷰어에서 확인 |

### 1.5 설계 확정 방향 (2026-08-01 사용자 브리핑)

> 검색 기능을 기반으로 **SaaS형 개인 검색 스케줄링**을 지향. 요건을 다음과 같이 확정.

1. **SaaS화** — 각 사용자가 본인의 검색 조건을 스케줄로 등록 (계정 단위 격리)
2. **서버 경로 저장(1차)** — 스케줄 실행 결과를 서버의 스케줄 전용 경로에 MD 파일로 저장. (2차 확장: 개인 로컬 저장/동기화 고려)
3. **신규 공고만 MD 생성** — 이미 수집된(기존 저장된) 데이터를 기준으로 중복을 제거하고 **신규 공고만** 일자별 MD 파일에 기록 → "매일 같은 검색으로 신규 공고 확인"
4. **알림(확장성)** — 실행 결과를 이메일/카카오톡으로 알리는 것을 확장 포인트로 설계 (당장 미구현, 이벤트 기반 훅만 유지)
5. **뷰어** — 왼쪽 트리에 **스케줄별 경로** → **일자별 MD 파일**을 표시하고, 일자 파일을 클릭하면 해당 일자의 신규 공고를 테이블로 확인

### 1.6 계정 격리 설계 결정 (2026-08-01 확정)

> **SaaS/테넌트 개념: 로그인한 사용자만 본인의 스케줄을 보고 관리** — 우선 구현 대상 1순위.

| 항목 | 결정 |
|------|------|
| 소유자 식별 | auth JWT `sub`(userId) = `crawl_config.account_id` (auth DB의 users.id와 동일 값, 스크래퍼 DB는 FK 없이 BIGINT 저장) |
| 스크래퍼 인증 | scraper 백엔드에 JWT 검증 필터 추가(공용 `jwt.public-key`), 스케줄 API 전부 보호(미인증 401) |
| **MD 경로** | `{root}/{account_id}/{schedule_name}/{YYYY-MM-DD}.md` — root=`/home/ubuntu/data/scraper` (기존 `MarkdownParserService.DATA_DIR` 재사용) |
| localPath 생성 | 서버가 계정+이름으로 자동 생성 (`/home/ubuntu/data/scraper/{accountId}/{sanitized-name}`), 클라이언트 값 비신뢰 |
| JWT 키 관리 | **중앙 `.env`로 이전**: `JWT_PUBLIC_KEY` (single-line PEM). auth/scraper 모두 `${JWT_PUBLIC_KEY:기본값}` 참조, `.env` 미반영 시 기본값으로 동작 |
| 유니크 제약 | 전역 `uk_crawl_config_name(name)` → **`uk_crawl_config_account_name(account_id, name)`** 로 변경 (계정별 이름 중복 허용) |
| 기존 데이터 | `ALTER TABLE` 후 기존 행 `account_id=1`(첫 사용자)로 귀속 — 실제 첫 사용자 id 확인 후 UPDATE (스크래퍼 DB엔 users 테이블 없음) |
| 배포 주의 | scraper DB `ddl-auto: validate` → **DDL을 서버에서 먼저 실행 후 재시작** (순서: DDL → 배포) |
| 스케줄 실행기 | system job은 계정 무관(`findByIsActiveTrue`) 유지, API만 계정 필터 |

---

### 1.2 문서별 요건 원문 위치

| 문서 | 해당 절 | 핵심 내용 |
|------|---------|-----------|
| `docs/scraper/architecture-v2.md` | §4 스케줄링 흐름, §7.2 API | `crawl_schedule`(account_id, search_conditions JSON, cron_expression) + `crawl_schedule_log` 테이블, `/api/v1/schedules` CRUD+run+logs |
| `docs/REACT-FRONTEND-DESIGN-V3.md` | §2.2 스케줄 관리 | 등록 폼(시간/요일 UI+크론 미리보기), 카드 목록(수정/삭제/수동실행/결과보기), 수동실행 비동기+진행 표시 |
| `docs/OCI-PLATFORM-GUIDE.md` | 구현 상태 표 | 스케줄등록 ⚠️ 부분완료 — "기존 스케줄 상세 조회 필요" |
| `docs/common/modules.md` | Schedule API | `common_schedule_config`/`common_schedule_log` 공통 모듈 (moduleName, taskName, cron) |
| `docs/scraper/roadmap.md` | §3.2 스케줄러 | Cron 표현식 설정, 스케줄러 등록/해제 |

---

## 2. 현재 구현 (as-is)

### 2.1 데이터 모델 — `crawl_config` 기반 (문서의 `crawl_schedule` 아님)

- `crawl_config`: `id`, `name`(UNIQUE), `description`, `schedule`(기본 `0 9 * * *`), `retention_days`(30), `is_active`, `local_path`
- `crawl_site_config`: `config_id` FK, `site_definition_id` FK, `is_enabled`, `param_values`(JSON, `{"keyword":"Java","career":"3~5년",...}`)
- `crawl_log`: `config_id`, `site_definition_id`, `status`, `total_count`, `new_count`, `error_message`, `started_at`, `completed_at`
- (문서상 별도 테이블 `crawl_schedule`/`crawl_schedule_log`는 미생성)

### 2.2 백엔드

| 컴포넌트 | 내용 |
|----------|------|
| `CrawlExecutionService.executeScheduledCrawls()` | `@Scheduled(cron="${scraper.schedule.cron:0 9 * * *}")` **전역 단일 cron**으로 모든 `is_active=true` config 실행 |
| `CrawlExecutionService.executeCrawl(config)` | 사이트별 크롤링 → 중복(URL) 제거 → 일별 MD 파일 저장 → `crawl_data`/`crawl_log` 저장 → 알림 |
| API | `GET/POST /crawl-config`, `GET/PUT/DELETE /crawl-config/{id}`, `POST /crawl-config/{id}/execute`(비동기), `GET /crawl-logs/config/{id}/recent`, `GET /docs/crawlers` |

### 2.3 프론트 — `Schedule.tsx`

- 등록 폼: 이름/키워드/경력(슬라이더)/지역(멀티)/사이트/시간·요일(cron 미리보기)
- **저장은 `localStorage` (`schedule_${name}`)만 사용 — 백엔드 호출 없음**
- 목록: `/docs/crawlers`(백엔드 `crawl_config`) 기반 카드 표시
- 수정: `handleEdit`가 백엔드 카드 데이터를 localStorage로 저장 (백엔드 미반영)
- 삭제: `handleDelete`가 `localStorage.removeItem`만 수행 (백엔드 미반영)
- 수동 실행: `POST /crawl-config/{id}/execute` → alert 후 종료 (진행 상태 폴링 없음)

---

## 3. 문제점 / 오류 (gap 분석)

### 🔴 P0 — 저장과 실행의 단절 (근본 원인)

1. **프론트가 localStorage에만 저장** → 폼으로 등록한 스케줄은 백엔드에 없어 실제 크롤링이 절대 실행되지 않음.
2. **목록은 백엔드 데이터** → 새로 등록한 스케줄이 목록에 안 나타나고, 목록의 카드는 폼과 무관.
3. **수정/삭제는 localStorage 대상** → 화면의 카드(백엔드 데이터)를 수정/삭제해도 DB에 반영 안 됨. 카드의 ✏️/🗑️는 실질적으로 "아무 일도 안 일어남" 또는 이상 동작.

### 🔴 P0 — per-config cron 미사용

4. 실행 로직이 `CrawlConfig.schedule`을 **읽지 않음**. `@Scheduled`의 전역 `scraper.schedule.cron`(기본 매일 09:00) 하나로 모든 config이 동시 실행.
   → UI에서 시간/요일을 바꿔도 실행 시각에 전혀 영향 없음. cron 설정 기능이 유효하지 않음.

### 🟠 P1 — 문서와 실제 API/모델 불일치

5. 문서(`architecture-v2.md §7.2`)는 `/api/v1/schedules` CRUD를 명세하지만 실제 API는 `/crawl-config`+`/crawl-logs`. 어떤 쪽을 기준으로 할지 미결정.
6. 문서(`architecture-v2.md §4.2`)는 `crawl_schedule`+`search_conditions JSON` 모델이지만 실제는 `crawl_config`+`crawl_site_config.param_values`(사이트 단위). 현행 구조를 유지할지, 문서 모델로 전환할지 미결정.

### 🟠 P1 — 정보 누락 / UX

7. **마지막 실행 표시 없음**: `fetchCrawlLogs`(`/crawl-logs/config/{id}/recent`)가 `scraper.ts`에 정의만 되어 있고 카드에 미사용. V3 명세의 "마지막 실행: ... (360건)" 미구현.
8. **수동 실행 진행 표시 없음**: `alert` 하나로 끝. 폴링/상태 표시 미구현.
9. **상세 조회 없음**: 카드에 site 단위 `param_values`만 나열, 스케줄 상세 조회 미구현 (OCI 가이드 지적사항).
10. **새 검색조건 미반영**: 카드에 `careerMin/careerMax/locations` 표시 로직 없음. `param_values`에 새 조건을 저장하는 포맷도 미정.

### 🔴 P0 — 뷰어 API 미구현 (2026-08-01 확인)

15. **React 뷰어(`Viewer.tsx`/`FileTree.tsx`)가 호출하는 엔드포인트가 현재 백엔드에 없음** → 뷰어 전체 404:
    - `/docs/list?rootPath=` (FileTree.tsx:16)
    - `/docs/jobs?rootPath=&path=&site=&page=&size=` (Viewer.tsx:66)
    - (레거시 Alpine 뷰어 `templates/docs/viewer.html`도 `/docs/tree/view`, `/docs/file`, `/docs/search` 참조 — 역시 미구현)
    - 실제 백엔드에는 `/docs/crawlers`, `/docs/view`만 존재. 뷰어 요건(§4.5, §1.5-5)을 성립시키려면 **이 API를 신규 구현**해야 함. `MarkdownParserService`(MD→jobs 파싱 로직)를 재사용 가능.
16. **`/docs/crawlers` 응답 포맷이 신규 조건 미지원**: `paramValues`는 문자열 JSON 그대로라 `careerMin/careerMax/locations` 표시에 파싱 필요.

### 🟠 P1 — "신규 공고만" dedup 정확성 (사용자 핵심 요건 관련)

17. **lookback = 3일 하드코딩** (`CrawlExecutionService.DEDUP_LOOKBACK_DAYS=3`): 주 1회 스케줄이나 4일 이전 등장 공고는 중복으로 재집계됨. 주기에 맞는 lookback(또는 전사 URL dedup) 필요.
18. **일자 파일 덮어쓰기**: 같은 날짜 `YYYY-MM-DD.md`를 항상 `Files.writeString`으로 덮어씀 → 하루 2회 실행 시 1회차 신규 공고가 소실. dedup 조회에 "오늘 파일" 포함 + append, 또는 실행 주기 1일 제한 필요.
19. **중복 재발생**: 공고가 사이트에서 사라졌다 재등록되면 다시 "신규"로 잡힘. `crawl_data.source_url` 전사(전체기간) dedup이 정확하나 현재는 MD 파일 기반 dedup만.

### 🟡 P2 — 구조적 미결

11. **계정 격리 없음**: `architecture-v2.md`는 `account_id` 기반 격리 명시, 현재는 전역 config (인증 자체가 없음). 적용 여부 결정 필요.
12. **수동 실행 동시성**: 같은 config를 동시에 두 번 실행하면 중복/경합 발생 가능. 실행중 상태 관리 부재.
13. **`retention_days` 미사용**: 값만 있고 정리(삭제) 로직 없음.
14. **`CrawlConfigService.updateConfig` 불완전**: `localPath`/`siteConfigs` 미갱신 (수정 시 사이트 조건 변경 불가).

---

## 4. 목표 요건 (to-be)

### 4.1 저장: 프론트 → 백엔드 직접 CRUD

- 프론트 저장/수정/삭제를 전부 백엔드 API로 변경, **localStorage 제거** (하위호환으로 읽기만 병합 가능).
- 검색조건은 사이트 단위 `param_values` JSON에 저장:
  ```json
  { "keyword": "Java", "careerMin": 3, "careerMax": 5, "locations": ["서울", "경기"] }
  ```
  (기존 `career`/`location` 문자열은 하위호환 파서 유지)

### 4.2 실행: per-config cron 지원 + 신규 공고 dedup 확정

- `CrawlExecutionService`를 전역 cron → **config별 cron** 기반으로 변경:
  - `@Scheduled(cron="0 * * * * *")` 매분 폴링(또는 cron 트리거 등록)으로 `is_active=true` config의 `schedule`과 매칭 시에만 실행
  - `lastRunAt`(미실행 보장) 추가하여 같은 분에 중복 실행 방지
- `schedule` 저장 포맷은 5필드 cron (`분 시 일 월 요일`), 프론트 시간/요일 UI → cron 변환(`toCron`) 유지.
- **신규 공고 판정(§1.5-3 확정)**: "이전에 저장된 MD에 없는 URL = 신규"
  - dedup lookback을 `DEDUP_LOOKBACK_DAYS`(현재 3일) 대신 **`retention_days`(기본 30일)** 또는 전체 기간으로 조정 (스케줄 주기보다 길어야 함)
  - 일자 파일은 덮어쓰기 대신 **append** 또는 "오늘 파일도 dedup 조회에 포함" → 하루 2회 실행에도 신규 공고 소실 방지
  - 옵션(정밀화): `crawl_data.source_url` 전사 dedup (DB 조회)

### 4.3 저장 경로 (SaaS형)

- 경로 스키마: `/data/{owner}/{schedule_name}/{YYYY-MM-DD}.md` (owner = 현재는 단일 관리자, 계정 도입 시 `account_id`/이메일)
- `crawl_config.local_path` = `{owner}/{schedule_name}` 로 설정, 뷰어 tree root로 사용
- 2차 확장(개인 로컬 저장)은 이 경로 기준 다운로드/동기화로 전환 가능하게 설계

### 4.4 API (현행 `/crawl-config` 기준, 문서 `/schedules`와 정합 결정 후 반영)

| 기능 | 현재 | 목표 |
|------|------|------|
| 목록 | GET `/crawl-config` | 동일 + 최근 실행 로그 포함 |
| 생성 | POST `/crawl-config` | 동일 (siteConfigs + localPath 포함) |
| 수정 | PUT `/crawl-config/{id}` | `localPath`/`siteConfigs` 갱신 지원 |
| 삭제 | DELETE `/crawl-config/{id}` | 동일 (연관 cascade 확인) |
| 수동 실행 | POST `/crawl-config/{id}/execute` | 동일 + 실행중 상태 반환 |
| 이력 | GET `/crawl-logs/config/{id}/recent` | 카드 "마지막 실행"에 사용 |
| **파일 트리(신규)** | GET `/docs/list?rootPath=` | 스케줄 경로 → 일자 MD 트리 |
| **일자별 공고(신규)** | GET `/docs/jobs?rootPath=&path=&site=&page=&size=` | 일자별 신규 공고 테이블(페이징/사이트 필터) |

### 4.5 뷰어 (설계 확정 반영)

- 좌측: **스케줄 목록**(`/docs/crawlers`) → 선택 시 **해당 스케줄 경로의 파일 트리**(`/docs/list`) → 일자별 MD (`YYYY-MM-DD.md`)
- 일자 MD 클릭 → 해당 일자 **신규 공고만** 테이블 표시 (`/docs/jobs`, 사이트 탭/페이징)
- V3 레이아웃(`REACT-FRONTEND-DESIGN-V3.md §2.2`) 기준, 기존 `Viewer.tsx`/`FileTree.tsx` 재사용
- 현재 누락된 백엔드 엔드포인트(`/docs/list`, `/docs/jobs`)를 `MarkdownParserService` 기반으로 **신규 구현**이 선행 필요

### 4.6 알림 (확장 포인트만 유지)

- 실행 완료 후 `notificationService.sendNotification("scraper", "new_jobs_found", msg)` 호출 유지 (CrawlExecutionService.java:126-136 이미 존재)
- 채널 확장(메일 SMTP, 카카오톡)은 common `NotificationConfig`(moduleName/eventType/notificationType/recipientEmail)와 연결 예정 — 당장 구현 범위 아님

### 4.7 검증 시나리오

1. Search 조건 → 스케줄 등록 → 목록에 나타남 → 수동 실행 → 뷰어에 결과 확인
2. cron을 "다음 2분"으로 설정 → 자동 실행 확인 → `crawl_log` 기록 확인
3. 수정(키워드/시간) → DB 반영 확인
4. 삭제 → DB에서 제거 확인 (cascade로 site/log 처리)
5. 수동 실행 연타 → 중복 실행 방지 확인

---

## 5. 결정 필요 항목 (open questions)

| # | 질문 | 선택지 |
|---|------|--------|
| Q1 | API/모델 기준 | ① 현행 `crawl_config` 유지 (추천: 뷰어·실행기와 이미 정합) ② 문서대로 `crawl_schedule` 신설 |
| Q2 | per-config cron 구현 방식 | ① 매분 폴링+문자열 cron 파싱 (추천: 단순) ② `DynamicScheduler`/`CronTrigger` 동적 등록 |
| Q3 | 계정 격리 | ① 유지보류(현재 1인, 인증 미도입) ② `account_id` 컬럼 추가 → SaaS 첫 단계 |
| Q4 | 실행중 상태 관리 | ① DB `is_running` 플래그 ② 인메모리 상태 |
| Q5 | retention 정리 | ① 백그라운드 스케줄(매일) ② 수동 정리 버튼 |
| Q6 | "신규" dedup 기준 | ① MD lookback(3일→30일) ② `crawl_data.source_url` 전사 dedup |
| Q7 | 일자 파일 쓰기 | ① 덮어쓰기 + 오늘 파일 dedup 포함 ② append 모드 |
| Q8 | 뷰어 API 신규 구현 범위 | ① `/docs/list`+`/docs/jobs`만 ② 레거시(`/docs/tree/view`,`/docs/file`,`/docs/search`) 포함 → 기존 viewer.html 폐기 여부 |

---

## 6. 관련 파일

- 백엔드: `modules/scraper/backend/src/main/java/com/scraper/platform/service/CrawlExecutionService.java`, `CrawlConfigService.java`, `MarkdownParserService.java`, `controller/CrawlConfigController.java`, `CrawlExecutionController.java`, `CrawlLogController.java`, `controller/CrawlerListController.java`, `model/CrawlConfig.java`
- 프론트: `modules/scraper/frontend/src/pages/Schedule.tsx`, `pages/Viewer.tsx`, `pages/Search.tsx`, `components/FileTree.tsx`, `api/scraper.ts`, `types/index.ts`
- 문서: `docs/scraper/architecture-v2.md`, `docs/REACT-FRONTEND-DESIGN-V3.md`, `docs/OCI-PLATFORM-GUIDE.md`, `docs/common/modules.md`, `docs/scraper/roadmap.md`
