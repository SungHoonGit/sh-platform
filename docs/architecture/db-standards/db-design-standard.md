---
title: DB Design Standard
description: DB Design Standard - architecture module documentation
category: architecture
created: 2026-07-15
updated: 2026-07-21
---

# DB 설계 표준 정의서

> SH Platform 모든 모듈의 DB 설계 시 반드시 따르는 표준입니다.

---

## 1. 테이블 네이밍

| 규칙 | 예시 |
|------|------|
| 소문자 + snake_case | `crawl_config`, `site_definition` |
| 복수형 명사 사용 | ✅ `users`, ❌ `user` |
| 접두사 금지 (tbl_, t_) | ✅ `crawl_data`, ❌ `tbl_crawl_data` |
| M:N 조인 테이블: `_` 연결 | `crawl_site_config` |
| 플래그 컬럼: `is_` 접두사 | `is_active`, `is_enabled` |

---

## 2. 컬럼 네이밍

| 규칙 | 예시 |
|------|------|
| 소문자 + snake_case | `source_url`, `career_level` |
| FK: `{테이블명}_id` | `category_id`, `site_definition_id` |
| PK: `id` (BIGINT) | `id BIGINT AUTO_INCREMENT` |
| Boolean: `is_` 접두사 | `is_active`, `is_required` |
| Timestamp: `_at` 접미사 | `created_at`, `completed_at` |

---

## 3. 필수 공통 컬럼

모든 테이블에 다음 컬럼을 포함한다:

```sql
id          BIGINT AUTO_INCREMENT PRIMARY KEY
created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP
updated_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
```

| 컬럼 | 타입 | 규칙 |
|------|------|------|
| `id` | BIGINT | PK, AUTO_INCREMENT |
| `created_at` | TIMESTAMP | 생성 시간, 수정 불가 |
| `updated_at` | TIMESTAMP | 수정 시간, 자동 갱신 |

**soft delete 사용 시:**
```sql
deleted_at  TIMESTAMP NULL DEFAULT NULL   -- NULL이면 활성, 값이 있으면 삭제됨
```

---

## 4. 타입 표준

### 4.1 숫자

| 용도 | 타입 | 크기 |
|------|------|------|
| PK/FK | BIGINT | 8 bytes |
| 카운트/순서 | INT | 4 bytes |
| 가격/금액 | DECIMAL(12,2) | 고정소수점 |

### 4.2 문자열

| 용도 | 타입 | 비고 |
|------|------|------|
| 이메일, 슬러그 | VARCHAR(100) | 고정 길이 |
| 이름, 제목 | VARCHAR(200) | |
| URL | VARCHAR(500) | |
| 긴 본문 | TEXT | 64KB |
| 짧은 본문 | VARCHAR(2000) | |

### 4.3 시간

| 용도 | 타입 | 비고 |
|------|------|------|
| 생성/수정 시간 | TIMESTAMP | default + on update |
| 일반 날짜 | DATE | |
| 크론 문자열 | VARCHAR(100) | |

### 4.4 구조화 데이터

| 용도 | 타입 | 비고 |
|------|------|------|
| 자유형태 설정 | JSON | MySQL JSON 타입 |
| 태그 목록 | JSON | ["tag1","tag2"] |
| Enums | ENUM 또는 VARCHAR(50) | ENUM은 변경 비용 큼, VARCHAR 권장 |

> **ENUM 주의**: ENUM 변경 시 ALTER TABLE 필요. 값이 빈번히 바뀌면 VARCHAR(50) 사용.

### 4.5 Boolean

| 용도 | 타입 | 비고 |
|------|------|------|
| 활성화 여부 | BOOLEAN (TINYINT(1)) | 0/1 |

---

## 5. 인덱싱 표준

### 5.1 기본 인덱스

```sql
-- FK 컬럼: 무조건 인덱스
INDEX idx_{테이블명}_{컬럼명} (category_id)

-- 검색 빈도 높은 컬럼
INDEX idx_{테이블명}_{컬럼명} (source_url(191))

-- 복합 인덱스: 자주 같이 조회되는 컬럼
INDEX idx_{테이블명}_{col1}_{col2} (col1, col2)
```

### 5.2 유니크 인덱스

```sql
-- 슬러그, 이메일 등 중복 불가 컬럼
UNIQUE KEY uk_{테이블명}_{컬럼명} (slug)
```

### 5.3 풀텍스트 인덱스

```sql
-- 검색 대상 텍스트
FULLTEXT INDEX idx_{테이블명}_{컬럼명} (title, description)
```

### 5.4 인덱스 네이밍 규칙

| 타입 | 형식 | 예시 |
|------|------|------|
| 일반 인덱스 | `idx_{테이블}_{컬럼}` | `idx_crawl_data_category_id` |
| 유니크 | `uk_{테이블}_{컬럼}` | `uk_category_slug` |
| 풀텍스트 | `ft_{테이블}_{컬럼}` | `ft_crawl_data_title` |
| 복합 | `idx_{테이블}_{col1}_{col2}` | `idx_crawl_config_is_active_created` |

---

## 6. FK(외래키) 표준

```sql
FOREIGN KEY (category_id) REFERENCES category(id) ON DELETE CASCADE
```

| 상황 | ON DELETE | ON UPDATE |
|------|-----------|-----------|
| 자식도 삭제 | CASCADE | CASCADE |
| 부모 삭제 시 자식 보존 | RESTRICT | CASCADE |
| 부모 삭제 시 자식 보존 + NULL | SET NULL | CASCADE |
| 별도 테이블로 이동 | NO ACTION | CASCADE |

---

## 7. 엔티티 간 관계 패턴

### 7.1 1:N 관계

```sql
-- 부모 테이블
CREATE TABLE category (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    ...
);

-- 자식 테이블
CREATE TABLE crawl_data (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    category_id BIGINT NOT NULL,       -- FK
    ...
    FOREIGN KEY (category_id) REFERENCES category(id) ON DELETE CASCADE
);
```

### 7.2 M:N 관계 (조인 테이블)

```sql
-- 조인 테이블
CREATE TABLE crawl_site_config (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    config_id BIGINT NOT NULL,
    site_definition_id BIGINT NOT NULL,
    ...
    UNIQUE KEY uk_config_site (config_id, site_definition_id),
    FOREIGN KEY (config_id) REFERENCES crawl_config(id) ON DELETE CASCADE,
    FOREIGN KEY (site_definition_id) REFERENCES site_definition(id) ON DELETE RESTRICT
);
```

### 7.3 설정 데이터 패턴 (EAV/JSON 혼합)

사이트별로 다른 파라미터를 가져야 하는 경우:

```
site_parameter_definition (메타데이터 정의)
    → param_key, param_type, is_required, options(JSON)

crawl_site_config.param_values (실제 값 저장)
    → {"keyword":"Java","career":"3~5년","location":"서울"}
```

**장점:**
- 파라미터 추가/수정 시 DDL 변경 없음
- 사이트마다 다른 구조 지원
- 프론트에서 동적 폼 렌더링 가능

---

## 8. JSON 컬럼 사용 규칙

### 8.1 사용 대상

| 대상 | 예시 |
|------|------|
| 구조화된 설정값 | `{"keyword":"Java","career":"3~5년"}` |
| 태그/옵션 목록 | `["Java","Spring","JPA"]` |
| 복잡한 중첩 구조 | `{"saramin":{...},"jobkorea":{...}}` |

### 8.2 제약조건

- JSON 유효성 검증: `CHECK (json_valid(column_name))`
- 인덱싱 불가 (MySQL 8.0 미만)
- 검색 시 JSON_EXTRACT 사용

```sql
-- JSON 값 추출
SELECT JSON_EXTRACT(param_values, '$.keyword') FROM crawl_site_config;

-- JSON 필터링
SELECT * FROM crawl_site_config 
WHERE JSON_EXTRACT(param_values, '$.career') = '3~5년';
```

---

## 9. 기존 테이블 마이그레이션 기준

기존 테이블이 표준 미준수 시, 아래 우선순위로 마이그레이션:

1. **PK/FK 규칙** → `id BIGINT` + `*_id` 컬럼명
2. **공통 컬럼** → `created_at`, `updated_at` 추가
3. **인덱스** → FK 컬럼 + 검색 컬럼 인덱스 추가
4. **유니크** → 슬러그, 이메일 등 중복 불가 컬럼에 추가

> ⚠️ 마이그레이션 시 반드시 **DDL 문서 먼저 업데이트** → **실행** 순서로 진행.

---

## 10. DDL 문서 표준

모든 DDL은 `docs/` 하위에 Markdown으로 관리:

```
docs/
├── architecture/
│   └── db-standards/
│       ├── db-design-standard.md      ← 이 문서
│       └── db-naming-convention.md    ← 네이밍 상세
├── scraper/
│   └── ddl.sql                        ← 스크래퍼 DB DDL
└── auth/
    └── ddl.sql                        ← 인증 DB DDL
```

### DDL 문서 형식

```sql
-- ============================================================
-- {모듈명} DDL
-- Database: {db_name}
-- 표준: docs/architecture/db-standards/db-design-standard.md
-- ============================================================

-- 1. {테이블명} ({설명})
CREATE TABLE IF NOT EXISTS {테이블명} (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    ...
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

---

## 11. 체크리스트

- [ ] 테이블명: 소문자 + 복수형 + snake_case
- [ ] 컬럼명: 소문자 + snake_case
- [ ] PK: `id BIGINT AUTO_INCREMENT`
- [ ] FK: `{테이블}_id` 네이밍
- [ ] 공통 컬럼: `created_at`, `updated_at`
- [ ] FK 컬럼: 인덱스 추가
- [ ] 검색 컬럼: 인덱스 추가
- [ ] 슬러그/중복불가: 유니크 인덱스
- [ ] DDL 문서: `docs/` 하위에 Markdown 저장
- [ ] 기존 테이블과 호환성 확인

---

## 12. 참고

- [개발 표준 (Java)](../standards.md)
- [모듈 표준](../../development/module-standard.md)
- [스크래퍼 DDL](../../scraper/ddl.sql)
