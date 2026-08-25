# Redis 라이브러리 레퍼런스 (sh-platform)

## 개요
- **목적**: sh-platform에 도입된 Redis 관련 라이브러리 및 API 레퍼런스
- **적용 모듈**: auth-backend
- **작성일**: 2026-08-25

---

## 1. 사용된 라이브러리

### 1.1 spring-boot-starter-data-redis

```kotlin
// build.gradle.kts
implementation("org.springframework.boot:spring-boot-starter-data-redis")
```

**제공 기능:**
- `RedisTemplate` - 범용 Redis 클라이언트
- `StringRedisTemplate` - 문자열 전용 Redis 클라이언트
- `LettuceConnectionFactory` - Redis 연결 (기본 클라이언트: Lettuce)
- `@EnableCaching` - 캐시 어노테이션 (미사용)

### 1.2 Lettuce (기본 Redis 클라이언트)

Spring Boot 기본 Redis 클라이언트. 네트워크 비동기 지원, 커넥션 풀 내장.

---

## 2. 핵심 클래스

### 2.1 RedisConfig

```java
@Configuration
public class RedisConfig {

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(new StringRedisSerializer());
        template.afterPropertiesSet();
        return template;
    }

    @Bean
    public StringRedisTemplate stringRedisTemplate(RedisConnectionFactory connectionFactory) {
        return new StringRedisTemplate(connectionFactory);
    }
}
```

**역할:** Redis 직렬화 설정 (StringRedisSerializer 사용)

### 2.2 RedisRepository (커스텀 유틸리티)

Redis CRUD를 감싸는 에러 핸들링 포함 유틸리티.

| 메서드 | 설명 | Redis 명령어 |
|--------|------|-------------|
| `set(key, value, timeout, unit)` | 값 저장 + TTL | `SET key value EX timeout` |
| `get(key)` | 값 조회 | `GET key` |
| `delete(key)` | 키 삭제 | `DEL key` |
| `hasKey(key)` | 키 존재 확인 | `EXISTS key` |
| `increment(key)` | 정수 증가 | `INCR key` |
| `expire(key, timeout, unit)` | TTL 설정 | `EXPIRE key timeout` |
| `getExpire(key)` | TTL 조회 | `TTL key` |
| `hashPut(key, hashKey, value)` | 해시 필드 저장 | `HSET key hashKey value` |
| `hashGet(key, hashKey)` | 해시 필드 조회 | `HGET key hashKey` |
| `hashDelete(key, hashKeys...)` | 해시 필드 삭제 | `HDEL key hashKeys` |
| `addToSet(key, values...)}` | 세트 멤버 추가 | `SADD key values` |
| `removeFromSet(key, values...)` | 세트 멤버 제거 | `SREM key values` |
| `getSetMembers(key)` | 세트 멤버 조회 | `SMEMBERS key` |
| `getSetSize(key)` | 세트 크기 | `SCARD key` |
| `leftPush(key, value)` | 리스트 왼쪽 추가 | `LPUSH key value` |
| `getList(key, start, end)` | 리스트 범위 조회 | `LRANGE key start end` |

**graceful degradation:** 모든 메서드에서 Redis 예외 발생 시 로그만 남기고 null/false 반환

---

## 3. Redis 키 구조

### 3.1 JWT 블랙리스트

```
Key: blacklist:{token_hash_sha256}
Value: "1"
TTL: Access Token 남은 유효시간 (초)
```

```java
// TokenBlacklistService.java
String hash = SHA256(token) → Base64URL
String key = "blacklist:" + hash
redisRepository.set(key, "1", remainingSeconds, TimeUnit.SECONDS)
```

### 3.2 사용자 세션

```
Key: session:{userId}:{sessionId}
Value: Hash
  ├── device: "Chrome/Windows"
  ├── ip: "192.168.1.1"
  ├── loginAt: "2026-08-25T10:00:00"
  └── lastActiveAt: "2026-08-25T11:30:00"
TTL: 180분 (sessionTimeoutMinutes * 60)
```

### 3.3 세션 목록

```
Key: user:sessions:{userId}
Value: Set { sessionId1, sessionId2, ... }
TTL: 180분
```

### 3.4 Rate Limiter

```
Key: ratelimit:{ip}:{uri}
Value: 정수 (요청 횟수)
TTL: 60초
```

```java
// RateLimiter.java
String key = "ratelimit:" + ip + ":" + uri
Long count = redisRepository.increment(key)  // 첫 요청 시 1
if (count == 1) {
    redisRepository.expire(key, 60, TimeUnit.SECONDS)  // 윈도우 시작
}
return count <= maxAttempts  // 5(로그인), 3(인증), 30(일반)
```

### 3.5 Refresh Token

```
Key: refresh:{token}
Value: userId (문자열)
TTL: 14일 (refreshTokenExpirationMs / 1000)
```

### 3.6 로그인 이력

```
Key: login:log:{yyyy-MM-dd}
Value: List (JSON 문자열)
TTL: 90일

JSON 구조:
{
  "userId": 6,
  "email": "user@example.com",
  "ip": "192.168.1.1",
  "device": "Mozilla/5.0...",
  "success": true,
  "timestamp": "2026-08-25T10:00:00"
}
```

---

## 4. 설정 파일

### 4.1 application-prod.yml

```yaml
spring:
  data:
    redis:
      host: 127.0.0.1
      port: 6379
      password: ${REDIS_PASSWORD:}
      timeout: 3000
      lettuce:
        pool:
          max-active: 8
          max-idle: 8
          min-idle: 2
          max-wait: 3000
```

### 4.2 .env

```
REDIS_PASSWORD=sh_redis_2026!
```

### 4.3 세션 설정 (선택)

```
APP_SESSION_MAX_PER_USER=3
APP_SESSION_TIMEOUT_MINUTES=180
APP_SESSION_PREVENT_DUPLICATE=false
```

---

## 5. Graceful Degradation

Redis 장애 시 자동 폴백:

| 기능 | Redis 장애 시 동작 |
|------|-------------------|
| Rate Limiter | ConcurrentHashMap 폴백 |
| JWT 블랙리스트 | 검증 건너뜀 (서명만으로 검증) |
| 세션 추적 | 비활성화 (로그인은 허용) |
| Refresh Token | MariaDB 폴백 (기존 테이블) |
| 로그인 이력 | 기록 안 됨 (서비스는 정상) |

---

## 6. 모니터링 명령어

```bash
# Redis 상태
redis-cli -a sh_redis_2026! INFO server

# 메모리 사용량
redis-cli -a sh_redis_2026! INFO memory | grep used_memory_human

# 현재 키 목록
redis-cli -a sh_redis_2026! KEYS "*"

# 특정 키 타입 확인
redis-cli -a sh_redis_2026! TYPE "user:sessions:2"

# TTL 확인
redis-cli -a sh_redis_2026! TTL "blacklist:xxxx"

# 세트 멤버 확인
redis-cli -a sh_redis_2026! SMEMBERS "user:sessions:2"

# 해시 필드 확인
redis-cli -a sh_redis_2026! HGETALL "session:2:uuid-string"

# 실시간 명령 모니터링
redis-cli -a sh_redis_2026! MONITOR
```

---

## 7. 문제 해결

| 문제 | 원인 | 해결 |
|------|------|------|
| `ERR invalid password` | 비밀번호 불일치 | `.env`의 `REDIS_PASSWORD` 확인 |
| `Connection refused` | Redis 미시작 | `sudo systemctl start redis-server` |
| `OOM command not allowed` | 메모리 부족 | `maxmemory` 설정 확인 |
| KEYS 빈 배열 | 로그인 미완료 | OAuth2/일반 로그인 후 재확인 |
| 세션 미생성 | OAuth2 흐름 미연결 | `OAuth2SuccessHandler` 수정 필요 |

---

## 8. 커맨드 치트시트

```bash
# 연결 테스트
redis-cli -a sh_redis_2026! PING

# 키 전체 삭제 (주의!)
redis-cli -a sh_redis_2026! FLUSHALL

# 특정 패턴 키 삭제
redis-cli -a sh_redis_2026! KEYS "ratelimit:*" | xargs -L 1 redis-cli -a sh_redis_2026! DEL

# Redis 정보
redis-cli -a sh_redis_2026! INFO
```

---
*작성일: 2026-08-25*
