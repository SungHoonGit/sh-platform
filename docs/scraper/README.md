# Scraper Platform 문서

> 채용 공고 스크래퍼 관리 시스템

---

## 아키텍처

```
scraper-platform
├── DB (scraper_platform)
│   ├── site_definition          # 사이트 정의
│   ├── site_parameter_definition # 사이트별 파라미터 정의
│   ├── crawl_config             # 크롤링 설정 (메인)
│   ├── crawl_site_config        # 사이트별 크롤링 설정
│   ├── crawl_data               # 크롤링 데이터
│   └── crawl_log                # 크롤링 로그
│
├── Backend (Spring Boot)
│   ├── Controller              # REST API
│   ├── Service                 # 비즈니스 로직
│   ├── Repository              # DB 접근
│   └── Entity                  # JPA 엔티티
│
└── Scraper (Python)
    └── job-scraper             # 실제 크롤링 실행
```

---

## 문서 구조

```
docs/scraper/
├── README.md                   # 이 문서
├── architecture.md             # 아키텍처 상세
├── ddl.sql                     # DB DDL
├── data-paths.md               # 데이터 경로
├── roadmap.md                  # 로드맵
└── migration-v2.md             # v2 마이그레이션 가이드
```

---

## DB 테이블

| 테이블 | 설명 | 관계 |
|--------|------|------|
| `site_definition` | 지원 사이트 목록 | 1:N → site_parameter_definition |
| `site_parameter_definition` | 사이트별 파라미터 정의 | N:1 → site_definition |
| `crawl_config` | 크롤링 설정 (메인) | 1:N → crawl_site_config |
| `crawl_site_config` | 사이트별 설정值 | N:1 → crawl_config, site_definition |
| `crawl_data` | 수집된 채용 공고 | N:1 → crawl_config |
| `crawl_log` | 크롤링 실행 로그 | N:1 → crawl_config, site_definition |

---

## API 엔드포인트

| Method | URL | 설명 |
|--------|-----|------|
| GET | `/scraper/sites` | 사이트 목록 |
| GET | `/scraper/sites/{id}/parameters` | 사이트별 파라미터 정의 |
| GET/POST/PUT/DELETE | `/scraper/configs` | 크롤링 설정 CRUD |
| POST | `/scraper/data-import/all` | MD 파일 임포트 |
| GET | `/scraper/crawl-data/search` | 키워드 검색 |
| GET | `/scraper/crawl-data/advanced-search` | 고급 검색 |
| GET | `/scraper/swagger-ui/index.html` | Swagger UI |
| GET | `/scraper/javadoc/` | Javadoc |
| GET | `/scraper/test-reports/` | JUnit 테스트 리포트 |

---

## 현재 진행 상황

| Phase | 내용 | 상태 |
|-------|------|------|
| Phase 1 | DB 구조 + API | ✅ 완료 |
| Phase 2 | 프론트엔드 UI | 🔜 예정 |
| Phase 3 | Python 스크래퍼 연동 | 🔜 예정 |
| Phase 4 | 스케줄러 자동화 | 🔜 예정 |
