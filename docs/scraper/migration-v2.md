# Scraper Platform DB 마이그레이션 v2 가이드

> 기존 테이블 구조를 표준에 맞게 변경

---

## 변경 사항

### 기존 구조 (v1)

```
category (카테고리)
├── id, name, slug, description
│
crawl_config (크롤링 설정)
├── id, category, query, career_level, sites, schedule...
│
crawl_data (크롤링 데이터)
├── id, category_id (FK → category), file_path...
│
crawl_log (크롤링 로그)
├── id, category_id (FK → category), status...
```

**문제점:**
- 사이트별 파라미터가 `crawl_config`에 칼럼으로 고정
- 사이트 추가 시 테이블 ALTER 필요
- 확장성 부족

### 새 구조 (v2)

```
site_definition (사이트 정의)
├── id, site_name, display_name, base_url, is_enabled
│
site_parameter_definition (파라미터 정의)
├── id, site_definition_id (FK), param_key, param_name, param_type, is_required, options
│
crawl_config (크롤링 설정)
├── id, name, description, schedule, retention_days, is_active
│
crawl_site_config (사이트별 설정)
├── id, config_id (FK), site_definition_id (FK), is_enabled, param_values (JSON)
│
crawl_data (크롤링 데이터)
├── id, config_id (FK), category, file_path...
│
crawl_log (크롤링 로그)
├── id, config_id (FK), site_definition_id (FK), status...
```

**장점:**
- 새 사이트 추가 시 INSERT만 하면 됨
- 사이트별 파라미터 자유롭게 추가/수정
- JSON으로 유연한 설정 저장

---

## 마이그레이션 단계

### 1. 백업

```bash
mysqldump -u sh_user -p scraper_platform > backup_v1.sql
```

### 2. DDL 실행

```bash
mysql -u sh_user -p scraper_platform < docs/scraper/ddl.sql
```

### 3. 기존 데이터 마이그레이션

- `crawl_data.category_id` → `crawl_data.category` 변환
- category_id 1 → 'java', 2 → 'react'

---

## 사이트별 필수 파라미터

### 사람인 (saramin)

| 파라미터 | 타입 | 필수 | 설명 |
|----------|------|------|------|
| keyword | text | ✅ | 검색어 |
| career | select | ✅ | 경력 |
| education | select | ✅ | 학력 |
| job_type | select | ✅ | 직무 |
| location | select | ✅ | 지역 |
| employment | select | ❌ | 고용형태 |
| salary | select | ❌ | 연봉 |

### 잡코리아 (jobkorea)

| 파라미터 | 타입 | 필수 | 설명 |
|----------|------|------|------|
| keyword | text | ✅ | 검색어 |
| career | select | ✅ | 경력 |
| education | select | ✅ | 학력 |
| company_type | select | ✅ | 기업형태 |
| employment_type | select | ✅ | 고용형태 |
| job_function | select | ✅ | 직무 |
| location | select | ✅ | 지역 |
| salary | select | ❌ | 연봉 |

### 원티드 (wanted)

| 파라미터 | 타입 | 필수 | 설명 |
|----------|------|------|------|
| keyword | text | ✅ | 검색어 |
| career | select | ✅ | 경력 |
| education | select | ❌ | 학력 |
| tech_stack | tags | ✅ | 기술 스택 |
| job_type | select | ✅ | 직무 |
| location | select | ✅ | 지역 |
| employment_type | select | ❌ | 고용형태 |
