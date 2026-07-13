# SH Platform 작업 이력

## 2026-07-13 작업 요약

### 1. 도메인 + SSL 설정
- duckdns.org에서 `sunghoonyk.duckdns.org` 등록
- Let's Encrypt SSL 인증서 발급 (만료: 2026-10-10)
- nginx SSL 설정 + HSTS 적용
- 자동 갱신 설정 (certbot)

### 2. OAuth2 프로바이더 등록 (4개)
| 프로바이더 | Client ID | 상태 |
|-----------|-----------|------|
| 카카오 | 3c05068bb49157f8175347908d84886c | 완료 |
| 네이버 | R4IcoQ6BRwtpeI9zTqfp | 완료 |
| 구글 | 1040528247878-9ph6o2beahc7h291qcpvs1m7dutbotla.apps.googleusercontent.com | 완료 |
| 깃험 | Ov23li7UZOBaralTetpP | 완료 |

### 3. 계정 연결 (Account Linking) 기능 구현
- UserProviderEntity + UserProviderRepository 생성
- CustomOAuth2UserService 계정 연결 로직 추가
- AccountLinkService 생성 (link/unlink/list)
- AuthController에 API 4개 추가
- MariaDB user_providers 테이블 생성 + 마이그레이션

### 4. Monorepo + Multi-Module 리팩토링
- sh-platform-core → sh-platform-auth로 이름 변경
- sh-platform-common 모듈 생성 (공통 라이브러리)
- Gradle 멀티모듈 구조 설정
- 빌드 성공 확인

### 5. 프론트엔드 협업 구조 설정
- CODEOWNERS 파일 생성 (폴더별 리뷰어 지정)
- frontend/ 디렉토리 생성
- 프론트엔드 개발자 (@noeyoeslee) Collaborator 초대 예정

### 6. 문서 정리
- api-auth.md: 계정 연결 API 섹션 추가
- frontend-auth-guide.md: 프론트엔드 인증 가이드 작성
- README.md: Monorepo 구조에 맞게 업데이트
- OCI 리소스 정보 업데이트 (2 OCPU/12GB)

### 7. GitHub Actions 수정
- 워크플로우에서 sh-platform-core → sh-platform-auth 참조로 변경

---

## GitHub 커밋 히스토리

| 커밋 | 설명 |
|------|------|
| d27231b | fix: GitHub Actions 워크플로우에서 sh-platform-auth 참조로 변경 |
| 142bea3 | chore: CODEOWNERS에 프론트엔드 개발자 (@noeyoeslee) 추가 |
| 8317cee | feat: 프론트엔드 협업 구조 설정 |
| afe2441 | refactor: sh-platform-core → sh-platform-auth 모듈명 변경 |
| f1223bb | refactor: Monorepo + Multi-Module 구조 리팩토링 |
| 47f3ffa | docs: 플랫폼 아키텍처 설계 문서 작성 |
| 9aacae3 | docs: 계정 연결 API 문서 + 프론트엔드 인증 가이드 작성 |
| 03d8b47 | feat: 계정 연결 (Account Linking) 기능 구현 |
| 770f9cc | docs: 문서 정리 + OCI 리소스 정보 업데이트 |

---

## 남은 작업

1. ✅ GitHub에서 프론트엔드 개발자 초대
2. ✅ Branch Protection 설정
3. 프론트엔드 프로젝트 초기화 (React)
4. 모니터링 시스템 구축 (Prometheus + Grafana)
5. sh-platform-board 모듈 추가 (게시판)

---

## 현재 프로젝트 구조

```
sh-platform/ (Monorepo)
├── settings.gradle.kts          # 멀티모듈 설정
├── build.gradle.kts             # 루트 빌드
├── sh-platform-auth/            # 인증 서비스 (Spring Boot 앱)
│   ├── build.gradle.kts
│   └── src/
├── sh-platform-common/          # 공통 라이브러리
│   ├── build.gradle.kts
│   └── src/
├── frontend/                    # 프론트엔드 (추후)
├── docs/                        # 문서
│   ├── architecture/            # 아키텍처 설계
│   ├── auth/                    # 인증 관련
│   ├── infra/                   # 인프라 설정
│   └── daily/                   # 작업 일지
└── .github/
    ├── CODEOWNERS               # 폴더별 리뷰어 지정
    └── workflows/               # GitHub Actions
```

---

## 2026-07-13 작업 내역

### 추가된 기능
1. **DB 문서 자동화 (SchemaSpy)**
   - SchemaSpy 6.2.4 설치 및 설정
   - MariaDB JDBC 드라이버 연동
   - Graphviz 설치 (ERD 다이어그램 생성)
   - nginx 설정으로 HTTPS 접속 가능
   - 접속 경로: https://sunghoonyk.duckdns.org/schemaSpy/

2. **문서 추가**
   -  - SchemaSpy 사용 가이드

### 배포
- SchemaSpy 자동 실행 스크립트 생성: 
- 출력 디렉토리: 
