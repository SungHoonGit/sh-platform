---
title: "Frontmatter Template"
description: "문서 frontmatter 작성 표준"
category: guides
tags: [convention, template, documentation]
created: 2026-07-21
updated: 2026-07-23
---

# Frontmatter Template

sh-platform의 모든 Markdown 문서는 YAML frontmatter를 포함해야 합니다.

## 템플릿

```yaml
---
title: "문서 제목"              # 반드시 따옴표
description: "한 줄 요약"       # 실제 내용 기반, 하드코딩 금지
category: 카테고리
tags: [태그1, 태그2]            # 선택
created: YYYY-MM-DD
updated: YYYY-MM-DD
---
```

## 카테고리 목록

| 카테고리 | 경로 | 설명 |
|---------|------|------|
| `architecture` | `docs/architecture/` | 시스템 아키텍처 |
| `auth` | `docs/auth/` | 인증/보안 |
| `scraper` | `docs/scraper/` | 스크래퍼 모듈 |
| `guides` | `docs/guides/` | 설정/가이드 |
| `infra` | `docs/infra/` | 인프라 |
| `infrastructure` | `docs/infrastructure/` | 인프라 (상세) |
| `common` | `docs/common/` | 공통 모듈 |
| `database` | `docs/database/` | 데이터베이스 |
| `development` | `docs/development/` | 개발 가이드 |
| `plans` | `docs/plans/` | 기획/설계 |
| `saas` | `docs/saas/` | SaaS |
| `daily` | `docs/daily/` | 작업 일지 |
| `front` | `docs/front/` | 프론트엔드 |
| `root` | `docs/` | 루트 (README, roadmap) |

## 규칙

- `title`: 따옴표 필수, 날짜 문서는 `YYYY-MM-DD` 포함
- `description`: 실제 작업 요약 (X: `daily module documentation`)
- `created`: 최초 커밋일
- `updated`: 마지막 수정일
- `category`: 상위 디렉토리명
- `tags`: 생략 가능, 소문자
