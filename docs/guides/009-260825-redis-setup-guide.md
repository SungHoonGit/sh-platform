# 009-260825-redis-setup-guide

## 개요
- **목적**: sh-platform 서버에 Redis 설치 및 설정 방법 안내
- **대상**: sh-platform 운영/개발 인력
- **작성일**: 2026-08-25

## 1. Redis란?

in-memory data store. 캐시, 세션, Rate Limiter, JWT 블랙리스트 등에 사용.

## 2. 설치 가이드

### 2.1 사전 조건
- Ubuntu 서버 (OCI A1.Flex ARM64)
- sudo 권한

### 2.2 설치 명령어

```bash
# 1. 패키지 업데이트 및 Redis 설치
sudo apt update && sudo apt install -y redis-server

# 2. 부팅 시 자동 시작 설정
sudo systemctl enable redis-server

# 3. Redis 시작
sudo systemctl start redis-server

# 4. 상태 확인
sudo systemctl status redis-server
```

**정상 동작 확인:**
```
● redis-server.service - Advanced key-value store
   Active: active (running)
   Status: "Ready to accept connections"
```

### 2.3 비밀번호 설정

```bash
# 설정 파일 열기 (nano 또는 vim)
sudo nano /etc/redis/redis.conf
# 또는
sudo vim /etc/redis/redis.conf
```

아래 라인을 찾아서 수정:

```
# 변경 전:
# requirepass foobared

# 변경 후:
requirepass sh_redis_2026!
```

### 2.4 재시작 및 테스트

```bash
# Redis 재시작
sudo systemctl restart redis-server

# 연결 테스트 (PONG 응답 오면 성공)
redis-cli -a sh_redis_2026! ping
```

### 2.5 .env에 Redis 비밀번호 추가

```bash
sudo nano /home/ubuntu/sh-platform/.env
```

아래 라인 추가:

```
REDIS_PASSWORD=sh_redis_2026!
```

### 2.6 auth 서비스 재시작

```bash
sudo systemctl restart sh-platform-auth
```

## 3. 설정 확인

### 3.1 Redis 정보 확인

```bash
# Redis 정보
redis-cli -a sh_redis_2026! INFO server | head -5

# 메모리 사용량
redis-cli -a sh_redis_2026! INFO memory | grep used_memory_human

# 현재 키 개수
redis-cli -a sh_redis_2026! DBSIZE
```

### 3.2 서비스 로그 확인

```bash
# auth 서비스 로그에서 Redis 연결 확인
sudo journalctl -u sh-platform-auth --since "5 min ago" | grep -i redis
```

## 4. Redis 명령어 치트시트

```bash
# 전체 키 목록
redis-cli -a sh_redis_2026! KEYS "*"

# 특정 키 삭제
redis-cli -a sh_redis_2026! DEL "key_name"

# TTL 확인
redis-cli -a sh_redis_2026! TTL "key_name"

# 모든 데이터 삭제 (주의!)
redis-cli -a sh_redis_2026! FLUSHALL
```

## 5. 문제 해결

| 문제 | 원인 | 해결 |
|------|------|------|
| `Could not connect` | Redis 미시작 | `sudo systemctl start redis-server` |
| `ERR invalid password` | 비밀번호 불일치 | `.env`의 `REDIS_PASSWORD` 확인 |
| `OOM command not allowed` | 메모리 부족 | `maxmemory` 설정 확인 |
| auth 서비스 연결 실패 | 포트/비밀번호 불일치 | `application-prod.yml` 설정 확인 |

## 6. 보안 설정

- **127.0.0.1만 바인딩**: 외부에서 접근 불가
- **비밀번호 설정**: `requirepass`
- **포트 변경**: 기본 6379 (변경 불필요, localhost만 사용)

## 7. 모니터링

```bash
# Redis 상태 대시보드
redis-cli -a sh_redis_2026! INFO

# 실시간 명령 모니터링
redis-cli -a sh_redis_2026! MONITOR

# 종료: Ctrl+C
```

## 8. 참고 자료

- [Redis 공식 문서](https://redis.io/docs/)
- [Redis 명령어 목록](https://redis.io/docs/reference/commands/)
- [Spring Data Redis](https://spring.io/projects/spring-data-redis)

---
*작성일: 2026-08-25*
