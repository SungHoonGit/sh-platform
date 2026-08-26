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

Grafana 연동(redis_exporter :9121 → Prometheus → 대시보드 11835)은
`docs/plans/016-260825-redis-monitoring-roadmap-design.md` §B 완료 상태.

## 7.5 영속성·메모리 운영 설정 (2026-08-26 적용 완료)

### AOF (Append Only File) — 재시작해도 데이터 유지

```bash
# 런타임 즉시 적용 (재시작 불필요)
redis-cli -a 'sh_redis_2026!' CONFIG SET appendonly yes

# 설정파일 영구 반영 (재부팅 대비)
sudo sed -i 's/^appendonly no/appendonly yes/' /etc/redis/redis.conf

# 검증
redis-cli -a 'sh_redis_2026!' INFO persistence | grep -E "aof_enabled|aof_last_write_status"
sudo ls -lh /var/lib/redis/appendonlydir/
```

정상 상태:
```
aof_enabled:1
aof_last_write_status:ok
appendonlydir/ : appendonly.aof.1.base.rdb + appendonly.aof.1.incr.aof + manifest
```

### 메모리 상한·축출 정책

```bash
redis-cli -a 'sh_redis_2026!' CONFIG SET maxmemory 256mb
redis-cli -a 'sh_redis_2026!' CONFIG SET maxmemory-policy volatile-lru
```

| 항목 | 값 | 이유 |
|------|-----|------|
| maxmemory | 256mb | 서버 여유 메모리 고정 상한 — Grafana 알림(80%) 기준선 |
| policy | volatile-lru | 초과 시 **TTL 있는 키**부터 오래된 것 축출. 우리 키는 전부 TTL 보유 |

> 주의: `CONFIG SET`만 하면 재시작 시 원복된다. 영구 반영은 `/etc/redis/redis.conf`의
> `maxmemory` / `maxmemory-policy` 주석 해제 후 값을 수정할 것.

### 데이터 소실 리스크 참고

- AOF로 세션·토큰은 재시작 후에도 유지되지만, Redis를 "절대 안 날아가는 DB"로 쓰지 않는다.
- 감사·증적 데이터(login_logs/admin_audit_logs)는 MariaDB에 이중 기록됨 (플랜 016 §C-audit).

## 8. 참고 자료

- [Redis 공식 문서](https://redis.io/docs/)
- [Redis 명령어 목록](https://redis.io/docs/reference/commands/)
- [Spring Data Redis](https://spring.io/projects/spring-data-redis)

---
*작성일: 2026-08-25*
