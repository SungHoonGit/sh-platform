# 로드맵

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
- [x] **`api-auth.md`** — 인증 API 명세 (회원가입 → 로그인 → JWT)
- [x] `roadmap.md` — 로드맵

## Phase 2: 인증 시스템 구축 (🔜 시작)
- [ ] sh-platform-core Spring Boot 프로젝트 생성 (Gradle, Java 21)
- [ ] Security + JWT (RS256) 설정
- [ ] 회원가입 / 이메일 인증 API 구현
- [ ] 로그인 / 토큰 갱신 API 구현
- [ ] OAuth2 Client (카카오/네이버/구글/깃헙)
- [ ] GlobalExceptionHandler + ApiResponse 통일

## Phase 3: 배포 및 연동
- [ ] sh-platform-core WAS (WEB 서버 :8080) 배포
- [ ] nginx 프록시 (WEB → Spring Boot :8080)
- [ ] MariaDB DB (DB 서버 10.0.0.39) 연결
- [ ] ai-housing-web 로그인 연결
- [ ] 서브도메인 + SSL (Let's Encrypt)

## Phase 4: MSA 확장
- [ ] Spring Cloud Gateway 도입
- [ ] auth-service / housing-service 모듈 분리
- [ ] daily-notifier 모듈 추가
- [ ] job-scraper API 연동
