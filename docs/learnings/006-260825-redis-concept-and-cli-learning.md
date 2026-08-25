# 006-260825 Redis 개념 정리 — 인메모리 KV 저장소와 상태 관리

## 개요
- **주제**: Redis의 개념, MongoDB/RDB와의 차이, redis-cli로 데이터 확인하는 방법
- **학습일**: 2026-08-25
- **수준**: 초급 → 중급 (sh-platform 실전 적용 기반)

---

## 1. 개념 설명

### 1.1 Redis란?

**RE**mote **DI**ctionary **S**erver. **메모리에 key-value 형태로 데이터를 저장하는 저장소.**

```
MySQL/MongoDB : 디스크 기반 데이터베이스 (영속 보관이 목적)
Redis         : 메모리 기반 상태 저장소 (빠른 읽기/쓰기 + 자동 만료가 목적)
```

> ⚠️ 흔한 오해 교정: "MongoDB처럼 DB가 겹쳐진 것"이 아니다.
> MongoDB는 *도큐먼트(DB) 저장소*고 Redis는 *키-값 메모리 저장소*다.
> 둘은 대체 관계가 아니라 **보완 관계** — RDB에 "지워지면 안 되는 데이터"를,
> Redis에 "빨라야 하고 언젠가 사라져도 되는 상태(세션/토큰/카운터)"를 둔다.

### 1.2 왜 필요한가? (RDB만으로 안 되는 이유)

로그인 세션을 MariaDB에 저장하면:

| 문제 | 설명 |
|------|------|
| 느림 | 모든 API 요청마다 SELECT 쿼리 발생 |
| 부담 | 만료된 세션 청소를 스케줄러가 돌려야 함 |
| 확장성 | 서버 2대로 늘리면 세션 공유 문제 발생 |

Redis라면:
- 메모리 접근이라 **0.1ms 수준** (디스크 DB는 수 ms)
- **TTL 설정 시 자동 삭제** — 청소 코드 불필요
- 앱 서버들이 같은 Redis를 보므로 **누가 띄우든 세션 공유**

### 1.3 핵심 특징 3가지

1. **자료구조가 내장됨** — 단순 String뿐 아니라 List/Set/Hash/Sorted Set을 명령 하나로 조작
2. **TTL(만료)** — `EXPIRE key 180` 처럼 초 단위 수명 지정, 만료되면 자동 삭제
3. **단일 스레드** — 명령이 순차 처리되어 원자성이 보장됨 (race condition 걱정 적음). 대신 느린 명령(KEYS *)은 금물

### 1.4 자료구조 5형제 (이게 절반이다)

| 타입 | 비유 | 대표 명령 | sh-platform 사용처 |
|------|------|-----------|-------------------|
| String | 변수 하나 | SET / GET / INCR | refresh token (`refresh:<토큰>` → userId), rate limit 카운터 |
| Hash | 객체 한 개 | HSET / HGETALL | 세션 상세 (`session:2:<uuid>` = ip/device/loginAt) |
| Set | 중복 없는 집합 | SADD / SMEMBERS / SCARD | 유저별 세션 목록 (`user:sessions:2`) |
| List | 배열/큐 | LPUSH / LRANGE | 로그인 이력 (`login-log:*`) |
| Sorted Set | 순위표 | ZADD / ZRANGE | (미사용) LRU 세션, 리더보드용 |

---

## 2. 사용법 — "그래서 어떻게 확인한다는 거임?"

### 2.1 접속과 생존 확인

```bash
# 서버에서 접속 (-a는 비밀번호, 경고 떠도 무시 가능)
redis-cli -a '비밀번호'

PING          # → PONG 나오면 살아있음
DBSIZE        # → 현재 저장된 키 개수
INFO memory   # → 메모리 사용량
```

### 2.2 키 찾기 — "무엇이 저장돼 있지?"

```bash
KEYS *                    # ⚠️ 전체 조회 — 운영서버 금지(순간 정지 위험), 로컬에서만
SCAN 0 MATCH 'session:*' COUNT 100   # ✅ 운영에서는 이렇게 순회 조회
TYPE session:2:<uuid>     # → hash / string / set 등 타입 확인
```

### 2.3 타입별 값 읽기

```bash
# String
GET refresh:<토큰값>            # → "2" (userId)
TTL  refresh:<토큰값>           # → 남은 수명(초), -1=무기한, -2=없음(만료됨)

# Hash (객체 통째로)
HGETALL session:2:<uuid>        # → ip, device, loginAt, lastActiveAt 전부
HGET  session:2:<uuid> loginAt  # → 특정 필드만

# Set
SMEMBERS user:sessions:2        # → 활성 세션 UUID 목록
SCARD  user:sessions:2          # → 세션 몇 개냐 (동시 로그인 수)

# List
LRANGE login-log:ksa@naver.com 0 -1   # → 최신순 로그인 이력 전체
LLEN   login-log:ksa@naver.com        # → 로그 개수
```

### 2.4 실시간 감시 (개발할 때 강력함)

```bash
MONITOR    # ← 실행해두면 지금 들어오는 모든 Redis 명령이 실시간 출력됨
           # 로그인 버튼 누르면 SET/HSET/SADD가 찍히는 걸 눈으로 확인 가능
```

### 2.5 지우기

```bash
DEL session:2:<uuid>              # 키 하나
DEL user:sessions:2               # 세션 목록 통째로 (= 전체 로그아웃)
redis-cli --scan --pattern "session:2:*" | xargs redis-cli DEL   # 패턴 일괄
FLUSHALL                          # ⚠️ 전체 초기화 — 절대 운영에서 금지
```

### 2.6 Mongo/MySQL 사용자용 심리 매핑

| 익숙한 것 | Redis에서는 |
|-----------|------------|
| `SHOW TABLES` | `SCAN 0 MATCH * COUNT 100` |
| `SELECT * FROM t WHERE id=?` | `GET/HGETALL <key>` — **조회가 아니라 키 직접 지목** |
| row | key-value 한 쌍 |
| 컬럼 추가(ALTER) | 불필요 — Hash 필드는 그냥 넣으면 됨 |
| JOIN | 없음 — 애플리케이션에서 조합 |
| DELETE 스케줄러 | TTL이 자동 처리 |

핵심: **RDB는 "질의(query)"하고 Redis는 "지목(point)"한다.**
키 이름 설계(`session:{userId}:{uuid}`)가 곧 스키마 설계다.

---

## 3. 주의사항

- **휘발성**: 기본 설정으론 재시작 시 데이터 소실 위험. 영구 보존 필요하면 `/etc/redis/redis.conf`의 RDB/AOF 설정 확인 (감사 로그 등 증적 데이터는 Redis 말고 DB에)
- **KEYS \* 금지**: 단일 스레드라 수백만 키 조회 중 서비스 전체가 멈춤 → `SCAN` 사용
- **메모리 = 용량**: 디스크 DB처럼 무한하지 않음. maxmemory + eviction policy 설정 학습 필요
- **비밀번호 필수**: `requirepass` 미설치 상태로 외부 오픈하면 채굴 표적이 됨 (우리는 설정 완료)
- **범용 조회 불가**: "이 userId 가진 모든 데이터" 같은 질의는 키 설계 단계에서 역색인을 준비해야 함

---

## 4. 실전 적용 — sh-platform에서의 역할

### 4.1 우리 서비스의 키 지도

```
session:{userId}:{sessionId}     Hash   세션 상세(ip/device/loginAt)   TTL 180분
user:sessions:{userId}           Set    활성 세션 UUID 목록             TTL 180분
refresh:{token}                  String refresh token → userId          TTL 14일
jwt:blacklist:{token}            String 로그아웃된 accessToken         남은 수명만큼
login-log:{email}                List   로그인 성공/실패 이력 JSON      TTL 보존기간
rate-limit:{ip}                  String 로그인 시도 카운터              TTL 60초
```

→ 동시 로그인 제한, 리프레시 토큰, JWT 블랙리스트, 로그인 감사, Rate Limit —
전부 "빨라야 하고 만료되는 상태"라서 Redis에 두는 것.

### 4.2 관련 코드

- `modules/auth/backend/src/main/java/com/shplatform/shared/config/RedisRepository.java` — 자료구조별 CRUD 래퍼
- `modules/auth/backend/src/main/java/com/shplatform/auth/domain/SessionService.java` — 세션 생성/절단 정책
- `modules/auth/backend/src/main/java/com/shplatform/auth/domain/LoginLogService.java` — 로그인 이력(List+TTL)
- `modules/auth/backend/src/main/java/com/shplatform/shared/config/RateLimiter.java` — INCR+TTL 카운터

### 4.3 장애 대응 시 확인 순서 (실제 경험 기반)

1. `systemctl status redis-server` — 프로세스 살아있나
2. `redis-cli PING` — 응답하나 (PONG)
3. `SMEMBERS user:sessions:{id}` — 세션이 진짜 있는가 (없는데 로그인 막히면 잔존 세션 의심)
4. `journalctl -u sh-platform-auth | grep SESSION` — 앱 로그와 대조

---

## 5. 참고 자료

- [Redis 공식 문서](https://redis.io/docs/latest/)
- [Try Redis — 브라우저 튜토리얼](https://try.redis.io/)
- 내부 문서: `docs/guides/009-260825-redis-setup-guide.md` (설치), `docs/guides/010-260825-redis-library-reference.md` (Java 클라이언트)
- 오류 기록: `docs/errors/009-260825-oauth2-session-limit-500-error.md` (세션 잔존 트러블슈팅)

---
*작성일: 2026-08-25*
