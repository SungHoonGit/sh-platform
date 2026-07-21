---
title: Account Linking Design
description: Account Linking Design - auth module documentation
category: auth
created: 2026-07-13
updated: 2026-07-21
---

# OAuth2 계정 연결 (Account Linking) 설계

## 개요

사용자가 여러 OAuth2 프로바이더(카카오, 네이버, 구글 등)로 로그인할 때, 같은 사람의 계정을 하나로 통합하는 방안을 설계합니다.

## 문제점

### 현재 동작 (Phase 3)
- 각 프로바이더별 별도 계정 생성
- 같은 사람이 카카오와 네이버로 가입하면 별도 계정 2개
- 사용자가 어떤 계정인지 혼란

### 예시
```
users 테이블:
id | email          | provider | providerId
1  | user@kakao.com | KAKAO    | 12345678
2  | user@naver.com | NAVER    | 87654321
```

→ 같은 사람이 2개의 별도 계정을 보유

## 해결 방안: 계정 연결

### 1. 테이블 구조

**users 테이블 (기본 정보):**
```sql
CREATE TABLE users (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    email VARCHAR(255) UNIQUE NOT NULL,
    name VARCHAR(100),
    role VARCHAR(20) DEFAULT 'USER',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);
```

**user_providers 테이블 (프로바이더 연결):**
```sql
CREATE TABLE user_providers (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    provider VARCHAR(20) NOT NULL,      -- KAKAO, NAVER, GOOGLE, GITHUB
    provider_id VARCHAR(255) NOT NULL,   -- 프로바이더별 고유 ID
    provider_email VARCHAR(255),         -- 프로바이더 이메일
    connected_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id),
    UNIQUE KEY uk_provider_provider_id (provider, provider_id)
);
```

### 2. 동작 플로우

```
1. 사용자가 카카오로 로그인
   → 이메일(user@test.com) 확인
   → 기존 사용자 없음 → 새 계정 생성

2. 같은 사람이 네이버로 로그인
   → 이메일(user@test.com) 확인
   → 기존 사용자 발견 → "이미 가입된 이메일입니다. 연결하시겠습니까?"

3. 사용자가 "예" 선택
   → user_providers에 네이버 추가
   → 같은 계정으로 로그인

4. 사용자가 "아니오" 선택
   → 새 계정 생성 (별도 계정)
```

### 3. 사용자 경험

| 상황 | 화면 | 동작 |
|------|------|------|
| 첫 번째 프로바이더 등록 | 자동 가입 | 새 계정 생성 |
| 두 번째 프로바이더 등록 | 연결 확인 | "이미 가입된 이메일. 연결하시겠습니까?" |
| 로그인 시 | 프로바이더 선택 | 연결된 프로바이더 nào로든 로그인 가능 |
| 설정 화면 | 프로바이더 관리 | "연결된 소셜 계정: 카카오 ✅, 네이버 ✅" |

### 4. API 설계

**계정 연결 확인:**
```
POST /api/v1/auth/oauth2/link-check
Request:
{
    "email": "user@test.com",
    "provider": "NAVER",
    "providerId": "87654321"
}

Response 200:
{
    "code": "SUCCESS",
    "data": {
        "exists": true,
        "existingProvider": "KAKAO",
        "message": "이미 가입된 이메일입니다. 연결하시겠습니까?"
    }
}
```

**계정 연결:**
```
POST /api/v1/auth/oauth2/link
Header: Authorization: Bearer {accessToken}
Request:
{
    "provider": "NAVER",
    "providerId": "87654321",
    "providerEmail": "user@naver.com"
}

Response 200:
{
    "code": "SUCCESS",
    "message": "계정이 연결되었습니다."
}
```

**연결된 프로바이더 목록:**
```
GET /api/v1/auth/oauth2/providers
Header: Authorization: Bearer {accessToken}

Response 200:
{
    "code": "SUCCESS",
    "data": {
        "providers": [
            {"provider": "KAKAO", "connectedAt": "2026-07-12"},
            {"provider": "NAVER", "connectedAt": "2026-07-13"}
        ]
    }
}
```

**프로바이더 연결 해제:**
```
DELETE /api/v1/auth/oauth2/providers/{provider}
Header: Authorization: Bearer {accessToken}

Response 200:
{
    "code": "SUCCESS",
    "message": "프로바이더 연결이 해제되었습니다."
}
```

### 5. 보안 고려사항

| 항목 | 처리 방안 |
|------|----------|
| 최소 1개 프로바이더 유지 | 마지막 프로바이더는 연결 해제 불가 |
| 이메일 변경 | 프로바이더 이메일과 시스템 이메일 분리 관리 |
| 프로바이더 탈퇴 | 연결 해제 처리, 계정은 유지 |
| 중복 연결 방지 | (provider, provider_id) 유니크 키 |

### 6. 구현 시점

| 시점 | 작업 |
|------|------|
| Phase 3 (현재) | 기본 OAuth2 로그인만 구현 |
| Phase 4 | 계정 연결 기능 추가 |

### 7. 마이그레이션 전략

**기존 사용자 처리:**
1. 현재 users 테이블에 provider, provider_id 컬럼 제거
2. user_providers 테이블로 이전
3. 기존 사용자에 대해 기본 프로바이더 연결

**변경 쿼리:**
```sql
-- 1. user_providers 테이블 생성
CREATE TABLE user_providers (...);

-- 2. 기존 사용자 데이터 이전
INSERT INTO user_providers (user_id, provider, provider_id)
SELECT id, provider, provider_id FROM users;

-- 3. users 테이블에서 provider, provider_id 컬럼 제거
ALTER TABLE users DROP COLUMN provider;
ALTER TABLE users DROP COLUMN provider_id;
```

## 결론

계정 연결 기능은 사용자 편의성을 높이는 중요한 기능입니다. 현재 Phase 3에서는 기본 OAuth2 로그인만 구현하고, Phase 4에서 계정 연결 기능을 추가하는 것을 추천합니다.
