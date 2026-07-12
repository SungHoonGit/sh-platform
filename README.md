# SH Platform

SH(SungHoon) SaaS 기반 통합 플랫폼. Monorepo + Multi-Module 구조.

## 목표

- 통합 로그인 (SH Pass) — 일반 + 소셜(카카오/네이버/구글/깃험)
- API Gateway — 서비스 라우팅/JWT 검증
- SSO — 여러 플랫폼에서 하나의 계정으로 로그인
- 점진적 MSA 확장 — 게시판, 쇼핑몰 등 연결

## 저장소 구조

```
sh-platform/
├── settings.gradle.kts          # 멀티모듈 설정
├── build.gradle.kts             # 루트 빌드
├── sh-platform-auth/            # 인증 서비스 (Spring Boot 앱)
│   ├── build.gradle.kts
│   └── src/
├── sh-platform-common/          # 공통 라이브러리 (DTO, 예외, util)
│   ├── build.gradle.kts
│   └── src/
├── docs/                        # 문서
│   ├── architecture/            # 아키텍처 설계
│   ├── auth/                    # 인증 관련
│   ├── infra/                   # 인프라 설정
│   └── daily/                   # 작업 일지
└── .github/                     # GitHub Actions
```

## 모듈 설명

| 모듈 | 설명 | 상태 |
|------|------|------|
| sh-platform-auth | 인증 서비스 (로그인, OAuth2, JWT, SSO) | ✅ 개발 중 |
| sh-platform-common | 공통 라이브러리 (DTO, 예외 처리) | ✅ 생성됨 |
| sh-platform-board | 게시판 (추후) | ⏳ 예정 |
| sh-platform-shop | 쇼핑몰 (추후) | ⏳ 예정 |

## OCI 인프라

| VM | Public IP | 역할 | 사양 |
|----|-----------|------|------|
| WEB | 140.245.95.162 | nginx + Spring Boot | A1.Flex |
| DB | 161.33.138.23 | MariaDB | A1.Flex |

> Always Free 한도: A1.Flex 2OCPU/12GB 공유
> 
> 인프라 설계 상세: https://github.com/SungHoonGit/OCI

## 빌드 및 실행

```bash
# 전체 빌드
./gradlew build

# auth 모듈만 빌드
./gradlew :sh-platform-auth:build

# 실행
./gradlew :sh-platform-auth:bootRun
```

## 관련 문서

- [아키텍처 설계](docs/architecture/platform-architecture-design.md)
- [인증 API 명세](docs/auth/api-auth.md)
- [프론트엔드 인증 가이드](docs/auth/frontend-auth-guide.md)
- [도메인/SSL 설정](docs/infra/domain-ssl-setup-guide.md)
