# 016-260825 Redis 운영화 로드맵 — 모니터링·결함보완·활용확장 설계

## 개요
- **목적**: Redis 도입(완료) 이후의 운영 안정화, 모니터링 체계 구축, 실전 활용 확장 방향 정의
- **범위**: auth 모듈의 Redis 사용 전반 + 인프라(prometheus/grafana)
- **작성일**: 2026-08-25
- **배경 문서**: `docs/guides/009`(설치), `docs/errors/009`(세션 잔존 사고), `docs/learnings/006`(개념정리)

## 1. 현재 상태 (2026-08-25 기준)

### 완료된 것
| 항목 | 상태 |
|------|------|
| Redis 서버 설치 (OCI, requirepass) | ✅ |
| 세션 관리 (동시 로그인 제어, TTL 180분) | ✅ |
| Refresh Token 저장소 (MariaDB 병행 마이그레이션) | ✅ |
| JWT 블랙리스트 | ✅ |
| Rate Limiter | ✅ |
| 로그인 이력(List + TTL) / 관리자 애널리틱스 API | ✅ |
| 로그아웃 시 서버 자원 정리 (프론트 3앱 + 백엔드) | ✅ |

### 발견된 결함 (2026-08-25 심야 재검증 후 성격 확정)

**유령 세션(Ghost Session)**: `user:sessions:{userId}` Set에 Hash 없는 UUID 잔존.

원인은 두 가지로 구분됨:
1. **[관찰된 건] 수동 정리 잔여**: 장애 대응 중 `--scan --pattern "session:*"` DEL은 실행,
   `DEL user:sessions:{id}` 미실행 → Hash만 삭제되고 Set 멤버 잔존. **운영 코드 결함 아님.**
2. **[코드 경로] TTL 비대칭**: Set TTL은 로그인마다 갱신되지만 개별 Hash TTL은 독립 만료.
   로그인 간격이 벌어지면 오래된 Hash가 먼저 죽어 Set에 좀비 누적.
   (평시 삭제 경로는 removeSession/removeAllSessions 모두 쌍으로 동작하므로 즉발성 없음)

→ 영향: 활성 세션 수 과대 계상(관리자 화면·한도 판정 오판). 저빌도 누적형 hardening 대상.

## 2. 설계

### Phase A — 결함 보완 (우선순위 최상)

#### A-1. 유령 세션 lazy cleanup
```
getActiveSessions()/getActiveSessionCount() 호출 시:
  for each member in Set:
    if not EXISTS session:{userId}:{member}:
      SREM user:sessions:{userId} member    # 죽은 멤버 즉시 정리
```
- 대상: `SessionService.getActiveSessionCount`, `getActiveSessions`, `createSession`의 count 로직
- Set 크기가 작아(≤max) 성능 영향 미미

#### A-2. 기기별 로그아웃 (sessionId claim)
- accessToken claims에 `sessionId` 포함 → 로그아웃 시 해당 세션만 삭제
- `JwtAuthenticationFilter`는 여전히 세션을 요청마다 검증하지 않음(현행 유지)
- 변경 파일: TokenProvider, AuthServiceImpl.createTokens, OAuth2SuccessHandler, AuthController.logout

#### A-3. 세션 정책 확정 (운영값)
```env
APP_SESSION_MAX_PER_USER=3
APP_SESSION_PREVENT_DUPLICATE=false   # 초과 시 가장 오래된 세션 자동 절단
```

### Phase B — 모니터링 체계 (Grafana)

> 역할 구분: Grafana = "Redis 시스템 건강도" 감시 / redis-cli·RedisInsight = 개별 데이터 열람

```
[redis-server] ←─ INFO ── [redis_exporter:9121/metrics] ←─ scrape ── [Prometheus] ─→ [Grafana]
```

#### B-1. redis_exporter 설치 (systemd)
- 바이너리: oliver006/redis_exporter (ARM64)
- `infra/services.yml`에 서비스 정의 추가 → `python scripts/render_config.py`
  (systemd unit + prometheus.yml 자동 생성 — SSOT 원칙 준수, 가이드 008)

#### B-2. 수집 지표 & 대시보드 패널
| 지표(metric) | 의미 | 경계 기준(안) |
|--------------|------|---------------|
| redis_memory_used_bytes | 메모리 사용량 | maxmemory의 80% |
| redis_keyspace_hits/misses | 캐시 히트율 | 히트율 < 80% 지속 |
| redis_connected_clients | 접속 수 | 급증 (=누수 의심) |
| redis_total_commands_received | QPS | 급증 알림 |
| redis_evicted_keys | 메모리 부족으로 쫓겨난 키 | > 0 이면 경고 |
| redis_rejected_connections | 거부된 접속 | > 0 즉시 알림 |
| redis_rdb_last_save_time | 마지막 백업 | 24h 초과 경고 |

#### B-3. 알림
- Prometheus alert rule: 메모리 80% / rejected_connections > 0 / exporter down
- 기존 Grafana 알림 채널 재사용

### Phase C — 활용 확장 (서비스 가치 연동)

| 기능 | Redis 패턴 | 적용 후보 |
|------|-----------|----------|
| 공고 인기 목록 캐시 | cache-aside (String+TTL) | scraper `/job-postings/recent` 조회 결과 |
| 스크래퍼 스케줄 중복 실행 방지 | 분산 락 (SET NX PX) | ScheduleService 실행 직전 lock 획득 |
| 조회수/스크랩 랭킹 | ZSET | 공고별 스크랩 수 실시간 집계 → Viewer "인기" 배지 |
| 크롤링 완료 알림 | Pub/Sub 또는 List 큐 | 프론트 SSE와 연계 검토 |

### Phase D — 안정성 강화

1. **영속성**: `/etc/redis/redis.conf` — `appendonly yes` (AOF, everysec)
   → 재시작해도 세션/토큰 유지 (현재는 재시작 시 소실)
2. **메모리 한도**: `maxmemory 256mb` + `allkeys-lru`
3. **백업**: RDB 스냅샷 주기 점검 + 덤프 파일 백업 크론
4. (원격) OCI Cache(Managed Redis) 전환 검토 — 노드 수 증가 시

## 3. 구현 계획

| 단계 | 내용 | 예상 공수 | 우선순위 |
|------|------|-----------|----------|
| A-1 | 유령 세션 lazy cleanup (+테스트) | 1h | 🔴 즉시 |
| A-3 | .env 세션 정책 확정·재검증 | 10m | 🔴 즉시 |
| B-1~3 | redis_exporter + Grafana 대시보드 + 알림 | 2~3h | 🟡 다음 세션 |
| A-2 | sessionId claim 기기별 로그아웃 | 2h | 🟡 |
| D-1 | AOF 영속성 설정 | 30m | 🟡 |
| C-* | 캐시/락/랭킹 (각각 별도 설계 후) | 기능별 | 🟢 수요 발생 시 |

## 4. 참고 자료
- redis_exporter: https://github.com/oliver006/redis_exporter
- Grafana Redis 대시보드 템플릿: https://grafana.com/grafana/dashboards/11835
- 내부: `docs/guides/008-260821-infra-ssot-guide.md` (services.yml SSOT)

---
*작성일: 2026-08-25*
