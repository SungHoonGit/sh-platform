# 2026-08-01 nginx 보안 강화 적용 (git 관리 전환)

## 개요

에러 페이지 전략 + OWASP 보안 강화 권고안에 따라 nginx 설정을 하드닝하고, 서버 수동 관리에서 **git 버전 관리**로 전환했다.

## 배경

- 서버 nginx 설정은 `/etc/nginx/sites-available/sh-platform` 에 존재했으나 이 저장소(`/home/ubuntu/sh-platform`) **밖**이라 git 관리 대상이 아니었다.
- 백업은 수동 `cp *.bak.$(date)` 방식으로만 유지되고 있었다.

## 적용한 하드닝

`infra/nginx/sh-platform.conf` (실제 서버 설정 + 추가분)

| 항목 | 내용 |
|------|------|
| `server_tokens off` | Server 헤더에서 nginx 버전 노출 차단 |
| `X-Content-Type-Options: nosniff` | MIME 스니핑 방지 |
| `X-Frame-Options: DENY` | 클릭재킹 방지 |
| `Referrer-Policy: strict-origin-when-cross-origin` | 레퍼러 노출 최소화 |
| `Permissions-Policy` | 카메라/마이크/위치 사용 차단 |
| `error_page 502 503 504 /502.html` | 인프라 장애 시 브랜딩 점검 페이지 |
| `location = /502.html { internal; }` | 외부 직접 접근 차단 |

502 페이지: `infra/nginx/502.html` → `/var/www/sh-platform/502.html` 배치

## 핵심 함정: sites-enabled가 심링크가 아님

**원인**: 서버의 `/etc/nginx/sites-enabled/sh-platform` 이 심링크가 아닌 **독립 파일**(6104바이트)이라,
`sites-available/` 만 덮어쓰면 nginx가 변경을 로드하지 않았다.

**증상**: `nginx -t` 통과 + reload 성공인데도 `curl -I` 응답이 `Server: nginx/1.24.0 (Ubuntu)` 로 구버전 유지.
`sudo nginx -T` 에 `server_tokens off` 가 없어야 판명.

**해결**: `sites-enabled/` 에도 동일 파일을 복사해야 로드된다.
배포 워크플로(`deploy-backend.yml`)도 `sites-available` + `sites-enabled` **둘 다 복사**하도록 수정.

## 적용 절차 (수동, 일회성)

```bash
cd /home/ubuntu/sh-platform && git pull origin master
sudo mkdir -p /var/www/sh-platform
sudo cp infra/nginx/502.html /var/www/sh-platform/502.html
sudo cp infra/nginx/sh-platform.conf /etc/nginx/sites-available/sh-platform
sudo cp infra/nginx/sh-platform.conf /etc/nginx/sites-enabled/sh-platform   # 필수 (독립 파일)
sudo nginx -t && sudo systemctl reload nginx
```

## 검증

```bash
curl -I https://sunghoonyk.duckdns.org/ | grep -iE "server|x-frame|x-content|referrer|permissions"
```

- 기대: `server: nginx` (버전 없음) + 보안 헤더 4종
- 관찰: `Server: nginx/1.24.0 (Ubuntu)` → 적용 성공 후 `nginx` 로 축약 확인 필요

## 배포 자동화 (이후)

`deploy-backend.yml` 마지막 단계에 추가:

```yaml
sudo mkdir -p /var/www/sh-platform
sudo cp infra/nginx/502.html /var/www/sh-platform/502.html
sudo cp infra/nginx/sh-platform.conf /etc/nginx/sites-available/sh-platform
sudo cp infra/nginx/sh-platform.conf /etc/nginx/sites-enabled/sh-platform
sudo nginx -t
sudo systemctl reload nginx
```

이제부터 **nginx 설정의 원본은 저장소 `infra/nginx/sh-platform.conf`** 이고,
서버에서 직접 수정하면 다음 배포 때 덮어써진다 (수정은 저장소에서).

## 커밋

- `db076c1` chore: nginx 설정 git 관리 전환 (실제 서버 설정 + 하드닝 반영)
- `215b297` fix: nginx 동기화가 sites-enabled(독립 파일)까지 복사하도록 수정

## 남은 항목

- [ ] CSP(Content-Security-Policy)는 각 SPA 콘솔 위반 확인 후 적용 (owasp-hardening.md 참고)
- [ ] 배포 완료 후 `curl -I` 최종 검증 재확인
- [ ] (선택) 이 PC에 `ssh oci-web` SSH 키 설정 → 제가 서버 조치 대신 수행 가능
