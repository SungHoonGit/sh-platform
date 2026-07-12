# SH Platform 아키텍처 설계

## 1. 비전

**"여러 플랫폼의 접근 관리 및 인증을 중앙에서 관리하는 플랫폼"**

- 멀티테넌트 SaaS: 고객사별 공간 분리
- 마이크로서비스: 독립적 배포 및 확장
- 모니터링 통합: 플랫폼 내 모니터링 시스템 포함
- 점진적 성장: 개발하고 싶은 기능을 계속 추가

---

## 2. 아키텍처 패턴 비교

### 2.1 모놀리식 vs MSA vs 모놀리식 + 모듈화

| 패턴 | 설명 | 장점 | 단점 |
|------|------|------|------|
| **모놀리식** | 하나의 앱에 모든 기능 | 개발/배포 쉬움, 초반 빠름 | 확장 어려움, 복잡도 증가 시 유지보수 어려움 |
| **완전 MSA** | 모든 기능을 독립 마이크로서비스로 | 유연한 확장, 독립 배포 | 복잡도 높음, 초기 개발 느림 |
| **모놀리식 + 모듈화** | 모놀리식으로 시작하되 모듈화 | 빠른 시작 + 향후 MSA 전환 용이 | 모듈 경계 설계 필요 |

**추천: 모놀리식 + 모듈화 (Modular Monolith)**
- 현재 Spring Boot로 빠르게 시작
- 모듈 간 인터페이스 명확히 설정
- 향후 필요 시 MSA로 분리 가능

---

## 3. 도메인 전략 비교

### Option A: 단일 플랫폼 도메인

```
platform.sh-hoon.com (관리자)
├── auth-service
├── user-service
├── monitoring-service
└── ...
```

**장점:**
- 관리 용이: 하나의 도메인에서 모든 것 관리
- 인증/인가 간단: 중앙 집중식 관리
- CORS 설정 쉬움

**단점:**
- 고객사 브랜딩 어려움
- 도메인 확장 시 서브도메인 필수
- 멀티테넌트 시 테넌트별 커스터마이징 어려움

---

### Option B: 서브도메인 방식

```
admin.platform.com (관리자)
client1.platform.com (고객사1)
client2.platform.com (고객사2)
api.platform.com (API 게이트웨이)
```

**장점:**
- 테넌트별 독립적 배포 가능
- 브랜딩 유연성 (커스텀 도메인 연결 가능)
- 격리성 좋음

**단점:**
- DNS 관리 복잡
- SSL 인증서 관리 (wildcard 필요)
- 인증 쿠키 도메인 설정 어려움

---

### Option C: 독립 도메인 + 중앙 관리

```
platform.sh-hoon.com (중앙 관리)
shopping-mall.com (고객사1 - 독립 도메인)
blog-service.com (고객사2 - 독립 도메인)
```

**장점:**
- 완전한 브랜딩 자유
- 고객사가 자체 도메인 사용
- SaaS로서 상업적 가치 높음

**단점:**
- 구현 복잡도 매우 높음
- OAuth2 콜백 URL 관리 어려움
- 보안 설정 복잡

---

### Option D: 하이브리드 (추천)

```
[중앙 관리 플랫폼]
platform.sh-hoon.com
├── 관리 콘솔
├── API 게이트웨이
├── 인증 서비스
└── 모니터링

[고객사 앱 - 선택적 구성]
client1.sh-hoon.com  (기본)
client1.com          (커스텀 도메인 연결)
```

**장점:**
- 중앙 관리 + 유연한 확장
- 커스텀 도메인 선택 가능
- MSA 전환 시 분리 용이

**단점:**
- 초기 설계 복잡
- 라우팅/인증 설정 필요

---

## 4. 권장 아키텍처: 모놀리식 + 모듈화 + 하이브리드 도메인

### 4.1 전체 구조

```
sh-platform (Spring Boot 모놀리식)
│
├── [mod-auth] 인증/인가 모듈
│   ├── OAuth2 (카카오, 네이버, 구글, 깃험)
│   ├── JWT 관리
│   ├── 세션 관리
│   └── RBAC (역할 기반 접근 제어)
│
├── [mod-user] 사용자 관리 모듈
│   ├── 프로필 관리
│   ├── 테넌트별 사용자
│   └── 권한 관리
│
├── [mod-tenant] 테넌트 관리 모듈
│   ├── 테넌트 CRUD
│   ├── 테넌트별 설정
│   └── 사용량 추적
│
├── [mod-monitoring] 모니터링 모듈
│   ├── Prometheus 메트릭
│   ├── Grafana 대시보드
│   ├── 알림 규칙
│   └── 로그 수집
│
├── [mod-gateway] API 게이트웨이 모듈
│   ├── 라우팅
│   ├── Rate Limiting
│   ├── CORS 관리
│   └── SSL/TLS 관리
│
└── [core] 코어 모듈
    ├── 공통 util
    ├── 예외 처리
    ├── 설정
    └── DB 연결
```

### 4.2 모듈 간 의존성

```
[mod-gateway]
    ↓ (라우팅)
[mod-auth] ←→ [mod-user]
    ↓ (인증 확인)
[mod-tenant]
    ↓ (메트릭 수집)
[mod-monitoring]
```

---

## 5. 멀티테넌트 구현 전략

### 5.1 데이터베이스 격리 전략

| 전략 | 설명 | 장점 | 단점 |
|------|------|------|------|
| **DB 격리** | 테넌트별 독립 DB | 완전한 격리 | 비용 높음, 관리 복잡 |
| **Schema 분리** | 테넌트별 Schema |较好的 격리 | Schema 관리 필요 |
| **테이블 분리** | 테넌트 ID 컬럼 추가 | 구현 쉬움, 비용 낮음 | 격리 약함 |
| **Mixed** | 중요 데이터는 격리, 공통은 분리 | 유연성 | 복잡도 증가 |

**추천: 테이블 분리 (tenant_id 컬럼) + 중요 데이터는 별도 DB**

### 5.2 테넌트 식별

```
방식 1: 서브도메인 (client1.platform.com)
방식 2: 헤더 (X-Tenant-ID)
방식 3: JWT 클레임 (tenantId)
방식 4: 경로 (platform.com/client1/...)

추천: 서브도메인 + JWT 클레임 조합
```

---

## 6. MSA 전환 로드맵

### Phase 1: 모놀리식 (현재)
```
sh-platform (Spring Boot)
├── 모든 모듈 포함
├── 단일 DB (MariaDB)
└── 단일 배포
```

### Phase 2: 모듈 분리 (3-6개월 후)
```
sh-platform
├── auth-service (인증)
├── user-service (사용자)
├── tenant-service (테넌트)
└── gateway-service (게이트웨이)
```

### Phase 3: 완전 MSA (1년 후)
```
[인증 클러스터]
├── auth-service (복제)
└── session-service

[비즈니스 클러스터]
├── user-service
├── tenant-service
└── ...

[인프라 클러스터]
├── monitoring-service
├── log-service
└── gateway-service
```

---

## 7. 모니터링 통합

### 7.1 모니터링 아키텍처

```
[Spring Boot 앱]
    ↓ (메트릭 내보내기)
[Prometheus]
    ↓ (데이터 수집)
[Grafana]
    ↓ (대시보드 + 알림)
[알림 채널] (이메일, 슬랙, 웹훅)
```

### 7.2 수집 대상

| 대상 | 메트릭 |
|------|--------|
| JVM | 힙 사용량, GC 빈도, 스레드 수 |
| API | 요청 수, 응답 시간, 에러율 |
| DB | 연결 풀, 쿼리 시간, 연결 수 |
| OAuth2 | 로그인 성공/실패, 프로바이더별 |
| 시스템 | CPU, 메모리, 디스크 |

---

## 8. 구현 순서

### 즉시 (1-2주)
1. ✅ 현재 모놀리식 구조 유지
2. ✅ 모듈 구조 명확화 (auth, user, tenant, monitoring)
3. ✅ 프론트엔드 인증 모듈 구현

### 단기 (1-3개월)
1. 테넌트 관리 모듈 추가
2. RBAC (역할 기반 접근 제어) 구현
3. 모니터링 시스템 구축 (Prometheus + Grafana)
4. 프론트엔드 대시보드 구현

### 중기 (3-6개월)
1. API 게이트웨이 모듈 분리
2. 테넌트별 설정 관리
3. 사용량 추적 및 과금
4. 보안 강화 (WAF, Rate Limiting)

### 장기 (6개월-1년)
1. MSA 전환 검토
2. 커스텀 도메인 지원
3. 멀티 리전 배포
4. SSO/SAML 지원

---

## 9. 기술 스택

### 백엔드
- Spring Boot 3.x
- Spring Security + OAuth2
- Spring Data JPA
- MariaDB
- Redis (세션/캐시)
- Prometheus + Grafana

### 프론트엔드
- React (웹)
- React Native (모바일)
- TypeScript

### 인프라
- OCI Always Free
- Nginx (리버스 프록시)
- Docker (선택적)
- GitHub Actions (CI/CD)

---

## 10. 결론

**현실적인 접근:**
1. 현재: 모놀리식으로 빠르게 기능 구현
2. 향후: 모듈화를 통해 MSA 전환 준비
3. 도메인: 하이브리드 방식 (중앙 관리 + 커스텀 도메인 선택)

**핵심 원칙:**
- 빠른 시작 > 완벽한 설계
- 점진적 확장 > 처음부터 큰 설계
- 모듈 경계 명확 > 완전한 분리
