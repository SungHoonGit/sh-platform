# 로드맵

## 변경 이력

### 2026-07-12 — OAuth2 인증 시스템 구축
- OAuth2UserInfo 전략 패턴 (카카오/네이버/구글/깃헙)
- CustomOAuth2UserService (자동 회원가입 + 인증 이벤트 로그)
- OAuth2SuccessHandler (JWT 발급 + 프론트엔드 리다이렉트)
- OAuth2FailureHandler (에러 리다이렉트)
- RateLimiter (인증 API Rate Limiting, ISMS-P 2.11.1 준수)
- SecurityConfig OAuth2 설정 (환경변수 기반, 확장성 고려)
- ErrorCode + messages_ko/en.properties 업데이트
- ISMS-P/WCAG 2.1 AA 준수 설계

### 2026-07-11 — Spring Boot 초기 배포 완료
- WEB 서버 Java 21 + Gradle 8.8 설치
- sh-platform-core systemd 서비스 등록 (`sh-platform.service`)
- nginx 설정 통합 (api 프록시 + kakao-bot 웹훅)
- MariaDB 연결 확인 (WEB → DB:3306)
- 테이블 자동 생성 확인 (users, refresh_tokens, verification_codes)
- `/api/health` 엔드포인트 동작 확인
- OCI 레포 문서 업데이트 완료

## Phase 1: 분석 및 설계 (✅ 완료)

### 인프라
- [x] OCI Always Free 전환 (WEB+Spring Boot + DB 2-Tier)
- [x] iptables 방화벽 (WEB/DB 별도 설정, 내부망만 허용)
- [x] A1.Flex 2개 (WEB 1/6GB + DB 1/8GB)
- [x] MariaDB 설치 + sh_pass/ai_housing DB 생성

### 산출물 (docs/)
- [x] `architecture.md` — 2-Tier + MSA 아키텍처
- [x] `login-plan.md` — 로그인/인증 기획
- [x] **`standards.md`** — 개발 표준 정의서 (패키지 구조, 네이밍, API 응답 포맷)
- [x] **`erd.md`** — ERD (users, refresh_tokens, verification_codes)
- [x] **`i18n.md`** — 다국어 처리 방안 (MessageSource + Accept-Language)
- [x] **`api-auth.md`** — 인증 API 명세 (회원가입 → 로그인 → JWT → OAuth2)
- [x] `roadmap.md` — 로드맵

## Phase 2: 인증 시스템 구축 (✅ 완료)

### 기본 인증
- [x] sh-platform-core Spring Boot 프로젝트 생성 (Gradle, Java 21)
- [x] WEB 서버 Java 21 + Gradle 8.8 설치
- [x] systemd 서비스 등록 (sh-platform.service)
- [x] nginx 프록시 설정 (/api/* → :8080, /webhook → :5002)
- [x] MariaDB DB 연결 확인 (WEB → DB 서버 10.0.0.39)
- [x] 테이블 자동 생성 확인 (users, refresh_tokens, verification_codes)
- [x] /api/health 엔드포인트 동작 확인
- [x] Security + JWT (RS256) 설정 확인 (기존 코드 활용)
- [x] 회원가입 API 구현
- [x] 로그인 API 구현 (JWT 발급)
- [x] 토큰 갱신 API 구현 (Refresh Token Rotation)
- [x] 로그아웃 API 구현
- [x] 이메일 인증 API 구현 (Gmail SMTP)
- [x] GlobalExceptionHandler + ApiResponse 통일 확인

### OAuth2 소셜 로그인
- [x] OAuth2UserInfo 전략 인터페이스 + 4개 구현체
- [x] OAuth2UserInfoFactory (프로바이더 확장 용이)
- [x] CustomOAuth2User (Spring Security OAuth2User 구현)
- [x] CustomOAuth2UserService (사용자 조회/생성 + 인증 이벤트 로그)
- [x] OAuth2SuccessHandler (JWT 발급 + 프론트엔드 리다이렉트)
- [x] OAuth2FailureHandler (에러 리다이렉트)
- [x] SecurityConfig OAuth2 설정 (환경변수 기반)
- [x] returnUrl 검증 (Open Redirect 방지)

### 보안 강화 (ISMS-P 준수)
- [x] RateLimiter (인증 API Rate Limiting)
- [x] 인증 이벤트 로그 (ISMS-P 2.9.4 준수)
- [x] ErrorCode + messages_ko/en.properties 업데이트
- [x] SSL/TLS 설정 (Let's Encrypt) — Phase 3

### 프론트엔드 연동 (예정)
- [ ] React 프론트엔드 프로젝트 생성
- [ ] 로그인/회원가입 화면
- [ ] OAuth2 콜백 페이지 (/auth/callback)
- [ ] 에러 페이지 (/auth/error)

### 배포
- [ ] GitHub Actions 자동 배포 (OCI_SSH_KEY 등록)
- [ ] OAuth2 환경변수 등록 (KAKAO_CLIENT_ID 등)

## Phase 3: 배포 및 연동 (🔜 진행중)

### 3.1 OAuth2 프로바이더 앱 등록 (진행중)
- [ ] 카카오 앱 등록 (기존 REST API 키 활용, Client Secret 설정)
- [ ] 네이버 앱 등록
- [ ] 구글 앱 등록
- [ ] 깃험 앱 등록

### 3.2 서버 환경변수 설정
- [ ] systemd 서비스에 OAuth2 env 변수 추가
- [ ] 각 프로바이더별 Client ID/Secret 확인

### 3.3 OAuth2 플로우 테스트
- [ ] 각 프로바이더별 로그인/회원가입 테스트
- [ ] 에러 핸들링 테스트 (취소, 실패)

### 3.4 도메인 및 SSL 인증서 설정 (✅ 완료)
- [x] duckdns.org 계정 생성 및 도메인 설정 (sunghoonyk.duckdns.org)
- [x] DNS 레코드 설정 (A 레코드 → 140.245.95.162)
- [x] DNS 전파 확인
- [x] certbot 설치
- [x] SSL 인증서 발급 (Let's Encrypt)
- [x] nginx SSL 설정 적용
- [x] HTTPS 접속 테스트
- [x] HSTS 헤더 설정
- [x] SSL 자동 갱신 설정

### 3.5 프론트엔드 구축 (별도 레포)
- [ ] React 프로젝트 생성
- [ ] 로그인/회원가입 화면 개발
- [ ] 소셜 로그인 버튼 구현
- [ ] API 연동

### 3.6 완료 기준
- [ ] OAuth2 로그인/회원가입 정상 동작
- [ ] HTTPS 통신 가능
- [ ] 프론트엔드 배포 완료

## Phase 4: MSA 확장
- [ ] Spring Cloud Gateway 도입
- [ ] auth-service / housing-service 모듈 분리
- [ ] daily-notifier 모듈 추가
- [ ] job-scraper API 연동
