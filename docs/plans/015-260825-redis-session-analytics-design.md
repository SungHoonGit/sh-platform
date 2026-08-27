# 015-260825 Redis + Kafka + 세션 관리 + 애널리틱스 설계 문서

## 개요
- **목적**: Redis 도입을 통한 세션 관리, JWT 블랙리스트, Rate Limiter 고도화, 로그/애널리틱스 + Kafka 확장성 확보
- **범위**: Redis 인프라 설치, auth 모듈 세션 관리, 공통 모듈 Rate Limiter, 관리자 대시보드, 메시지 큐 인터페이스
- **작성일**: 2026-08-25
- **작성자**: AI Assistant / 사용자

---

## 1. 배경 및 이유

### 1.1 현재 상태

| 항목 | 현재 구현 | 문제점 |
|------|-----------|--------|
| **인증** | JWT RS256 + STATELESS | 토큰 만료 전까지 무효화 불가 (로그아웃 해도 토큰 유효) |
| **RefreshToken** | MariaDB `refresh_tokens` 테이블 | DB I/O, 분산 확장 시 병목 |
| **Rate Limiter** | `ConcurrentHashMap` (인메모리) | 서버 재시작 시 초기화, 멀티 인스턴스 미적용 |
| **로그** | systemd journald + 파일 로그 | 분석 불가, 실시간 대시보드 없음 |
| **세션 추적** | 없음 | 동시 로그인 제어 불가 |

### 1.2 왜 Redis인가?

Redis = in-memory data store (단순 캐시가 아닌 "데이터 구조 서버")

| 특징 | 설명 | MSA에서의 의미 |
|------|------|----------------|
| **속도** | 메모리 기반 (μs 레이턴시) | 매 요청마다 토큰 검증에 적합 |
| **TTL (자동 만료)** | 키별로 시간 설정 가능 | 세션/토큰/캐시 자동 만료 |
| **원자적 연산** | `INCR`, `EXPIRE` 등 | Rate Limiter 카운터에 적합 |
| **데이터 구조** | String, Hash, List, Set, Sorted Set | 용도별 최적 구조 선택 가능 |
| **파일리시스턴스** | RDB/AOF 백업 | 서버 재시작 후 데이터 복원 |
| **Pub/Sub** | 실시간 메시지 | MSA 간 이벤트 통신 (경량 큐) |

### 1.3 대용량 트래픽과 Redis

```
MSA 아키텍처에서 Redis의 위치:

┌──────────┐  ┌──────────┐  ┌──────────┐
│  Auth    │  │ Scraper  │  │  Resume  │
│ Service  │  │ Service  │  │ Service  │
└────┬─────┘  └────┬─────┘  └────┬─────┘
     │             │             │
     └─────────┬───┴─────────────┘
               │
          ┌────▼────┐
          │  Redis  │  ← 모든 서비스가 공유
          └─────────┘
               │
          ┌────▼────┐
          │ MariaDB │  ← 비즈니스 데이터만
          └─────────┘
```

**왜 "항상" 있는가:**
- 대용량 트래픽 → DB에 직접 접근하면 병목 → Redis가 캐시/버퍼 역할
- 분산 세션 → 각 인스턴스가 독립 세션을 가지면 사용자가 로그인마다 다른 서버에 배정될 수 있음 → Redis가 중앙 세션 스토어
- Rate Limiting → 서버 재시작해도 카운터 유지

---

## 2. 현재 아키텍처 분석

### 2.1 인증 흐름

```
사용자 → 로그인 요청
  │
  ├─ RateLimiter (ConcurrentHashMap) ← 문제: 인메모리
  ├─ AuthService.login()
  │   ├─ JWT Access Token 발급 (1시간)
  │   ├─ Refresh Token 발급 (14일, MariaDB 저장)
  │   └─ 응답: { accessToken, refreshToken }
  │
  ├─ 이후 요청마다:
  │   ├─ JwtAuthenticationFilter → JWT 서명 검증 (RSA 공개키)
  │   ├─ 유효하면 Principal 설정
  │   └─ 만료되면 401
  │
  └─ Refresh:
      ├─ RefreshToken이 MariaDB에 있으면 새 Access Token 발급
      └─ 없으면 401
```

### 2.2 문제점 상세

**1) JWT 무효화 불가**
로그아웃해도 Access Token은 1시간 동안 유효 → 탈취된 토큰은 만료 전까지 사용 가능 → 보안 취약점

**2) Rate Limiter 인메모리**
ConcurrentHashMap → 서버 재시작 시 0으로 초기화 → 공격자가 서버 재시작 유도하면 rate limit 초기화 → MSA 확장 시 각 인스턴스가 독립 카운터 유지

**3) 세션 추적 없음**
동시 로그인 제어 불가 → 같은 계정으로 무제한 로그인 가능 → 보안/비용 문제

**4) 로그 분석 불가**
파일 로그 → 사람이 읽어야 함 → 패턴 분석, 이상 탐지, 대시보드 불가

---

## 3. 요구 사항

### 3.1 기능 요구 사항

| ID | 요구 사항 | 우선순위 |
|----|-----------|----------|
| FR-001 | Redis 인메모리 데이터 스토어 설치 | 필수 |
| FR-002 | JWT 블랙리스트 (로그아웃 시 토큰 무효화) | 필수 |
| FR-003 | 동시 로그인 제어 (유저당 세션 수 제한) | 필수 |
| FR-004 | Rate Limiter Redis 마이그레이션 | 필수 |
| FR-005 | Refresh Token Redis 저장 (MariaDB 대체) | 권장 |
| FR-006 | 로그인 이력/애널리틱스 수집 | 권장 |
| FR-007 | 관리자 세션 관리 페이지 | 권장 |
| FR-008 | 요청 큐/이벤트 스트림 (Pub/Sub) | 탐색 |

### 3.2 비기능 요구 사항

| 항목 | 요구 사항 |
|------|-----------|
| **성능** | 토큰 검증 < 1ms, Rate Limit 체크 < 1ms |
| **가용성** | Redis 장애 시 기존 동작 유지 (graceful degradation) |
| **디스크** | A1.Flex 6GB RAM 중 Redis 512MB ~ 1GB 할당 |
| **보안** | Redis 비밀번호 설정, 외부 바인딩 금지 (127.0.0.1만) |
| **백업** | RDB 스냅샷 + AOF (선택) |

---

## 4. 설계

### 4.1 Redis 인프라

```
서버 (OCI A1.Flex 1/6GB, ARM64)
├── auth service (:8080)
├── scraper service (:8081)
├── resume service (:8082)
├── nginx
├── prometheus / grafana / promtail
└── Redis (:6379) ← 신규 (127.0.0.1 only)
```

Redis 설정:
```conf
# /etc/redis/redis.conf
bind 127.0.0.1           # 외부 접근 불가
port 6379
requirepass <password>   # 비밀번호
maxmemory 512mb          # 메모리 제한
maxmemory-policy allkeys-lru  # 초과 시 LRU 기반 제거
save 900 1               # RDB 스냅샷
save 300 10
appendonly yes           # AOF 활성화
```

### 4.2 Redis 키 구조

```
# JWT 블랙리스트
blacklist:{token_hash}          → "1"     TTL: Access Token 남은 시간

# 사용자 세션
session:{userId}:{sessionId}    → Hash    TTL: 180분
  ├── device: "Chrome/Windows"
  ├── ip: "192.168.1.1"
  ├── loginAt: "2026-08-25T10:00:00"
  └── lastActiveAt: "2026-08-25T11:30:00"

# 동시 로그인 추적
user:sessions:{userId}          → Set     (세션 삭제 시 멤버 제거)

# Rate Limiter
ratelimit:{ip}:{uri}            → Hash
  ├── count: 5
  └── window_start: 1692931200

# Refresh Token
refresh:{token_hash}            → String  TTL: 14일

# 로그인 이력 (애널리틱스)
login:log:{date}                → List    TTL: 90일
  └── JSON: { userId, email, ip, device, success, timestamp }

# 캐시
cache:{key}                     → String  TTL: 5분
```

### 4.3 아키텍처 변경 비교

**변경 전:**
```
Auth Service → MariaDB (refresh_tokens)
RateLimiter: ConcurrentHashMap (인메모리, 서비스별 독립)
로그: 파일 로그만
```

**변경 후:**
```
Auth Service → Redis (블랙리스트, 세션, Rate Limiter, Refresh Token)
           → MariaDB (users, business data)
RateLimiter: Redis 기반 (전 서비스 공유)
로그: 파일 + Redis 로그인 이력 + Prometheus 메트릭
```

### 4.4 구현 단계

| Phase | 내용 | 기간 | 포함 |
|-------|------|------|------|
| **Phase 1** | Redis 설치 + 기본 연동 | 1일 | apt 설치, spring-data-redis, 연결 설정 |
| **Phase 2** | Rate Limiter 고도화 | 1일 | ConcurrentHashMap → Redis 기반 |
| **Phase 3** | JWT 블랙리스트 | 1일 | 로그아웃 시 토큰 해시 저장, 검증 시 확인 |
| **Phase 4** | 세션 관리 | 2일 | 동시 로그인 제어, 세션 CRUD |
| **Phase 5** | Refresh Token Redis 이전 | 1일 | MariaDB → Redis, TTL 자동 만료 |
| **Phase 6** | 로그인 이력/애널리틱스 | 1일 | 로그인 이력 수집, 관리자 API |
| **Phase 7** | 관리자 페이지 | 2일 | 세션 목록, 강제 로그아웃, 통계 대시보드 |

### 4.5 Redis 장애 시 graceful degradation

```
Redis 연결 실패 시:
├── Rate Limiter: ConcurrentHashMap으로 폴백
├── 블랙리스트 검증: 건너뜀 (토큰 서명만으로 검증)
├── 세션 추적: 비활성화 (로그인은 허용)
└── 에러 로그: 남김 (모니터링 알림)

→ Redis 장애가 전체 서비스 장애로 이어지지 않음
```

---

## 5. 동시 로그인 제어 상세

### 5.1 동작 방식

```
로그인 요청
  │
  ├─ 1. Redis에서 현재 활성 세션 수 확인
  │     user:sessions:{userId} → SCARD → 세션 수
  │
  ├─ 2. 세션 수 < 최대 허용?
  │     ├─ Yes → 새 세션 등록 + JWT 발급
  │     └─ No  → 정책에 따라 처리:
  │              ├─ maxSessionsPreventsLogin=false → 이전 세션 만료 후 로그인
  │              └─ maxSessionsPreventsLogin=true  → 로그인 차단
  │
  └─ 3. 새 세션 Redis에 저장
        session:{userId}:{sessionId} = { device, ip, loginAt }
        user:sessions:{userId} → SADD sessionId
```

### 5.2 설정

```yaml
# application.yml
app:
  session:
    max-per-user: 3              # 유저당 최대 세션 수
    timeout-minutes: 180         # 세션 만료 시간
    prevent-duplicate: false     # true=로그인 차단, false=이전 세션 만료
```

---

## 6. 애널리틱스/로그 관리

### 6.1 수집 항목

| 항목 | 설명 | Redis 구조 |
|------|------|-----------|
| 로그인 성공/실패 | IP, 기기, 시간 | `login:log:{date}` List |
| 활성 세션 수 | 실시간 카운터 | `user:sessions:{userId}` Set |
| API 호출 통계 | 엔드포인트별 호출 횟수 | `stats:api:{date}:{endpoint}` Hash |
| 에러 발생 | 에러 유형별 카운터 | `stats:error:{date}:{type}` Hash |

### 6.2 관리자 대시보드

```
/admin/analytics
├── 실시간 대시보드
│   ├── 현재 활성 세션 수
│   ├── 오늘 로그인 횟수 (성공/실패)
│   ├── API 호출 트래픽 (시간대별)
│   └── 에러 발생률
├── 세션 관리
│   ├── 사용자별 세션 목록
│   ├── 강제 로그아웃
│   └── 특정 사용자 세션 제한
└── 로그 검색
    ├── 로그인 이력 (날짜/사용자/IP별)
    └── 이상 탐지 (비정상 로그인 패턴)
```

---

## 7. 요청 큐 / Pub/Sub (탐색)

### 7.1 Redis Pub/Sub 활용 가능 항목

| 항목 | 설명 | 적용 시점 |
|------|------|-----------|
| 크롤링 이벤트 | 스크래퍼에서 크롤링 완료 시 플랫폼에 알림 | Phase 8+ |
| 이메일 큐 | 이메일 발송 요청을 큐에 넣고 비동기 처리 | Phase 8+ |
| 실시간 알림 | SSE 대신 Redis Pub/Sub로 실시간 알림 | 탐색 |
| 분산 락 | 여러 인스턴스에서 동시에 실행되면 안 되는 작업 | 필요 시 |

### 7.2 주의사항

Redis Pub/Sub는 **메시지 보장이 없음** (.subscribe하는 클라이언트가 없으면 메시지 손실).
- **중요한 메시지** → MariaDB + 폴링 또는 전용 메시지 브로커(RabbitMQ/Kafka) 필요
- **경량 알림/이벤트** → Redis Pub/Sub 적합

---

## 8. 리스크 및 대응

| 리스크 | 영향 | 대응 |
|--------|------|------|
| Redis 서버 다운 | 세션/블랙리스트 접근 불가 | graceful degradation (폴백 로직) |
| 메모리 부족 | Redis OOM 종료 | maxmemory 설정 + LRU 정책 |
| 데이터 유실 | 블랙리스트/세션 초기화 | RDB + AOF (단, 블랙리스트는 유실되어도 토큰 만료로 자동 해결) |
| Redis 연결 끊김 | 모든 서비스 영향 | 연결 풀 + 재연결 로직 + 폴백 |

---

## 9. 참고 자료

- [Redis 공식 문서](https://redis.io/docs/)
- [Spring Data Redis](https://spring.io/projects/spring-data-redis)
- [Spring Session](https://spring.io/projects/spring-session)
- [JWT 블랙리스트 패턴](https://redis.com/ebook/everything-you-need-to-know-about-redis-session-management/)
- [Redis Rate Limiting](https://redis.io/docs/manual/psubscribe/#pattern-based-rate-limiting)

---
*작성일: 2026-08-25*
