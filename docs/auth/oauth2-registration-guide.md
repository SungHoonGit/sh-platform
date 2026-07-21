---
title: Oauth2 Registration Guide
description: Oauth2 Registration Guide - auth module documentation
category: auth
created: 2026-07-13
updated: 2026-07-21
---

# OAuth2 프로바이더 앱 등록 가이드

## 개요

SH Platform OAuth2 소셜 로그인을 위해 각 프로바이더 콘솔에서 앱을 등록하고 Client ID/Secret을 발급받는 가이드입니다.

## 프로바이더별 등록 방법

### 1. 카카오

기존 REST API 키(`3c05068bb49157f8175347908d84886c`)를 활용합니다.

#### 등록 절차
1. https://developers.kakao.com/console/app 접속
2. 기존 앱 선택 또는 새로 생성
3. **카카오 로그인** 메뉴 → **활성화 설정**
4. Redirect URI 등록:
   ```
   http://140.245.95.162/login/oauth2/code/kakao
   ```
5. **Client Secret** 설정 (카카오 로그인 활성화 시 자동 발급)

#### 필요 환경변수
- `KAKAO_CLIENT_ID`: REST API 키 (기존 것 활용)
- `KAKAO_CLIENT_SECRET`: 카카오 로그인 활성화 시 발급

---

### 2. 네이버

#### 등록 절차
1. https://developers.naver.com 접속 → 로그인
2. **애플리케이션 등록** 클릭
3. 입력:
   - 애플리케이션 이름: `SH Platform`
   - 사용 API: **네이버 로그인** 체크
   - 서비스 URL: `http://140.245.95.162`
   - Callback URL: `http://140.245.95.162/login/oauth2/code/naver`
4. 등록 완료 후 **Client ID**, **Client Secret** 확인

#### 필요 환경변수
- `NAVER_CLIENT_ID`: 애플리케이션 정보에서 확인
- `NAVER_CLIENT_SECRET`: 애플리케이션 정보에서 확인

---

### 3. 구글

#### 등록 절차
1. https://console.cloud.google.com 접속 → 프로젝트 선택 (또는 새로 생성)
2. **APIs & Services** → **OAuth consent screen** 설정
3. **Credentials** → **Create Credentials** → **OAuth client ID**
4. 입력:
   - Application type: **Web application**
   - Name: `SH Platform`
   - Authorized redirect URIs: `http://140.245.95.162/login/oauth2/code/google`
5. **Client ID**, **Client Secret** 확인

#### 필요 환경변수
- `GOOGLE_CLIENT_ID`: OAuth 클라이언트 ID
- `GOOGLE_CLIENT_SECRET`: OAuth 클라이언트 보안 비밀번호

---

### 4. 깃험

#### 등록 절차
1. https://github.com/settings/developers 접속
2. **OAuth Apps** → **New OAuth App**
3. 입력:
   - Application name: `SH Platform`
   - Homepage URL: `http://140.245.95.162`
   - Authorization callback URL: `http://140.245.95.162/login/oauth2/code/github`
4. **Register application**
5. **Client ID**, **Generate a new client secret** → **Client Secret** 확인

#### 필요 환경변수
- `GITHUB_CLIENT_ID`: OAuth App Client ID
- `GITHUB_CLIENT_SECRET`: OAuth App Client Secret

---

## 서버 환경변수 등록

각 프로바이더 앱 등록 후, 발급받은 키를 서버에 등록합니다.

### 방법 1: systemd 환경변수 (추천)

```bash
sudo nano /etc/systemd/system/sh-platform.service
```

`[Service]` 섹션에 추가:
```ini
Environment="KAKAO_CLIENT_ID=REST_API_키"
Environment="KAKAO_CLIENT_SECRET=시크릿"
Environment="NAVER_CLIENT_ID=클라이언트_ID"
Environment="NAVER_CLIENT_SECRET=시크릿"
Environment="GOOGLE_CLIENT_ID=클라이언트_ID"
Environment="GOOGLE_CLIENT_SECRET=시크릿"
Environment="GITHUB_CLIENT_ID=클라이언트_ID"
Environment="GITHUB_CLIENT_SECRET=시크릿"
Environment="FRONTEND_URL=http://localhost:3000"
Environment="OAUTH2_CALLBACK_BASE=http://140.245.95.162"
```

```bash
sudo systemctl daemon-reload
sudo systemctl restart sh-platform
```

### 방법 2: .env 파일 (간편하지만 보안 주의)

```bash
nano /home/ubuntu/sh-platform/.env
```

---

## 테스트 방법

### 1. 카카오 로그인 테스트
```bash
curl -v "http://140.245.95.162/api/v1/auth/oauth2/kakao?returnUrl=/dashboard"
```

### 2. 서버 로그 확인
```bash
ssh oci-web "sudo journalctl -u sh-platform -f"
```

### 3. 프론트엔드 콜백 테스트
브라우저에서 직접 접속:
```
http://140.245.95.162/api/v1/auth/oauth2/kakao?returnUrl=/dashboard
```

---

## 문제 해결

### 1. Redirect URI 불일치 오류
- 각 프로바이더 콘솔에서 Redirect URI가 정확히 등록되어 있는지 확인
- 프로토콜, 포트, 경로 포함 전체 URL 확인

### 2. Client Secret 오류
- 각 프로바이더 콘솔에서 Client Secret 재발급 후 서버 env 변수 업데이트

### 3. CORS 오류
- 프론트엔드 도메인이 `FRONTEND_URL` env 변수와 일치하는지 확인

---

## 완료 기준

- [ ] 각 프로바이더별 앱 등록 완료
- [ ] 환경변수 등록 완료
- [ ] 각 프로바이더별 로그인 테스트 통과
