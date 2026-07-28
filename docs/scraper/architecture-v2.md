# Scraper Platform 아키텍처

> 실시간 검색 + 스케줄 기반 데이터 축적 + 고객별 독립 구조
> 작성일: 2026-07-28

---

## 1. 핵심 개념

### 1.1 두 가지 동작

| 동작 | 설명 | 트리거 |
|------|------|--------|
| **실시간 검색** | 사용자가 검색 조건 입력 → 즉시 사이트 크롤링 → 결과 반환 | 사용자 액션 |
| **스케줄링** | 사용자가 "이 조건으로 매일 돌려줘" 등록 → 주기적 크롤링 → MD 파일 축적 | Cron |

### 1.2 데이터 흐름

```
[실시간 검색]
  사용자 → 검색 조건 입력 → SiteSearchMapper → 크롤러 실행 → 실시간 결과 반환
                                                        ↓
                                               (검색 결과 표시)

[스케줄링 등록]
  사용자 → "이 조건으로 스케줄 등록" → CrawlConfig 저장 (cron + 검색 조건)
                                                        ↓
[스케줄 실행]
  Cron → CrawlConfig 조회 → 크롤러 실행 → 결과 MD 파일 저장 → (확장: 메일 발송)
```

### 1.3 고객별 독립 구조

```
고객 A (account_id=1)
├── 검색 조건: {"keyword":"Java","career":"3~5년","location":"서울"}
├── 스케줄: 매일 09:00
├── MD 파일: /data/account-1/java/2026-07-28.md
└── 알림: 이메일 발송

고객 B (account_id=2)
├── 검색 조건: {"keyword":"React","career":"신입","location":"전체"}
├── 스케줄: 매주 월요일 10:00
├── MD 파일: /data/account-2/react/2026-07-28.md
└── 알림: 카카오톡 발송
```

---

## 2. 시스템 아키텍처

```
┌─────────────────────────────────────────────────────────┐
│                    프론트엔드 (React)                      │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌─────────┐ │
│  │ 실시간   │  │ 스케줄   │  │ 검색     │  │ 마이    │ │
│  │ 검색     │  │ 관리     │  │ 기록     │  │ 페이지  │ │
│  └────┬─────┘  └────┬─────┘  └────┬─────┘  └────┬────┘ │
└───────┼──────────────┼──────────────┼──────────────┼─────┘
        │              │              │              │
        v              v              v              v
┌─────────────────────────────────────────────────────────┐
│                 백엔드 API (Spring Boot)                  │
│  ┌──────────────┐  ┌──────────────┐  ┌───────────────┐ │
│  │ SearchController│ │ ScheduleController│ │ ConfigController│ │
│  │  - 실시간 검색│  │  - 스케줄 CRUD │  │  - 검색 조건  │ │
│  └──────┬───────┘  └──────┬───────┘  └───────┬───────┘ │
│         │                 │                   │         │
│         v                 v                   v         │
│  ┌──────────────────────────────────────────────────┐  │
│  │              핵심 서비스 레이어                     │  │
│  │  ┌──────────────┐  ┌──────────────┐              │  │
│  │  │SiteSearchMapper│ │CrawlerFactory│              │  │
│  │  │  - 표준→사이트 변환│ │  - 크롤러 선택│              │  │
│  │  └──────────────┘  └──────────────┘              │  │
│  └──────────────────────────────────────────────────┘  │
│         │                                               │
│         v                                               │
│  ┌──────────────────────────────────────────────────┐  │
│  │              크롤러 (4개 사이트)                    │  │
│  │  SaraminCrawler │ JobkoreaCrawler │              │  │
│  │  WantedCrawler  │ RememberCrawler │              │  │
│  └──────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────┘
        │
        v
┌─────────────────────────────────────────────────────────┐
│                    데이터 저장소                          │
│  ┌──────────┐  ┌──────────┐  ┌──────────────────────┐  │
│  │ MariaDB  │  │ MD 파일  │  │ 알림 서비스           │  │
│  │ - 설정   │  │ - 일별   │  │ - 이메일 (확장)      │  │
│  │ - 이력   │  │ - 고객별 │  │ - 카카오톡 (확장)    │  │
│  └──────────┘  └──────────┘  └──────────────────────┘  │
└─────────────────────────────────────────────────────────┘
```

---

## 3. 실시간 검색 흐름

### 3.1 API

```
POST /scraper/api/v1/search
{
    "keyword": "Java",
    "career": "3~5년",
    "location": "서울",
    "sites": ["saramin", "jobkorea", "wanted", "remember"]
}
```

### 3.2 처리 흐름

```
1. SearchController에서 검색 요청 수신
2. SiteSearchMapper.toSiteParams()로 사이트별 파라미터 변환
   - saramin: {"stext":"Java","career_level":"5","loc_cd":"101000"}
   - jobkorea: {"stext":"Java","careerType":"career","local":"I000"}
   - wanted: {"query":"Java","years":"3","locations":"seoul"}
   - remember: {"query":"Java","min_experience":"3","sido":"서울"}
3. 각 크롤러를 병렬로 실행
4. 결과를 통합하여 반환
5. (선택) 검색 기록 DB에 저장
```

### 3.3 응답

```json
{
    "total": 156,
    "jobs": [
        {
            "site": "saramin",
            "company": "네이버",
            "position": "Java 백엔드 개발자",
            "career": "3~5년",
            "location": "서울",
            "url": "https://saramin.co.kr/...",
            "deadline": "2026-08-15"
        }
    ],
    "searchTime": "2.3초"
}
```

---

## 4. 스케줄링 흐름

### 4.1 스케줄 등록

```
POST /scraper/api/v1/schedules
{
    "name": "Java 시니어 채용 모니터링",
    "keyword": "Java",
    "career": "3~5년",
    "location": "서울",
    "sites": ["saramin", "jobkorea"],
    "cron": "0 9 * * *",
    "notification": {
        "type": "email",
        "target": "user@example.com"
    }
}
```

### 4.2 데이터 모델

```sql
-- 스케줄 설정
CREATE TABLE crawl_schedule (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    account_id BIGINT NOT NULL COMMENT '고객 ID',
    name VARCHAR(100) NOT NULL COMMENT '스케줄명',
    search_conditions JSON NOT NULL COMMENT '검색 조건 {"keyword":"Java","career":"3~5년"}',
    cron_expression VARCHAR(100) NOT NULL COMMENT 'Cron 표현식',
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (account_id) REFERENCES account(id) ON DELETE CASCADE
);

-- 스케줄 실행 이력
CREATE TABLE crawl_schedule_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    schedule_id BIGINT NOT NULL,
    status ENUM('success', 'failed', 'running') NOT NULL,
    total_count INT DEFAULT 0,
    new_count INT DEFAULT 0,
    file_path VARCHAR(500) COMMENT '저장된 MD 파일 경로',
    started_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP NULL,
    FOREIGN KEY (schedule_id) REFERENCES crawl_schedule(id) ON DELETE CASCADE
);
```

### 4.3 실행 흐름

```
1. Cron 트리거
2. 활성화된 스케줄 조회 (account_id별)
3. 각 스케줄의 search_conditions로 SiteSearchMapper 변환
4. 크롤러 실행
5. 결과를 일별 MD 파일로 저장
   - 경로: /data/{account_id}/{YYYY-MM-DD}.md
6. (확장) 알림 발송
   - 이메일: 축적된 MD 파일 첨부
   - 카카오톡: 요약 전송
```

---

## 5. 고객별 독립 구조

### 5.1 데이터 격리

```
/data/
├── account-1/                    # 고객 A
│   ├── 2026-07-28.md            # 일별 축적 데이터
│   ├── 2026-07-27.md
│   └── ...
├── account-2/                    # 고객 B
│   ├── 2026-07-28.md
│   └── ...
└── account-3/                    # 고객 C
    └── ...
```

### 5.2 검색 조건 격리

각 고객은 자기만의 검색 조건을 가짐:
- 고객 A: Java 시니어 (서울)
- 고객 B: React 신입 (전체)
- 고객 C: Python 중급 (경기)

### 5.3 스케줄 격리

각 고객은 자기만의 스케줄을 가짐:
- 고객 A: 매일 09:00
- 고객 B: 매주 월요일 10:00
- 고객 C: 격주 금요일 14:00

---

## 6. 확장: 이메일 발송

### 6.1 흐름

```
스케줄 실행 완료
    ↓
MD 파일 생성
    ↓
이메일 템플릿 렌더링
    ↓
SMTP 발송
    ↓
발송 이력 저장
```

### 6.2 이메일 템플릿

```html
<h2>채용공고 모니터링 리포트</h2>
<p>기간: 2026-07-28 | 검색 조건: Java 3~5년 서울</p>

<table>
  <tr><th>사이트</th><th>회사</th><th>포지션</th><th>경력</th></tr>
  <tr><td>사람인</td><td>네이버</td><td>Java 백엔드</td><td>3~5년</td></tr>
  <tr><td>잡코리아</td><td>카카오</td><td>서버 개발</td><td>3~5년</td></tr>
</table>

<p>총 15건의 새로운 채용공고가 발견되었습니다.</p>
<a href="https://sunghoonyk.duckdns.org/scraper/...">전체 보기</a>
```

---

## 7. API 엔드포인트

### 7.1 실시간 검색

| 메서드 | 경로 | 설명 |
|--------|------|------|
| POST | `/scraper/api/v1/search` | 실시간 검색 (크롤링 포함) |
| GET | `/scraper/api/v1/search/history` | 검색 기록 조회 |

### 7.2 스케줄 관리

| 메서드 | 경로 | 설명 |
|--------|------|------|
| GET | `/scraper/api/v1/schedules` | 스케줄 목록 |
| POST | `/scraper/api/v1/schedules` | 스케줄 등록 |
| PUT | `/scraper/api/v1/schedules/{id}` | 스케줄 수정 |
| DELETE | `/scraper/api/v1/schedules/{id}` | 스케줄 삭제 |
| POST | `/scraper/api/v1/schedules/{id}/run` | 스케줄 수동 실행 |
| GET | `/scraper/api/v1/schedules/{id}/logs` | 실행 이력 |

### 7.3 검색 조건 관리

| 메서드 | 경로 | 설명 |
|--------|------|------|
| GET | `/scraper/api/v1/conditions` | 검색 조건 목록 |
| POST | `/scraper/api/v1/conditions` | 검색 조건 저장 |
| GET | `/scraper/api/v1/conditions/{id}/sites` | 사이트별 파라미터 미리보기 |

---

## 8. 구현 단계

| 단계 | 내용 | 상태 |
|------|------|------|
| 1 | 검색 파라미터 표준화 (SiteSearchMapper) | ✅ 완료 |
| 2 | 크롤러별 URL 파라미터 적용 | ✅ 완료 |
| 3 | 실시간 검색 API 구현 | 🔜 예정 |
| 4 | 스케줄 관리 API + DB | 🔜 예정 |
| 5 | 프론트엔드 실시간 검색 UI | 🔜 예정 |
| 6 | 프론트엔드 스케줄 관리 UI | 🔜 예정 |
| 7 | 이메일 발송 서비스 | 🔜 예정 |
| 8 | 고객별 데이터 격리 | 🔜 예정 |

---

## 9. 기존 구조와의 차이

### 9.1 이전 (문제)

```
크롤러(스케줄) → MD 파일 저장 → 검색 페이지에서 읽기 → 클라이언트 필터링
```

- 검색 = 이미 크롤링된 MD 파일 조회
- 파라미터가 크롤러 URL에 반영 안 됨
- 고객별 격리 없음

### 9.2 이후 (목표)

```
[실시간 검색] 사용자 → 즉시 크롤링 → 결과 반환
[스케줄링] 사용자 등록 → 주기적 크롤링 → MD 축적 → (확장: 메일)
[고객별] 각 계정 독립 → 조건/스케줄/데이터 격리
```

- 검색 = 실시간 크롤링
- 파라미터가 크롤러 URL에 반영됨
- 고객별 완전한 격리
