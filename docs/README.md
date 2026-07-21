---
title: Readme
description: Readme - general module documentation
category: general
created: 2026-07-14
updated: 2026-07-21
---

# SH Platform 문서

## 문서 목록

### 인프라
| 문서 | 위치 | 설명 |
|------|------|------|
| [OCI 인프라 문서](./infrastructure/oci-infrastructure.md) | infrastructure/ | Oracle Cloud 계정, 서버, DB 정보 |

### 데이터베이스
| 문서 | 위치 | 설명 |
|------|------|------|
| [DB 파티셔닝 가이드](./database/partitioning-guide.md) | database/ | MariaDB 파티셔닝 설정 및 관리 |
| [DB 설계 표준](./architecture/db-standards/db-design-standard.md) | architecture/ | DB 설계 컨벤션 |

### 모듈
| 문서 | 위치 | 설명 |
|------|------|------|
| [공통 모듈 가이드](./common/modules.md) | common/ | scheduling, notification 모듈 |

### 인증
| 문서 | 위치 | 설명 |
|------|------|------|
| [API 인증 가이드](./auth/api-auth.md) | auth/ | JWT/OAuth2 인증 |
| [OAuth2 가이드](./auth/oauth2-registration-guide.md) | auth/ | OAuth2 제공자 등록 |
| [Gmail SMTP 설정](./auth/gmail-smtp-setup.md) | auth/ | 이메일 발송 설정 |

### 가이드
| 문서 | 위치 | 설명 |
|------|------|------|
| [개발 가이드](./development-guide.md) | root | 전체 개발 프로세스 |
| [Swagger 가이드](./guides/swagger-guide.md) | guides/ | API 문서 |
| [모니터링 가이드](./guides/monitoring-guide.md) | guides/ | Prometheus/Grafana |
| [Nginx 가이드](./guides/nginx-guide.md) | guides/ | 리버스 프록시 설정 |
| [포트 관리 가이드](./PORT-MANAGEMENT.md) | root | 포트/서비스 매핑, 충돌 해결 |
| [AI 개발 규칙](../AGENTS.md) | root | AI 코딩 에이전트 규칙 파일 |
| [Javadoc 가이드](./guides/javadoc-guide.md) | guides/ | 코드 문서 자동 생성 |
| [테스트 리포트 가이드](./guides/test-report-guide.md) | guides/ | JUnit 테스트 결과 |
| [스키마스파이 가이드](./guides/schemaSpy-guide.md) | guides/ | DB 문서 자동 생성 |
| [로깅 가이드](./guides/logging-guide.md) | guides/ | 로깅 설정 |

## 빠른 링크

### 서버 접속
```bash
ssh oci-web  # 웹 서버
ssh oci-db   # DB 서버
```

### 주요 URL
- 메인: https://sunghoonyk.duckdns.org/
- Swagger: https://sunghoonyk.duckdns.org/swagger-ui/
- Grafana: https://sunghoonyk.duckdns.org/grafana/
- Prometheus: https://sunghoonyk.duckdns.org/prometheus/
