# OCI 인프라 문서

## 1. Oracle Cloud 계정 정보

### 계정 정보
| 항목 | 값 |
|------|-----|
| Cloud Provider | Oracle Cloud Infrastructure (OCI) |
| 플랜 | Always Free |
| 리전 | 대만 (ap-taipei-1) |

### OCI 콘솔 접속
- URL: https://cloud.oracle.com
- 로그인: 이메일/비밀번호

## 2. 서버 정보

### Compute Instance (A1.Flex)
| 항목 | 값 |
|------|-----|
| 인스턴스 이름 | sh-platform-server |
| IP | 140.245.95.162 |
| OS | Ubuntu 24.04 |
| OCPU | 2 (ARM64) |
| 메모리 | 12GB |
| 상태 | Running |

### SSH 접속
Host oci-web not found: 3(NXDOMAIN)
Host oci-db not found: 3(NXDOMAIN)

## 3. Central Configuration (.env)

모든 서비스의 포트와 URL은 **`/home/ubuntu/sh-platform/.env`** 파일에서 중앙 관리됩니다.

### .env 파일


### 변경 시 영향 범위
| 파일 | 읽는 방식 |
|------|----------|
| OpenApiConfig.java (각 서비스) | `@Value("${SCRAPER_BASE_URL:...}")` |
| systemd service 파일 (4개) | `EnvironmentFile=/home/ubuntu/sh-platform/.env` |
| nginx config | 수동 반영 (포트/경로가 고정) |

**포트를 변경하려면:**
1. `.env`에서 포트 변경
2. `systemctl daemon-reload && systemctl restart sh-platform-{service}`
3. nginx config에서 `proxy_port` 변경

## 4. 데이터베이스 정보

### MariaDB
| 항목 | 값 |
|------|-----|
| 버전 | 10.11.14-MariaDB |
| 호스트 | 10.0.0.39 (내부) |
| 포트 | 3306 |
| 엔진 | InnoDB |
| 문자셋 | utf8mb4 |

### DB 계정
| 계정 | 권한 | 용도 |
|------|------|------|
| `sh_user` | ALL PRIVILEGES | 애플리케이션용 |
| `root` | ALL | 관리용 (SSH 접속 후 sudo 사용) |

### 데이터베이스 목록
| DB 이름 | 용도 | 상태 |
|---------|------|------|
| `sh_pass` | 인증 플랫폼 | 활성 |
| `scraper_platform` | 스크래퍼 플랫폼 | 활성 |
| `resume_platform` | 이력서 플랫폼 | 활성 |
| `portfolio_platform` | 포트폴리오 플랫폼 | 활성 |

### DB 접속
Welcome to Ubuntu 24.04.4 LTS (GNU/Linux 6.17.0-1011-oracle aarch64)

 * Documentation:  https://help.ubuntu.com
 * Management:     https://landscape.canonical.com
 * Support:        https://ubuntu.com/pro

This system has been minimized by removing packages and content that are
not required on a system that users do not log into.

To restore this content, you can run the 'unminimize' command.

## 5. 도메인 & SSL

### 도메인
| 항목 | 값 |
|------|-----|
| 도메인 | sunghoonyk.duckdns.org |
| DNS 서비스 | DuckDNS |
| IP | 140.245.95.162 |

### SSL 인증서
| 항목 | 값 |
|------|-----|
| 제공자 | Let's Encrypt |
| 만료일 | 2026-10-10 |
| 갱신 | 자동 (certbot) |

## 6. 서비스 URL 전체 목록

### Swagger / API 문서
| 서비스 | Swagger UI | API Docs |
|--------|-----------|----------|
| Auth | `/swagger-ui/` | `/v3/api-docs` |
| Scraper | `/scraper/swagger-ui/` | `/scraper/v3/api-docs` |
| Resume | `/resume/swagger-ui/` | `/resume/v3/api-docs` |
| Portfolio | `/portfolio/swagger-ui/` | `/portfolio/v3/api-docs` |

### Actuator (Health Check)
| 서비스 | URL |
|--------|-----|
| Auth | `/actuator/health` |
| Scraper | `/scraper/actuator/health` |
| Resume | `/resume/actuator/health` |
| Portfolio | `/portfolio/actuator/health` |

### 모니터링
| 서비스 | URL |
|--------|-----|
| Prometheus | `/prometheus/` |
| Grafana | `/grafana/` |

## 7. 서비스 관리 (systemd)

### 서비스 파일 위치
`/etc/systemd/system/`
- `sh-platform-auth.service`
- `sh-platform-scraper.service`
- `sh-platform-resume.service`
- `sh-platform-portfolio.service`

### 관리 명령어
```bash
# 상태 확인
sudo systemctl status sh-platform-{auth|scraper|resume|portfolio}

# 시작/중지/재시작
sudo systemctl start sh-platform-{service}
sudo systemctl stop sh-platform-{service}
sudo systemctl restart sh-platform-{service}

# 로그 확인
sudo journalctl -u sh-platform-{service} -f

# 전체 재시작
sudo systemctl restart sh-platform-auth sh-platform-scraper sh-platform-resume sh-platform-portfolio

# 부팅 시 자동 시작
sudo systemctl enable sh-platform-{auth|scraper|resume|portfolio}
```

### 포트 확인
```bash
ss -tlnp | grep -E '808[0-3]'
```

## 8. nginx 설정

### 설정 파일
`/etc/nginx/sites-available/sh-platform`

### 프록시 규칙
| URL 경로 | 업스트림 |
|---------|---------|
| `/api/` | `http://127.0.0.1:8080` |
| `/oauth2/` | `http://127.0.0.1:8080` |
| `/login/` | `http://127.0.0.1:8080` |
| `/swagger-ui/` | `http://127.0.0.1:8080/swagger-ui/` |
| `/v3/` | `http://127.0.0.1:8080/v3/` |
| `/scraper/` | `http://127.0.0.1:8081/` |
| `/resume/` | `http://127.0.0.1:8082/` |
| `/portfolio/` | `http://127.0.0.1:8083/` |
| `/prometheus/` | `http://127.0.0.1:9090/` |
| `/grafana/` | `http://127.0.0.1:3000` |

### nginx 변경 시
```bash
sudo nginx -t          # 설정 테스트
sudo systemctl reload nginx  # 리로드
```

## 9. DB 테이블 구조

### 스키마 관리
- `ddl-auto: validate` (auth, scraper) - 기존 테이블 검증
- `ddl-auto: none` (resume, portfolio) - 스키마 관리 안 함

### 파티셔닝
`crawl_log`, `common_schedule_log`, `common_notification_log`은 월별 RANGE 파티셔닝 적용
- 스크립트: `scripts/auto-partition.sh`
- SQL: `scripts/partition-maintenance.sql`

## 10. 장애 대응

### 서비스 접속 불가
1. nginx 상태 확인: `sudo systemctl status nginx`
2. 각 서비스 포트 확인: `ss -tlnp | grep 808[0-3]`
3. 서비스 로그: `sudo journalctl -u sh-platform-{service} -n 50`
4. 서비스 재시작: `sudo systemctl restart sh-platform-{service}`

### DB 접속 불가
1. SSH 접속 확인
2. MariaDB 서비스 상태: `sudo systemctl status mariadb`
3. 재시작: `sudo systemctl restart mariadb`

### SSL 인증서 만료
1. `sudo certbot renew`
2. `sudo systemctl reload nginx`

## 11. 비용

### Always Free 한도
| 항목 | 한도 | 현재 사용량 |
|------|------|------------|
| OCPU | 4 | 2 |
| 메모리 | 24GB | 12GB |
| 스토리지 | 200GB | ~20GB |
| 네트워크 | 10TB | 미사용 |

### 현재 비용: $0 (Always Free)
