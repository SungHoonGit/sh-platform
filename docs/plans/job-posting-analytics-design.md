# 채용공고 모니터링 시스템 설계 문서

## 1. 개요

### 1.1 목적
- 채용공고 수집 데이터를 기반으로 시장 트렌드 분석
- 실시간 수집 현황 모니터링
- AI 기반 인사이트 도출 (확장)

### 1.2 용어 정의
| 기존 용어 | 변경 용어 | 이유 |
|-----------|-----------|------|
| 크롤링 (Crawling) | 공고 수집 | 법적 이슈, 부정적 인식 |
| 크롤러 (Crawler) | 수집기 | |
| 크롤링 로그 | 수집 로그 | |

---

## 2. 벤치마킹 분석

### 2.1 국내 채용 플랫폼

| 플랫폼 | MAU | 주요 기능 | 차별화 포인트 |
|--------|-----|-----------|--------------|
| 사람인 | 921만 | AI 인재 추천, 공고 코칭, 세밀한 필터 | 가장 많음 |
| 잡코리아 | 761만 | 공채 달력, AI 추천, 알바몬/긱몬 연계 | 다양한 서비스 |
| 리멤버 | 506만 | 연봉 1억+ 공고만 모은 '블랙', 프리미엄 채용 | 명함 데이터 기반 |
| 잡플래닛 | 152만 | 기업 리뷰 + 연봉 비교, 프라이빗 채용관 | 55만건 연봉 데이터 |
| 원티드 | 61만 | AI 매칭 (900만건 이력서-공고 매칭) | IT 특화 |

### 2.2 글로벌 플랫폼

| 플랫폼 | 주요 기능 | 특징 |
|--------|-----------|------|
| Indeed | Hiring Insights (시장 트렌드), 검색어 기반 공고 최적화 | 글로벌 95.5% 도달 |
| LinkedIn | AI 추천, Open to Work 뱃지, 10명 미만 지원 필터 | 면접 전환율 2배 |

### 2.3 주요 인사이트

1. **공고의 구조가 변하고 있음**
   - 전체 채용 인원은 증가 중
   - 신입 공고 급감 (대기업 정규직 신입 43% 감소)
   - 수시채용 확산 (54.8%)

2. **AI 영향**
   - IT·통신 업종 신입 공고 감소
   - 채용 과정에서 AI 활용 증가 (30.6%)

---

## 3. 수집 데이터 분석 가능 인사이트

### 3.1 기본 분석 (Phase 1)

| 인사이트 | 데이터 소스 | 시각화 | 구현 난이도 |
|----------|-------------|--------|-------------|
| 일별 신규 공고 수 | `crawled_at` | Bar Chart | 하 |
| 사이트별 수집 비율 | `site_name` | Pie Chart | 하 |
| 공고 生命周期 (등록~마감) | `created_at`, `deadline` | Timeline | 중 |
| 평균 마감 기간 | `deadline` - `created_at` | 통계 카드 | 중 |
| 직무별 공고 분포 | `position` | Treemap | 중 |

### 3.2 심층 분석 (Phase 2)

| 인사이트 | 설명 | 활용 | 데이터 필요 |
|----------|------|------|-------------|
| 직무별 수요 추세 | 기술 스택별 공고 증감 | 채용 시장 트렌드 파악 | `tech` 필드 |
| 지역별 분포 | 지역별 공고 밀도 | 원격근무 추세 분석 | `location` 필드 |
| 경력별 요구사항 | 신입 vs 경력 비율 | 취업 전략 수립 | `career` 필드 |
| 사이트별 중복률 | 동일 공고의 사이트 분포 | 플랫폼별 비교 | `dedup_key` |

### 3.3 추가 수집 필요한 데이터

| 필드 | 현재 상태 | 필요성 | 이유 |
|------|------------|--------|------|
| `published_at` | 미수집 | 높음 | 공고 등록일 (최초 게시일) |
| `salary_min/max` | 미수집 | 중 | 연봉 범위 추출 |
| `company_size` | 미수집 | 중 | 기업 규모별 분석 |
| `employment_type` | 미수집 | 중 | 정규직/계약직/인턴 비율 |
| `work_type` | 미수집 | 높음 | 원격/하이브리드/출퇴근 |

### 3.4 AI RAG 분석 (Phase 3 - 확장)

| 인사이트 | 방법 | 결과 |
|----------|------|------|
| 공고 패턴 분석 | NLP로 키워드 추출 | 핫 기술 스택 트렌드 |
| 이상 탐지 | 시계열 분석 | 갑작스러운 수요 급증/급감 |
| 예측 모델 | 과거 데이터 기반 | 향후 채용 전망 |
| 기업 분석 | 공고 빈도, 요구사항 | 기업별 채용 성향 |

---

## 4. 시스템 설계

### 4.1 메뉴 구조

```
SH Platform
├── 📊 통합 검색 (기존)
├── 📅 스케줄 관리 (수정)
│   ├── 스케줄 카드
│   │   ├── 기본 정보
│   │   ├── [수정] [삭제] [▶ 실행]
│   │   └── 최근 실행 3건 (간략)
│   └── [전체 수집 로그 보기] → /logs
│
└── 📋 수집 로그 (신규)
    ├── 필터 바
    │   ├── 설정별 드롭다운
    │   ├── 상태별 (전체/성공/실패)
    │   ├── 기간 선택 (7일/30일/전체)
    │   └── 검색
    │
    ├── 통계 대시보드 (상단)
    │   ├── 카드1: 총 실행 횟수
    │   ├── 카드2: 성공률 (%)
    │   ├── 카드3: 총 수집 건수
    │   └── 카드4: 신규 수집 건수
    │
    ├── 차트 영역
    │   ├── 최근 30일 일별 신규 수집 건수 (Bar Chart)
    │   └── 설정별 수집 비교 (Pie Chart)
    │
    ├── 로그 테이블
    │   ├── 시간 | 설정명 | 사이트 | 상태 | 전체 | 신규 | 중복 | 소요시간
    │   └── 페이징
    │
    └── [확장 영역] (추후)
        └── AI 분석: 수집 추세 분석, 이상 탐지
```

### 4.2 백엔드 API (추가 필요)

| 엔드포인트 | 용도 | Method |
|-----------|------|--------|
| `/crawl-logs/recent-all` | 전체 최근 로그 | GET |
| `/crawl-logs/stats` | 통계 (총 실행, 성공률 등) | GET |
| `/crawl-logs/trend` | 일별 추세 (최근 30일) | GET |
| `/crawl-logs/by-site` | 사이트별 통계 | GET |

### 4.3 프론트엔드 컴포넌트

| 컴포넌트 | 설명 | 위치 |
|----------|------|------|
| `LogDashboard` | 통계 카드 | /logs |
| `LogChart` | 차트 (recharts) | /logs |
| `LogTable` | 페이징 테이블 | /logs |
| `LogFilter` | 필터 바 | /logs |
| `ScheduleRecentLogs` | 최근 실행 3건 | /schedule |

### 4.4 기술 스택

| 항목 | 선택 | 이유 |
|------|------|------|
| 차트 | `recharts` | 가벼움, React 네이티브 |
| 상태 관리 | React Query | 서버 상태 캐싱 |
| 테이블 | TanStack Table | 기능 풍부 |

---

## 5. 구현 로드맵

### Phase 1: 기본 수집 + 로그 뷰어 (2주)
- [ ] 백엔드 로그 API 추가
- [ ] 스케줄 페이지에 최근 실행 3건 표시
- [ ] `/logs` 페이지 기본 구조
- [ ] 로그 테이블 + 페이징

### Phase 2: 심층 분석 + 차트 (2주)
- [ ] 통계 대시보드
- [ ] 차트 (Bar, Pie)
- [ ] 필터 기능 고도화
- [ ] 기간별 분석

### Phase 3: AI RAG 통합 (1개월)
- [ ] 데이터 전처리 파이프라인
- [ ] AI 분석 모델 개발
- [ ] 이상 탐지 알림
- [ ] 예측 모델

---

## 6. DB 변경 사항

### 6.1 추가 컬럼 (job_postings)

```sql
ALTER TABLE job_postings
    ADD COLUMN published_at DATE COMMENT '공고 등록일',
    ADD COLUMN salary_min INT COMMENT '최소 연봉',
    ADD COLUMN salary_max INT COMMENT '최대 연봉',
    ADD COLUMN company_size VARCHAR(50) COMMENT '기업 규모 (대기업/중견/소기업)',
    ADD COLUMN employment_type VARCHAR(50) COMMENT '고용형태 (정규직/계약직/인턴)',
    ADD COLUMN work_type VARCHAR(50) COMMENT '근무형태 (출퇴근/원격/하이브리드)';
```

### 6.2 색인 추가

```sql
CREATE INDEX idx_job_postings_published ON job_postings(published_at);
CREATE INDEX idx_job_postings_salary ON job_postings(salary_min, salary_max);
```

---

## 7. 참고 자료

- [2026년 채용시장 분석 (KBR)](https://www.koreabizreview.com)
- [Indeed Analytics & Hiring Insights](https://www.indeed.com/hire/resources/howtohub/indeed-analytics-and-hiring-insights)
- [LinkedIn Jobs Features](https://bestjobsearchapps.com)
- [Lightcast Job Posting Analytics](https://kb.lightcast.io)

---

*작성일: 2026-08-04*
*작성자: AI Assistant*
