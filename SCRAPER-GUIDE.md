---
title: Scraper Guide
description: Scraper Guide - scraper module documentation
category: scraper
created: 2026-07-16
updated: 2026-07-21
---

# Scraper Platform Guide

> Java 기반 채용공고 수집 플랫폼. 사용자 설정 기반 자동 스케줄 크롤링 + 웹 뷰어.

## 아키텍처

```
사용자 → [Viewer UI] → [FileController] → MD 파일
                ↕
        [CrawlExecutionService] → [CrawlerFactory] → [SaraminCrawler/JobkoreaCrawler/WantedCrawler]
                ↕
        [crawl_config] → [crawl_site_config] → [site_definition]
                ↕
        /home/ubuntu/job-scraper/daily/{profile}/{YYYY-MM-DD}.md
```

## 서비스 정보

| 항목 | 값 |
|------|-----|
| 포트 | 8081 |
| Swagger | `https://sunghoonyk.duckdns.org/scraper/swagger-ui/index.html` |
| 뷰어 | `https://sunghoonyk.duckdns.org/scraper/docs/view` |
| 크롤링 설정 API | `https://sunghoonyk.duckdns.org/scraper/crawl-config` |

## 크롤러 구조

### SiteCrawler 인터페이스

```java
public interface SiteCrawler {
    List<Map<String, String>> search(CrawlSiteConfig siteConfig) throws Exception;
    String buildMdSection(List<Map<String, String>> jobs, String displayName);
}
```

### 구현체

| 크롤러 | 사이트 | 방법 | 한도 |
|--------|--------|------|------|
| SaraminCrawler | 사람인 | curl + Jsoup (anti-bot 우회) | 3페이지 × 50 = 150건 |
| JobkoreaCrawler | 잡코리아 | curl + Jsoup | 60건 |
| WantedCrawler | 원티드 | REST API (Java HttpClient) | 5페이지 = 100건 |

### CrawlerFactory

Spring 자동 주입으로 `List<SiteCrawler>`를 받아 사이트명 → 구현체 매핑.

## DB 구조

### 핵심 테이블

```
site_definition (6개)
  └─ saramin, jobkorea, wanted, jumpit, incruit, remember

crawl_config
  ├─ id: 1 (java_daily)
  └─ id: 3 (react_daily)

crawl_site_config
  ├─ config_id=1 → saramin, jobkorea, wanted (사람인/잡코리아/원티드)
  └─ config_id=3 → saramin, jobkorea, wanted (react_daily)
```

### 현재 등록된 설정

#### java_daily (ID: 1)

| 사이트 | 키워드 | 경력 | 지역 |
|--------|--------|------|------|
| 사람인 | Java 백엔드 | 3~5년 | 서울 |
| 잡코리아 | Java Spring | 3~5년 | 서울 |
| 원티드 | Java 백엔드 | 3~5년 | 서울 |

- 스케줄: `0 9 * * *` (매일 09:00)
- 출력 경로: `/home/ubuntu/job-scraper/daily/java/`
- 활성: ✅

#### react_daily (ID: 3)

| 사이트 | 키워드 | 경력 | 지역 |
|--------|--------|------|------|
| 사람인 | React 프론트엔드 | 1~3년 | 서울 |
| 잡코리아 | React | 1~3년 | 서울 |
| 원티드 | React 프론트엔드 | 1~3년 | 서울 |

- 스케줄: `0 9 * * *` (매일 09:00)
- 출력 경로: `/home/ubuntu/job-scraper/daily/react-java/`
- 활성: ✅

### param_values 필드

```json
{
  "keyword": "React 프론트엔드",
  "career": "1~3년",
  "location": "서울",
  "job_type": "개발"
}
```

**지원 career 값:** 신입, 경력, 1~3년, 3~5년, 5~10년, 10년이상

## 크롤링 실행

### 자동 스케줄

- 크론: `0 9 * * *` (매일 09:00)
- `CrawlExecutionService.executeScheduledCrawls()` → 활성 설정 전체 실행

### 수동 실행

```bash
# API 호출
curl -X POST https://sunghoonyk.duckdns.org/scraper/crawl-config/1/execute

# 뷰어에서
크롤러 선택 → ▶ 실행 버튼 클릭
```

- **비동기 실행**: 즉시 `{"status": "started"}` 반환, 백그라운드에서 크롤링
- **실행 상태**: 뷰어에서 폴링 (3초 간격) → 완료 시 `완료: N건 수집` 표시

### Deduplication

- 이전 3일치 MD 파일에서 URL 추출
- 이미 수집된 URL은 제외
- 헤더에 통계 표시: `> 총 N건 (HH:MM 기준) | 신규 N건, 중복 N건 제외`

### 출력 형식

```markdown
# 2026-07-16 React 프론트엔드 채용공고

> 총 230건 (00:49 기준) | 신규 230건, 중복 80건 제외

## 사람인 (150건)

### (주)코아아이티
- 포지션: 유지보수 SI개발자 채용
- 경력: 경력 3년↑ · 정규직
- 기술: 웹개발, 유지보수, SI개발, RDBMS, S/W
- 지역: 충남 천안시 동남구
- 마감: 채용시
- 링크: https://www.saramin.co.kr/...
```

## API 명세

### 크롤링 설정

| Method | URL | 설명 |
|--------|-----|------|
| GET | `/crawl-config` | 전체 설정 조회 |
| GET | `/crawl-config/active` | 활성 설정 조회 |
| GET | `/crawl-config/{id}` | 설정 상세 (site_configs 포함) |
| POST | `/crawl-config/{id}/execute` | 수동 실행 |
| GET | `/crawl-config/{id}/site-configs` | 사이트별 설정 조회 |

### 문서 뷰어

| Method | URL | 설명 |
|--------|-----|------|
| GET | `/docs/crawlers` | 크롤러 목록 |
| GET | `/docs/tree?rootPath=...` | 파일 트리 |
| GET | `/docs/file?rootPath=...&path=...` | 파일 내용 (HTML/Raw) |
| GET | `/docs/jobs?rootPath=...&path=...&site=...&page=0&size=20` | 채용공고 JSON (페이징) |
| GET | `/docs/search?rootPath=...&q=...` | 전체 검색 |
| GET | `/docs/export/pdf?rootPath=...&path=...` | PDF 내보내기 |
| GET | `/docs/export/excel?rootPath=...&path=...` | Excel 내보내기 |

### 뷰어 기능

- **사이트 탭**: 전체 / 사람인 / 잡코리아 / 원티드 + 건수 뱃지
- **그리드 테이블**: 회사명, 포지션, 경력, 기술, 지역, 마감, 링크
- **컬럼 정렬**: 헤더 클릭 → 오름/내림 정렬 (마감일은 D-1 우선)
- **빠른 필터**: 회사명/포지션/기술 즉시 검색
- **PDF/Excel 내보내기**
- **수동 실행**: `▶ 실행` 버튼 → 백그라운드 크롤링 → 완료 시 자동 새로고침

## 파일 구조

```
/home/ubuntu/job-scraper/daily/
├── java/                    # java_daily 출력
│   ├── 2026-07-15.md        # Java Spring 채용공고
│   └── 2026-07-16.md
├── react-java/              # react_daily 출력
│   └── 2026-07-16.md        # React 프론트엔드 채용공고
├── python-java/             # Python 스크래퍼 (이전)
│   ├── 2026-07-08.md
│   └── ... (07-08 ~ 07-14)
└── react/                   # Python React (이전)
    └── 2026-07-08.md
```

## 문제 해결

### 사람인 anti-bot

- **문제**: Jsoup Java HTTP → bot 감지, 빈 HTML 반환
- **해결**: `ProcessBuilder` + `curl`로 HTML 수집 → Jsoup으로 파싱

### CORS 에러

- **문제**: 브라우저에서 POST 요청 차단
- **해결**: `CorsConfig.java`에 `sunghoonyk.duckdns.org` 추가

### LazyInitializationException

- **문제**: async 쓰레드에서 Hibernate 세션 없음
- **해결**: `findEnabledWithSite()` + `JOIN FETCH`로 eager 로딩

### 수동 실행 타임아웃

- **문제**: `executeCrawl()` 동기 → HTTP 타임아웃
- **해결**: `CompletableFuture.runAsync()`로 비동기 실행

## 현재 수집 현황 (2026-07-16 기준)

| 설정 | 사이트 | 건수 |
|------|--------|------|
| java_daily | 사람인 | ~150 |
| java_daily | 잡코리아 | ~60 |
| java_daily | 원티드 | ~100 |
| react_daily | 사람인 | ~150 |
| react_daily | 잡코리아 | ~60 |
| react_daily | 원티드 | ~100 |

> 사람인 150건은 3페이지 상한값. 키워드 달라도 동일할 수 있음.
