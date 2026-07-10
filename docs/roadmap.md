# 로드맵

## Phase 1: 인프라 및 설계 (✅ 완료)
- [x] OCI Tokyo Free Tier VM 2대 생성
- [x] VM1/2 SSH PW 인증 + 보안 설정
- [x] VM2 MariaDB 설치 및 보안 설정
- [x] VM1/2 iptables 방화벽 설정 (내부망만 허용)
- [x] oci repo 생성 (인프라 문서 통합)
- [x] sh-platform repo 생성 + 설계 문서

## Phase 2: 인증 시스템 구축 (🔜 시작)
- [ ] Spring Boot 프로젝트 (sh-platform-core) 생성
- [ ] Security + JWT 설정
- [ ] OAuth2 Client (카카오/네이버/구글/깃헙)
- [ ] 이메일 인증 (Gmail SMTP)
- [ ] API Gateway 기본 라우팅

## Phase 3: ai-housing 연동
- [ ] ai-housing-api에 JWT 검증 추가
- [ ] ai-housing-web 로그인 페이지 연결
- [ ] nginx 프록시 설정 (VM1 → Spring Boot)

## Phase 4: MSA 확장
- [ ] VM3 생성 (A1.Flex 또는 동일 E2)
- [ ] sh-platform-gateway → VM3 분리
- [ ] ai-housing-api → VM3 분리
- [ ] daily-notifier 모듈 추가
- [ ] job-scraper API 연동
