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

## 빠른 링크

### 서버 접속
```bash
ssh oci-web  # 웹 서버
ssh oci-db   # DB 서버
```

### 주요 URL
- 메인: https://sunghoonyk.duckdns.org/
- Swagger: https://sunghoonyk.duckdns.org/scraper/swagger-ui.html
- Grafana: https://sunghoonyk.duckdns.org/grafana/
- Prometheus: https://sunghoonyk.duckdns.org/prometheus/
