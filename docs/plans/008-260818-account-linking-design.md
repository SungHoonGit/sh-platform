# 008-260818 계정 통합(Account Linking) 설계 문서

## 개요
- **목적**: 간편 로그인(카카오/네이버/구글/GitHub)과 이메일+비밀번호 로그인이 공존할 때, 계정을 어떻게 통합하고 관리할지 분석·설계
- **범위**: 계정 자동 연결 규칙, 계정 설정 UI, 보안 대책, API 설계
- **작성일**: 2026-08-18
- **작성자**: AI Assistant / 사용자

## 1. 배경 및 이유

### 1.1 현재 상태
- 로그인 수단: 이메일+비밀번호, 카카오, 네이버, 구글, GitHub (5가지)
- 현재 `CustomOAuth2UserService.findOrCreateUser`가 **이메일이 같으면 자동 연결**하고 있음
- 소셜 전용 계정은 비밀번호가 `null` (이메일 로그인 불가)
- 계정 설정 화면 없음 — 연결된 수단 확인/해제 불가

### 1.2 문제 인식
- 카카오/네이버/구글/GitHub 이메일이 동일한 사용자가 여럿일 수 있음
- 자동 연결이 **보안 취약점**이 될 수 있음 (계정 탈취 위험)
- 사용자가 자신의 계정에 어떤 로그인 수단이 연결되어 있는지 알 수 없음
- 소셜 전용 계정에 비밀번호를 설정할 방법이 없음

## 2. 요구 사항

### 2.1 기능 요구 사항
- [ ] FR-001: 같은 이메일의 소셜 로그인 자동 연결 (단, 보안 조건 충족 시)
- [ ] FR-002: 계정 설정 페이지 — 연결된 로그인 수단 목록 표시
- [ ] FR-003: 로그인 수단 추가 연결 (이미 로그인된 상태에서)
- [ ] FR-004: 로그인 수단 연결 해제
- [ ] FR-005: 소셜 전용 계정의 비밀번호 설정
- [ ] FR-006: 대표 계정(이메일) 표시 및 관리

### 2.2 비기능 요구 사항
- **보안**: 이메일 자동 연결 시 계정 탈취(ATO) 방지 — 미검증 이메일로 기존 계정에 연결 금지
- **UX**: 최소한의 방해 (사용자가 인지 못하는 사이 연결)
- **감사**: 연결/해제 이벤트 로깅

## 3. 업계 표준 분석

### 3.1 세 가지 연결 방식

| 방식 | 설명 | 장점 | 단점 |
|------|------|------|------|
| **자동 연결** | 같은 이메일이면 백그라운드로 연결 | UX가 매끄러움, 프롬프트 없음 | 이메일만으로 판단하면 **ATO 위험** |
| **링크-온-로그인** | 이메일 충돌 시 기존 계정 비밀번호로 본인 확인 후 연결 | 소유권 검증 확실 | 한 단계 추가 (사용자 인지) |
| **명시적 연결** | 로그인된 사용자가 설정 화면에서 수동 연결 | 소유권 모호함 없음, 안전 | 사용자가 직접 해야 함 |

### 3.2 업계 사례
- **사람인/잡코리아**: 자동 연결 기반. 소셜 세션을 유지해 재로그인 시 자동 진입
- **대부분의 CIAM(LoginRadius, Auth0 등)**: **자동 연결 + 검증된 이메일(verified) 조건 필수**
  - `email_verified` 플래그를 확인하지 않고 이메일만으로 연결하면 **계정 탈취 경로**가 됨 (GHSA-8mcf-rp68-xhfg, GHSA-g38m-r43w-p2q7 실제 사례)
  - "사전 등록(pre-register)된 미검증 이메일"에 공격자의 OAuth 아이덴티티가 묶여 피해자 계정 탈취
- **보안 원칙**:
  - 이메일 문자열이 아닌 **iss + sub (안정적인 Provider 사용자 ID)** 로 식별
  - 이메일로 연결할 때는 반드시 **이메일 인증 여부(verified)** 확인
  - 고위험 상황(새 기기, 이상 IP)에서는 **연결 전 재인증(step-up)**
  - 연결/해제는 **감사 로그** 필수

### 3.3 이메일 변경/프로바이더 불일치 (엣지 케이스)
- Google은 개인 Gmail 사용자의 이메일이 바뀔 수 있음 → DB의 이메일과 불일치
- Apple Sign-In은 프록시 이메일 반환 → 자동 매칭 실패 → **명시적 연결** 폴백 필요
- 결론: **식별자는 이메일이 아닌 Provider ID** 기준이 안전

## 4. 설계

### 4.1 계정 연결 규칙 (권장안)

```
소셜 로그인 성공 시
├── 1. 같은 (provider, providerId) 연결 확인 → 있으면 로그인
├── 2. 없으면 → 이메일로 기존 사용자 조회
│     ├── 기존 계정의 emailVerified == true → 자동 연결  ✅
│     └── 기존 계정의 emailVerified == false → 연결 금지 + 에러 (링크-온-로그인 유도) ⚠️
└── 3. 없으면 → 새 사용자 생성 (비밀번호 null, emailVerified true)
```

> **핵심 변경**: 현재 코드(`CustomOAuth2UserService.java:62-68`)는 emailVerified 조건 없이 자동 연결 → **수정 필요**

### 4.2 데이터 모델

**UserEntity** (기존)
- `email` (대표 계정)
- `password` (null = 소셜 전용)
- `email_verified`

**UserProviderEntity** (기존, 연결된 수단)
- `user_id`
- `provider` (kakao/naver/google/github)
- `provider_id` (Provider의 stable user id)
- `provider_email`

> 별도 테이블 추가 불필요 — 기존 구조 활용

### 4.3 API 설계

| Method | Path | 설명 | 인증 |
|--------|------|------|------|
| GET | `/api/v1/auth/me` | 내 계정 정보 (이메일, 비밀번호 설정 여부, 연결된 providers) | JWT |
| POST | `/api/v1/auth/me/password` | 비밀번호 설정/변경 (소셜 전용 계정 포함) | JWT |
| POST | `/api/v1/auth/me/providers/{provider}` | 로그인 수단 추가 연결 (OAuth2 인증 시작) | JWT |
| DELETE | `/api/v1/auth/me/providers/{provider}` | 로그인 수단 해제 | JWT |

> 해제 시 유의: **마지막 남은 로그인 수단은 해제 불가** (계정 잠김 방지)

### 4.4 계정 설정 UI

```
내 계정
├── 이메일: user@example.com
├── 비밀번호: 설정됨 / 설정 안됨 (→ 설정 버튼)
└── 연결된 로그인 수단
    ├── 카카오   [연결됨]  [해제]
    ├── 네이버   [연결 안됨]  [연결]
    ├── 구글     [연결됨]  [해제]
    └── GitHub   [연결 안됨]  [연결]
```

- 플랫폼 프레임(`/platform/`) 대시보드에 "계정 설정" 페이지 추가

## 5. 구현 계획

| 단계 | 내용 | 예상 기간 |
|------|------|-----------|
| Phase 1 | 보안 수정: emailVerified 조건 추가 (자동 연결 제한) | 0.5일 |
| Phase 2 | GET /me API + 연결 수단 목록 조회 | 0.5일 |
| Phase 3 | 비밀번호 설정/변경 API | 0.5일 |
| Phase 4 | 로그인 수단 연결/해제 API (+ 마지막 수단 보호) | 1일 |
| Phase 5 | 계정 설정 페이지 UI | 1일 |

## 6. 참고 자료
- [LoginRadius: Account Linking for Social Login Without Takeovers](https://www.loginradius.com/blog/identity/account-linking-social-login-ux)
- [GHSA-8mcf-rp68-xhfg: OAuth account takeover via auto-linking](https://github.com/reviactyl/panel/security/advisories/GHSA-8mcf-rp68-xhfg)
- [GHSA-g38m-r43w-p2q7: ATO via OAuth auto-link to unverified pre-registered email](https://github.com/better-auth/better-auth/security/advisories/GHSA-g38m-r43w-p2q7)
- [Bastionary: Link-on-login vs explicit link](https://bastionary.com/blog/account-linking)
- [peal.dev: Social Login Gotchas (store provider stable ID, not email)](https://www.peal.dev/blog/social-login-account-linking-edge-cases-oauth)

---
*작성일: 2026-08-18*