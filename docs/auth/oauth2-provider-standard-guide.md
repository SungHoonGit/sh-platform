---
title: Oauth2 Provider Standard Guide
description: Oauth2 Provider Standard Guide - auth module documentation
category: auth
created: 2026-07-13
updated: 2026-07-21
---

# OAuth2 프로바이더 표준 설정 가이드

## 개요

각 OAuth2 프로바이더별 앱 등록 시 설정해야 할 항목과 기준을 정의합니다.

## 공통 기준

### 필수 항목 (반드시 설정)
- 앱 이름
- Callback URL (Redirect URI)
- 서비스 URL
- 사용 API (로그인)
- Client ID / Client Secret
- 동의항목 (사용자 데이터 접근)

### 선택 항목 (나중에 설정 가능)
- 로고/아이콘
- 앱 설명
- 문의처
- 개인정보처리방침 URL

---

## 1. 카카오 (✅ 완료)

### 필수 설정 항목
| 항목 | 값 | 비고 |
|------|-----|------|
| 앱 이름 | SH Platform | 식별용 |
| 카카오 로그인 | 활성화 | OAuth2 동작에 필요 |
| Redirect URI | `https://sunghoonyk.duckdns.org/login/oauth2/code/kakao` | 필수 |
| 동의항목 | 이메일, 닉네임 | 사용자 식별에 필요 |
| REST API 키 | `3c05068bb49157f8175347908d84886c` | Client ID |
| Client Secret | 발급 | 보안 강화 (선택사항이지만 권장) |

### 설정 위치
- 내 앱 → 카카오 로그인 → Redirect URI 설정
- 내 앱 → 카카오 로그인 → 동의항목

### 주의사항
- 로그아웃 리다이렉트 URI와 별개
- Client Secret은 보안 설정에서 발급

---

## 2. 네이버 (진행중)

### 필수 설정 항목
| 항목 | 값 | 비고 |
|------|-----|------|
| 애플리케이션 이름 | SH Platform | 식별용 |
| 사용 API | 네이버 로그인 | 필수 |
| 서비스 URL | `https://sunghoonyk.duckdns.org` | 필수 |
| Callback URL | `https://sunghoonyk.duckdns.org/login/oauth2/code/naver` | 필수 |
| Client ID | 발급 | 필수 |
| Client Secret | 발급 | 필수 |

### 설정 위치
- 네이버 개발자 센터 → 애플리케이션 등록
- 네이버 개발자 센터 → 네이버 로그인 → API 설정

### 네이버 특이사항
- 별도의 API 등록 과정 필요
- Client Secret 발급 필수
- 동의항목 별도 설정 (이메일, 이름 등)

---

## 3. 구글 (예정)

### 필수 설정 항목
| 항목 | 값 | 비고 |
|------|-----|------|
| Application type | Web application | 필수 |
| Name | SH Platform | 식별용 |
| Authorized redirect URIs | `https://sunghoonyk.duckdns.org/login/oauth2/code/google` | 필수 |
| Client ID | 발급 | 필수 |
| Client Secret | 발급 | 필수 |

### 설정 위치
- Google Cloud Console → APIs & Services → Credentials
- OAuth consent screen 설정 필요

### 구글 특이사항
- 프로젝트 생성 필요
- OAuth consent screen 설정 필수
- 테스트 모드 제한 (100명까지)

---

## 4. 깃험 (예정)

### 필수 설정 항목
| 항목 | 값 | 비고 |
|------|-----|------|
| Application name | SH Platform | 식별용 |
| Homepage URL | `https://sunghoonyk.duckdns.org` | 필수 |
| Authorization callback URL | `https://sunghoonyk.duckdns.org/login/oauth2/code/github` | 필수 |
| Client ID | 발급 | 필수 |
| Client Secret | 발급 | 필수 |

### 설정 위치
- GitHub Settings → Developer settings → OAuth Apps

### 깃험 특이사항
- 별도 프로젝트 생성 불필요
- 가장 간단한 설정
- Client Secret 발급 시 복사 후 저장 필요

---

## 서버 환경변수 표준

### systemd 서비스 설정
```ini
Environment="KAKAO_CLIENT_ID=..."
Environment="KAKAO_CLIENT_SECRET=..."
Environment="NAVER_CLIENT_ID=..."
Environment="NAVER_CLIENT_SECRET=..."
Environment="GOOGLE_CLIENT_ID=..."
Environment="GOOGLE_CLIENT_SECRET=..."
Environment="GITHUB_CLIENT_ID=..."
Environment="GITHUB_CLIENT_SECRET=..."
Environment="OAUTH2_CALLBACK_BASE=https://sunghoonyk.duckdns.org"
```

### 설정 우선순위
1. systemd 환경변수 (추천)
2. application.yml
3. .env 파일

---

## 완료 기준

### 카카오 (✅ 완료)
- [x] 앱 이름 설정
- [x] 카카오 로그인 활성화
- [x] Redirect URI 등록
- [x] 동의항목 설정
- [x] REST API 키 확인
- [x] 로그인 플로우 테스트

### 네이버 (진행중)
- [ ] 애플리케이션 등록
- [ ] 네이버 로그인 API 등록
- [ ] Redirect URI 등록
- [ ] Client ID/Secret 발급
- [ ] 서버 환경변수 설정
- [ ] 로그인 플로우 테스트

### 구글 (예정)
- [ ] Google Cloud 프로젝트 생성
- [ ] OAuth consent screen 설정
- [ ] OAuth 클라이언트 ID 생성
- [ ] Redirect URI 등록
- [ ] Client ID/Secret 발급
- [ ] 서버 환경변수 설정
- [ ] 로그인 플로우 테스트

### 깃험 (예정)
- [ ] OAuth App 생성
- [ ] Redirect URI 등록
- [ ] Client ID/Secret 발급
- [ ] 서버 환경변수 설정
- [ ] 로그인 플로우 테스트
