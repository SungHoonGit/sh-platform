# 도메인 및 SSL 인증서 설정 가이드

## 개요

SH Platform에 무료 도메인(duckdns.org)과 무료 SSL 인증서(Let's Encrypt)를 설정하는 과정을 기록합니다.

## 사전 준비

### 현재 환경
- 서버 IP: `140.245.95.162`
- WEB 서버: OCI Always Free A1.Flex (1/6GB)
- nginx: 설치 완료
- Spring Boot: `:8080` 포트로 실행 중

### 필요 항목
- duckdns.org 계정 (무료)
- 도메인 (예: `sh-platform.duckdns.org`)

---

## Step 1: duckdns.org 계정 생성 및 도메인 설정

### 1.1 회원가입
1. https://duckdns.org 접속
2. **Login** 클릭
3. GitHub/Google 계정으로 로그인

### 1.2 도메인 생성
1. 로그인 후 **Domains** 섹션
2. **Create Domain** 입력란에 원하는 이름 입력 (예: `sh-platform`)
3. **add domain** 클릭
4. 도메인 생성 완료: `sh-platform.duckdns.org`

### 1.3 DNS 레코드 설정
1. **Domains** → 생성한 도메인 선택
2. **IP Address** 입력: `140.245.95.162`
3. **update ip** 클릭
4. DNS 레코드가 A 레코드로 설정됨

### 1.4 토큰 확인
- 각 도메인마다 고유 토큰이 발급됨
- 이 토큰은 나중에 DNS 갱신에 사용됨

---

## Step 2: DNS 전파 확인

### 2.1 전파 확인
```bash
# 로컬에서 확인
nslookup sh-platform.duckdns.org

# 또는 dig 사용
dig sh-platform.duckdns.org
```

### 2.2 예상 결과
```
sh-platform.duckdns.org.  60  IN  A  140.245.95.162
```

> 주의: DNS 전파 시간은 최대 5분 소요될 수 있습니다.

---

## Step 3: SSL 인증서 발급 (Let's Encrypt)

### 3.1 certbot 설치
```bash
# 서버에 접속
ssh oci-web

# certbot 설치
sudo apt update
sudo apt install -y certbot python3-certbot-nginx
```

### 3.2 SSL 인증서 발급
```bash
# 방법 1: nginx 플러그인 사용 (추천)
sudo certbot --nginx -d sh-platform.duckdns.org

# 방법 2: webroot 플러그인 사용
sudo certbot certonly --webroot -w /var/www/html -d sh-platform.duckdns.org
```

### 3.3 발급 과정
1. 이메일 입력 (인증서 만료 알림용)
2. 약관 동의
3. 도메인 소유권 확인
4. 인증서 발급 완료

### 3.4 발급된 인증서 위치
- 인증서: `/etc/letsencrypt/live/sh-platform.duckdns.org/fullchain.pem`
- 개인키: `/etc/letsencrypt/live/sh-platform.duckdns.org/privkey.pem`

---

## Step 4: nginx SSL 설정

### 4.1 SSL 설정 파일 생성
```bash
sudo nano /etc/nginx/sites-available/sh-platform-ssl
```

### 4.2 설정 내용
```nginx
# HTTP → HTTPS 리다이렉트
server {
    listen 80;
    listen [::]:80;
    server_name sh-platform.duckdns.org;

    # Let's Encrypt 인증 확인용
    location /.well-known/acme-challenge/ {
        root /var/www/html;
    }

    # HTTPS로 리다이렉트
    location / {
        return 301 https://$host$request_uri;
    }
}

# HTTPS 서버
server {
    listen 443 ssl http2;
    listen [::]:443 ssl http2;
    server_name sh-platform.duckdns.org;

    # SSL 인증서
    ssl_certificate /etc/letsencrypt/live/sh-platform.duckdns.org/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/sh-platform.duckdns.org/privkey.pem;

    # SSL 보안 설정
    ssl_protocols TLSv1.2 TLSv1.3;
    ssl_ciphers HIGH:!aNULL:!MD5;
    ssl_prefer_server_ciphers on;

    # HSTS (강제 HTTPS)
    add_header Strict-Transport-Security "max-age=31536000; includeSubDomains" always;

    # 프록시 설정
    location /api/ {
        proxy_pass http://127.0.0.1:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }

    location /webhook {
        proxy_pass http://127.0.0.1:5002;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
    }

    location / {
        root /var/www/html;
        try_files $uri $uri/ /index.html;
    }
}
```

### 4.3 설정 적용
```bash
# 설정 파일 심볼릭 링크
sudo ln -sf /etc/nginx/sites-available/sh-platform-ssl /etc/nginx/sites-enabled/

# nginx 설정 테스트
sudo nginx -t

# nginx 재시작
sudo systemctl restart nginx
```

---

## Step 5: SSL 갱신 자동화

### 5.1 갱신 테스트
```bash
sudo certbot renew --dry-run
```

### 5.2 자동 갱신 크론 설정
```bash
# 크론 작업 열기
sudo crontab -e

# 매일 새벽 2시에 갱신 시도
0 2 * * * certbot renew --quiet
```

---

## Step 6: 테스트

### 6.1 HTTPS 접속 테스트
```bash
# 브라우저에서 접속
https://sh-platform.duckdns.org

# curl로 테스트
curl -I https://sh-platform.duckdns.org
```

### 6.2 예상 결과
- 브라우저: 보안 아이콘 (자물쇠) 표시
- curl: `HTTP/2 200` 응답

---

## 문제 해결

### 1. DNS 전파 오래 걸림
- duckdns.org에서 IP 업데이트 확인
- `dig` 명령어로 현재 DNS 상태 확인

### 2. SSL 인증서 발급 실패
- 포트 80이 열려 있는지 확인: `sudo ufw status`
- 방화벽에서 포트 80 허용: `sudo ufw allow 80`

### 3. nginx 설정 에러
- `sudo nginx -t`로 설정 문법 확인
- `/var/log/nginx/error.log`에서 에러 확인

---

## 완료 기준

- [x] duckdns.org 도메인 생성 완료 (sunghoonyk.duckdns.org)
- [x] DNS 레코드 설정 완료 (140.245.95.162)
- [x] DNS 전파 확인 완료
- [x] SSL 인증서 발급 완료 (Let's Encrypt)
- [x] nginx SSL 설정 적용 완료
- [x] HTTPS 접속 테스트 통과
- [x] HSTS 헤더 설정 완료
- [x] 자동 갱신 설정 완료

---

## 변경 이력

### 2026-07-12 — 설정 완료
- 도메인: `sunghoonyk.duckdns.org`
- SSL 인증서: Let's Encrypt (2026-10-10 만료)
- HTTPS 접속: `https://sunghoonyk.duckdns.org`
- HSTS: `max-age=31536000; includeSubDomains`
- 자동 갱신: certbot 타이머 설정 완료
