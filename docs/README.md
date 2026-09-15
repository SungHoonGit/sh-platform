# SH Platform 문서

## 문서 목록

### 인프라
| 문서 | 위치 | 설명 |
|------|------|------|
| [인프라 SSOT 가이드](./guides/008-260821-infra-ssot-guide.md) | guides/ | 서비스명/포트 단일 소스(infra/services.yml) |
| [MySQL 백업 가이드](./guides/009-260914-mysql-backup-guide.md) | guides/ | DB·웹 파일 주기 백업과 PITR |

### 데이터베이스
| 문서 | 위치 | 설명 |
|------|------|------|
| [DB 파티셔닝 가이드](./database/partitioning-guide.md) | database/ | MariaDB 파티셔닝 설정 및 관리 |
| [DB 설계 표준](./architecture/db-standards/db-design-standard.md) | architecture/ | DB 설계 컨벤션 |

### 아키텍처
| 문서 | 위치 | 설명 |
|------|------|------|
| [ERD](./architecture/erd.md) | architecture/ | 엔티티 관계도 |
| [SQL DDL](./architecture/sql-ddl.md) | architecture/ | 테이블 정의 |
| [아키텍처](./architecture/architecture.md) | architecture/ | 시스템 구성 |

### 인증
| 문서 | 위치 | 설명 |
|------|------|------|
| [API 인증 가이드](./auth/api-auth.md) | auth/ | JWT/OAuth2 인증 |
| [OAuth2 가이드](./auth/oauth2-registration-guide.md) | auth/ | OAuth2 제공자 등록 |
| [Gmail SMTP 설정](./auth/gmail-smtp-setup.md) | auth/ | 이메일 발송 설정 |

### 보안
| 문서 | 위치 | 설명 |
|------|------|------|
| [에러 페이지 전략](./security/error-pages.md) | security/ | 404/500/OAuth2/인프라 오류 페이지 설계 |
| [OWASP 웹 취약점 대응](./security/owasp-hardening.md) | security/ | 버전 숨김, 보안 헤더, 프로덕션 설정 |

### 가이드
| 문서 | 위치 | 설명 |
|------|------|------|
| [개발 가이드](./development-guide.md) | root | 전체 개발 프로세스 |
| [Swagger 가이드](./guides/swagger-guide.md) | guides/ | API 문서 |
| [통합 모니터링/알림 가이드](./guides/001-260818-monitoring-alerting-guide.md) | guides/ | 모니터링+로깅+알림 통합 |
| [UI 공용 패키지 가이드](./guides/011-260828-ui-shared-guide.md) | guides/ | @sh-platform/ui 사용법 |
| [Nginx 가이드](./guides/nginx-guide.md) | guides/ | 리버스 프록시 설정 |
| [포트 관리 가이드](./PORT-MANAGEMENT.md) | root | 포트/서비스 매핑, 충돌 해결 |
| [AI 개발 규칙](../AGENTS.md) | root | AI 코딩 에이전트 규칙 파일 |
| [Javadoc 가이드](./guides/javadoc-guide.md) | guides/ | 코드 문서 자동 생성 |
| [테스트 리포트 가이드](./guides/test-report-guide.md) | guides/ | JUnit 테스트 결과 |
| [스키마스파이 가이드](./guides/schemaSpy-guide.md) | guides/ | DB 문서 자동 생성 |
| [로깅 설치 가이드](./guides/logging-install.md) | guides/ | Loki/Promtail 설치 |

> **보관 문서**: 개편 이전 설계서는 `archive/2026/`에 이관(예: REACT-FRONTEND-DESIGN*, OCI-PLATFORM-GUIDE). git 히스토리에도 남아 있음.

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
