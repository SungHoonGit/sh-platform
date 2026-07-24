---
title: Frontmatter Template
description: 문서 frontmatter 작성 표준
category: guide
tags: [convention, template, documentation]
created: 2026-07-21
updated: 2026-07-21
---

# Frontmatter Template

sh-platform의 모든 Markdown 문서는 YAML frontmatter를 포함해야 합니다.

## 템플릿

```yaml
---
title: 문서 제목
description: 한 줄 요약
category: 카테고리
tags: [태그1, 태그2]
created: 2026-07-21
updated: 2026-07-21
---
```

## 카테고리 목록

| 카테고리 | 경로 | 설명 |
|---------|------|------|
| `architecture` | `docs/architecture/` | 시스템 아키텍처 |
| `auth` | `docs/auth/` | 인증/보안 |
| `scraper` | `docs/scraper/` | 스크래퍼 모듈 |
| `guide` | `docs/guides/` | 설정/가이드 |
| `infra` | `docs/infra*/` | 인프라 |
| `common` | `docs/common/` | 공통 모듈 |
| `database` | `docs/database/` | 데이터베이스 |
| `plan` | `docs/plans/` | 기획/설계 |
| `saas` | `docs/saas/` | SaaS |
| `daily` | `docs/daily/` | 작업 일지 |
| `front` | `docs/front/` | 프론트엔드 |

## 규칙

- `title`: 파일명 기반 (kebab-case → Title Case)
- `description`: 첫 문장 또는 파일 목적 요약
- `created`: 최초 커밋일
- `updated`: 마지막 수정일
- `category`: 상위 디렉토리명
- `tags`: 생략 가능, 소문자
