# nginx 설정 가이드

## 설정 파일 위치

| 파일 | 설명 |
|------|------|
| `/etc/nginx/sites-available/sh-platform` | 사이트 설정 (실제 설정) |
| `/etc/nginx/sites-enabled/sh-platform` | 활성화된 설정 (sites-available의 심링크) |
| `/etc/nginx/nginx.conf` | 메인 설정 (sites-enabled include) |

## 현재 설정 구조

```nginx
server {
    server_name sunghoonyk.duckdns.org;

    # SSL (443)
    listen [::]:443 ssl ipv6only=on;
    listen 443 ssl;
    ssl_certificate     /etc/letsencrypt/live/sunghoonyk.duckdns.org/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/sunghoonyk.duckdns.org/privkey.pem;
```

## location 블록 설명

| 경로 | 유형 | 대상 | 용도 |
|------|:----:|------|------|
| `/webhook` | `proxy_pass` | `127.0.0.1:5002` | webhook 수신 |
| `/api/` | `proxy_pass` | `127.0.0.1:8080` | REST API |
| `/oauth2/` | `proxy_pass` | `127.0.0.1:8080` | OAuth2 로그인 |
| `/login/` | `proxy_pass` | `127.0.0.1:8080` | OAuth2 리다이렉트 |
| `/test-reports/` | `alias` (정적 파일) | build/reports/tests/test/ | JUnit 리포트 |
| `/javadoc/` | `alias` (정적 파일) | build/docs/javadoc/ | Javadoc 문서 |
| `/swagger-ui/` | `proxy_pass` + rewrite | `127.0.0.1:8080` | Swagger UI |
| `/v3/` | `proxy_pass` | `127.0.0.1:8080` | Swagger API 스펙 |
| `/` | `try_files` | 정적 파일 | 프론트엔드 |

## 자주 쓰는 명령어

```bash
sudo nginx -t              # 설정 문법 검사
sudo systemctl reload nginx  # 설정 적용 (무중단)
sudo systemctl restart nginx  # 서비스 재시작
sudo systemctl status nginx   # 상태 확인
```

## 설정 파일 직접 보기

```bash
sudo cat /etc/nginx/sites-available/sh-platform
```

## 변경 방법

```bash
# 1. 백업
sudo cp /etc/nginx/sites-available/sh-platform /etc/nginx/sites-available/sh-platform.bak

# 2. 편집
sudo nano /etc/nginx/sites-available/sh-platform

# 3. 검사 및 적용
sudo nginx -t && sudo systemctl reload nginx
```

## 프록시 vs 정적 파일

| 방식 | 사용처 | 설정 |
|------|--------|------|
| `proxy_pass` | Spring Boot 앱으로 전달 | `proxy_pass http://127.0.0.1:8080;` |
| `alias` | 정적 HTML 파일 직접 서빙 | `alias /path/to/files/;` |
| `rewrite` | URL 리다이렉트 | `rewrite ^/old$ /new permanent;` |
