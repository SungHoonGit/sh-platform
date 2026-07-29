# SaaS 테넌트 관리 기획 설계 문서

> 이 문서는 SH Platform의 SaaS 테넌트 관리 아키텍처를 분석하고 설계합니다.
> 작성일: 2026-07-14

---

## 1. SaaS 테넌트 관리란?

### 1.1 정의

**테넌트(Tenant)**: SaaS 플랫폼을 사용하는 개별 고객/조직/회사

```
일반 SaaS:
  사용자 A → 플랫폼 로그인 → 자신의 데이터만 접근
  사용자 B → 플랫폼 로그인 → 자신의 데이터만 접근

SaaS 테넌트:
  회사 A (테넌트) → 직원 5명 → 회사 A 데이터만 접근
  회사 B (테넌트) → 직원 10명 → 회사 B 데이터만 접근
```

### 1.2 왜 필요한가?

| 필요성 | 설명 |
|--------|------|
| **데이터 격리** | 테넌트 간 데이터 접근 차단 |
| **멀티 테넌트** | 하나의 인스턴스로 여러 고객 서비스 |
| **비용 효율** | 인프라 공유로 비용 절감 |
| **확장성** | 새로운 테넌트 추가 용이 |
| **커스터마이징** | 테넌트별 설정 분리 |

---

## 2. 유명 SaaS 플랫폼 분석

### 2.1 Slack

| 항목 | 내용 |
|------|------|
| **테넌트 구조** | 워크스페이스 = 테넌트 |
| **데이터 격리** | 워크스페이스별 완전 격리 |
| **구성원 관리** | 초대 기반, 역할 (Owner, Admin, Member) |
| **설정** | 워크스페이스별 커스터마이징 |
| **billing** | 워크스페이스별 구독 |
| **특징** | 사용자당 하나의 워크스페이스 소유 |

### 2.2 Notion

| 항목 | 내용 |
|------|------|
| **테넌트 구조** | 팀 = 테넌트 |
| **데이터 격리** | 팀별 격리, 게스트는 제한적 접근 |
| **구성원 관리** | 이메일 초대, 역할 (Owner, Admin, Member, Guest) |
| **설정** | 팀별 설정, 커스텀 도메인 |
| **billing** | 팀별 구독 |
| **특징** | 게스트 접근 제한, 팀 내 공유 |

### 2.3 Jira (Atlassian)

| 항목 | 내용 |
|------|------|
| **테넌트 구조** | 프로젝트 > 워크스페이스 |
| **데이터 격리** | 프로젝트별 격리 |
| **구성원 관리** | 역할 기반 (Admin, User, Viewer) |
| **설정** | 프로젝트별 워크플로우 |
| **billing** | 사용자 수 기반 과금 |
| **특징** | 복잡한 권한 구조, 프로젝트별 격리 |

### 2.4 Firebase (Google)

| 항목 | 내용 |
|------|------|
| **테넌트 구조** | 프로젝트 = 테넌트 |
| **데이터 격리** | 프로젝트별 완전 격리 (별도 DB 인스턴스) |
| **구성원 관리** | IAM 기반 역할 |
| **설정** | 프로젝트별 설정 |
| **billing** | 사용량 기반 과금 |
| **특징** | 프로젝트별 완전 격리, IAM 권한 관리 |

### 2.5 AWS

| 항목 | 내용 |
|------|------|
| **테넌트 구조** | 계정 = 테넌트 |
| **데이터 격리** | 리전별 격리, VPC 격리 |
| **구성원 관리** | IAM 기반, 복잡한 권한 구조 |
| **설정** | 서비스별 개별 설정 |
| **billing** | 사용량 기반 과금 |
| **특징** | 완전 격리, 복잡한 권한 구조 |

### 2.6 분석 종합

| 플랫폼 | 테넌트 모델 | 격리 수준 | 권한 구조 | 과금 모델 |
|--------|------------|----------|----------|----------|
| Slack | 워크스페이스 | 완전 격리 | Owner/Admin/Member | 구독 |
| Notion | 팀 | 팀별 격리 | Owner/Admin/Member/Guest | 구독 |
| Jira | 프로젝트 | 프로젝트별 격리 | Admin/User/Viewer | 사용자 수 |
| Firebase | 프로젝트 | 완전 격리 (별도 DB) | IAM 기반 | 사용량 |
| AWS | 계정 | 완전 격리 (VPC) | IAM 기반 | 사용량 |

---

## 3. 테넌트 격리 패턴 분석

### 3.1 격리 수준 비교

| 패턴 | 설명 | 장점 | 단점 | 적합 사례 |
|------|------|------|------|-----------|
| **Pool Model** | 모든 테넌트가 하나의 DB 공유 | 비용 효율적 | 격리 약함 | 소규모 SaaS |
| **Silo Model** | 테넌트별 별도 DB | 완전 격리 | 비용 높음 | 기업용 SaaS |
| **Bridge Model** | DB는 공유, 스키마는 분리 | 균형 | 구현 복잡 | 중규모 SaaS |
| **Row-Level Security** | 행 수준 보안 | 유연성 | 설정 복잡 | 다중 앱 SaaS |

### 3.2 격리 수준 선택 기준

| 기준 | Pool (현재) | Bridge (목표) | Silo |
|------|-------------|---------------|------|
| **비용** | 낮음 | 중간 | 높음 |
| **격리** | 약함 | 중간 | 강함 |
| **구현** | 쉬움 | 중간 | 어려움 |
| **확장** | 어려움 | 쉬움 | 매우 어려움 |

### 3.3 우리 상황 분석

```
현재: Pool Model (하나의 DB, 모든 테이블 공유)
목표: Bridge Model (DB 공유, 테넌트별 데이터 격리)

이유:
1. 비용 효율적 (OCI Free tier)
2. 충분한 격리 (테넌트 ID 기반 필터링)
3. 나중에 Silo로 확장 가능
```

---

## 4. SH Platform 테넌트 관리 아키텍처 설계

### 4.1 테넌트 모델 선택

```
단일 테넌트 모델 (기본)
→ 사용자당 1개 테넌트 소유
→ 나중에 다중 테넌트로 확장 가능
```

### 4.2 DB 스키마 설계

```sql
-- 1. 테넌트 테이블
CREATE TABLE sh_tenant (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL COMMENT 테넌트 이름,
    slug VARCHAR(50) UNIQUE NOT NULL COMMENT URL용 식별자,
    domain VARCHAR(100) COMMENT 커스텀 도메인,
    logo_url VARCHAR(500) COMMENT 로고 URL,
    status ENUM(ACTIVE,SUSPENDED,DELETED) DEFAULT ACTIVE,
    plan_type ENUM(FREE,BASIC,PRO,ENTERPRISE) DEFAULT FREE,
    max_users INT DEFAULT 5 COMMENT 최대 사용자 수,
    settings JSON COMMENT 테넌트별 설정,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- 2. 테넌트-사용자 관계
CREATE TABLE sh_tenant_member (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    role ENUM(OWNER,ADMIN,MEMBER,GUEST) DEFAULT MEMBER,
    status ENUM(ACTIVE,INVITED,SUSPENDED) DEFAULT INVITED,
    invited_at TIMESTAMP NULL,
    joined_at TIMESTAMP NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (tenant_id) REFERENCES sh_tenant(id),
    FOREIGN KEY (user_id) REFERENCES sh_user(id),
    UNIQUE KEY uk_tenant_user (tenant_id, user_id)
);

-- 3. 테넌트 초대장
CREATE TABLE sh_tenant_invitation (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_id BIGINT NOT NULL,
    email VARCHAR(200) NOT NULL,
    role ENUM(ADMIN,MEMBER,GUEST) DEFAULT MEMBER,
    token VARCHAR(100) UNIQUE NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    accepted_at TIMESTAMP NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (tenant_id) REFERENCES sh_tenant(id)
);

-- 4. 테넌트 로그/감사
CREATE TABLE sh_tenant_audit_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_id BIGINT NOT NULL,
    user_id BIGINT,
    action VARCHAR(50) NOT NULL,
    target_type VARCHAR(50),
    target_id BIGINT,
    details JSON,
    ip_address VARCHAR(45),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (tenant_id) REFERENCES sh_tenant(id)
);
```

### 4.3 ERD 다이어그램

```
┌────────────────────────────────────────────────────────────────────┐
│                          sh_tenant                                 │
├────────────────────────────────────────────────────────────────────┤
│ PK │ id                BIGINT                                      │
│    │ name              VARCHAR(100)  NN                            │
│ UQ │ slug              VARCHAR(50)   NN                            │
│    │ domain            VARCHAR(100)                                │
│    │ logo_url          VARCHAR(500)                                │
│    │ status            ENUM          DEFAULT ACTIVE                 │
│    │ plan_type         ENUM          DEFAULT FREE                  │
│    │ max_users         INT           DEFAULT 5                     │
│    │ settings          JSON                                       │
│    │ created_at        TIMESTAMP     NN                            │
│    │ updated_at        TIMESTAMP     NN (ON UPDATE)                │
└────────────────────┬───────────────────────────────────────────────┘
                     │ 1
                     │
                     │ N
┌────────────────────┴───────────────────────────────────────────────┐
│                      sh_tenant_member                               │
├────────────────────────────────────────────────────────────────────┤
│ PK │ id                BIGINT                                      │
│ FK │ tenant_id         BIGINT        NN                            │
│ FK │ user_id           BIGINT        NN                            │
│    │ role              ENUM          DEFAULT MEMBER                 │
│    │ status            ENUM          DEFAULT INVITED                │
│    │ invited_at        TIMESTAMP     NULL                          │
│    │ joined_at         TIMESTAMP     NULL                          │
│    │ created_at        TIMESTAMP     NN                            │
│ UQ │ uk_tenant_user    (tenant_id, user_id)                        │
└────────────────────────────────────────────────────────────────────┘

┌────────────────────────────────────────────────────────────────────┐
│                    sh_tenant_invitation                             │
├────────────────────────────────────────────────────────────────────┤
│ PK │ id                BIGINT                                      │
│ FK │ tenant_id         BIGINT        NN                            │
│    │ email             VARCHAR(200)  NN                            │
│    │ role              ENUM          DEFAULT MEMBER                 │
│ UQ │ token             VARCHAR(100)  NN                            │
│    │ expires_at        TIMESTAMP     NN                            │
│    │ accepted_at       TIMESTAMP     NULL                          │
│    │ created_at        TIMESTAMP     NN                            │
└────────────────────────────────────────────────────────────────────┘

┌────────────────────────────────────────────────────────────────────┐
│                    sh_tenant_audit_log                              │
├────────────────────────────────────────────────────────────────────┤
│ PK │ id                BIGINT                                      │
│ FK │ tenant_id         BIGINT        NN                            │
│ FK │ user_id           BIGINT                                      │
│    │ action            VARCHAR(50)   NN                            │
│    │ target_type       VARCHAR(50)                                 │
│    │ target_id         BIGINT                                      │
│    │ details           JSON                                       │
│    │ ip_address        VARCHAR(45)                                 │
│    │ created_at        TIMESTAMP     NN                            │
└────────────────────────────────────────────────────────────────────┘
```

---

## 5. API 엔드포인트 설계

### 5.1 테넌트 관리

```
POST   /api/v1/tenants              - 테넌트 생성
GET    /api/v1/tenants              - 내 테넌트 목록
GET    /api/v1/tenants/{id}         - 테넌트 상세
PUT    /api/v1/tenants/{id}         - 테넌트 수정
DELETE /api/v1/tenants/{id}         - 테넌트 삭제
```

### 5.2 테넌트 멤버 관리

```
GET    /api/v1/tenants/{id}/members - 멤버 목록
POST   /api/v1/tenants/{id}/members - 멤버 초대
PUT    /api/v1/tenants/{id}/members/{userId} - 멤버 역할 변경
DELETE /api/v1/tenants/{id}/members/{userId} - 멤버 제거
```

### 5.3 초대장 관리

```
POST   /api/v1/invitations/{token}/accept - 초대 수락
DELETE /api/v1/invitations/{token}         - 초대 취소
```

### 5.4 테넌트 설정

```
GET    /api/v1/tenants/{id}/settings - 설정 조회
PUT    /api/v1/tenants/{id}/settings - 설정 수정
```

---

## 6. 테넌트 컨텍스트 관리

### 6.1 요청마다 테넌트 확인

```java
// 테넌트 컨텍스트
public class TenantContext {
    private static final ThreadLocal<Tenant> currentTenant = new ThreadLocal<>();
    
    public static void setCurrent(Tenant tenant) {
        currentTenant.set(tenant);
    }
    
    public static Tenant getCurrent() {
        return currentTenant.get();
    }
    
    public static void clear() {
        currentTenant.remove();
    }
}

// 인터셉터
@Component
public class TenantInterceptor implements HandlerInterceptor {
    
    @Override
    public boolean preHandle(HttpServletRequest request, 
                            HttpServletResponse response, 
                            Object handler) {
        // 1. JWT에서 user_id 추출
        Long userId = extractUserId(request);
        
        // 2. user_id로 테넌트 확인
        Tenant tenant = tenantService.getTenantByUserId(userId);
        
        // 3. 테넌트 컨텍스트에 저장
        TenantContext.setCurrent(tenant);
        
        return true;
    }
    
    @Override
    public void afterCompletion(HttpServletRequest request, 
                               HttpServletResponse response, 
                               Object handler, Exception ex) {
        TenantContext.clear();
    }
}
```

### 6.2 데이터 격리 전략

```java
// 테넌트별 데이터 격리
@Repository
public class BoardRepository {
    
    @Query("SELECT b FROM Board b WHERE b.tenantId = :tenantId")
    List<Board> findByTenantId(@Param("tenantId") Long tenantId);
    
    // 자동 테넌트 필터링
    @Query("SELECT b FROM Board b WHERE b.tenantId = :#{#tenantContext.id}")
    List<Board> findAll();
}
```

---

## 7. 테넌트 설정 관리

### 7.1 테넌트별 설정 항목

```json
{
  "theme": "light",
  "language": "ko",
  "timezone": "Asia/Seoul",
  "notifications": {
    "email": true,
    "push": false
  },
  "features": {
    "board": true,
    "shop": false
  }
}
```

### 7.2 설정 API

```
GET    /api/v1/tenants/{id}/settings - 설정 조회
PUT    /api/v1/tenants/{id}/settings - 설정 수정
```

---

## 8. 보안 고려사항

| 항목 | 내용 |
|------|------|
| **테넌트 격리** | 테넌트 간 데이터 접근 불가 |
| **인증/인가** | JWT에 테넌트 ID 포함 |
| **감사 로그** | 모든 변경 사항 기록 |
| **데이터 암호화** | 민감 데이터 암호화 저장 |

---

## 9. 구현 단계

### Phase 1: 기본 테넌트 관리 (1주)

| 작업 | 설명 |
|------|------|
| DB 스키마 생성 | sh_tenant, sh_tenant_member 테이블 |
| 테넌트 CRUD API | 생성/조회/수정/삭제 |
| 테넌트 컨텍스트 | 요청마다 테넌트 확인 |

### Phase 2: 멤버 관리 (1주)

| 작업 | 설명 |
|------|------|
| 멤버 초대 | 이메일 초대, 토큰 발급 |
| 멤버 수락 | 초대 수락, 역할 설정 |
| 멤버 관리 | 역할 변경, 제거 |

### Phase 3: 데이터 격리 (1주)

| 작업 | 설명 |
|------|------|
| 테넌트 필터링 | 모든 쿼리에 테넌트 ID 추가 |
| 권한 검증 | 테넌트 멤버만 접근 가능 |
| 격리 테스트 | 테넌트 간 데이터 격리 확인 |

### Phase 4: UI/UX (2주)

| 작업 | 설명 |
|------|------|
| 테넌트 선택 UI | 테넌트 전환, 생성 |
| 멤버 관리 UI | 초대, 역할 관리 |
| 설정 UI | 테넌트 설정 변경 |

---

## 10. 참고 자료

- [AWS Multi-Tenant Architecture](https://aws.amazon.com/blogs/architecture/patterns-for-multi-tenant-saas-architecture/)
- [Azure SaaS Data Isolation](https://learn.microsoft.com/en-us/azure/architecture/guide/multitenant/overview)
- [Slack Architecture](https://slack.engineering/)
- [Notion Architecture](https://www.notion.so/blog)
