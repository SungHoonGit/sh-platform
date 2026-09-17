# 009-260914-MySQL 백업 가이드

## 개요
- **목적**: MariaDB 전체 DB(3종)를 매일 덤프하여 장애·실수 삭제 시 시점 복구(PITR) 기반을 마련한다.
- **배경**: binlog는 2026-09-17 10:10경 첫 활성화 — 그 이전 시점은 복구 원천이 없다(9/13 기록설은 `[coordinate OK]` false positive로 인한 오인, 9/17에 기존 binlog 파일 없음으로 확정). mysqldump 주기 백업으로 "스냅샷 + binlog 로그" 복구 체계를 구성.
- **대상 DB**: `sh_pass`(auth), `scraper_platform`, `resume_platform`
  (`portfolio_platform`는 2026-08-21 인프라 정리에서 DROP됨 — 9/14에 Grants 잔재를 보고 착각해 대상에 넣었다가 9/17 제외)
- **작성일**: 2026-09-14 (대상 수정: 2026-09-17)

## 1. 구성 요소
| 파일 | 위치 | 역할 |
|------|------|------|
| `scripts/backup-mysql.sh` | 리포지토리 | 3개 DB 덤프 + gzip + 보존기간 정리 |
| `infra/cron.d/sh-platform-mysql-backup` | 리포지토리 | 매일 03:30(KST) 실행 정의 |
| `/home/ubuntu/backups/mysql/` | 서버 | 백업 산출물(일자 디렉터리 + backup.log) |

백업 스케줄은 CI(`.github/workflows/deploy-backend.yml`)가 배포할 때
`/etc/cron.d/sh-platform-mysql-backup` 로 복사·권한 설정되어 자동 설치된다(멱등).

## 2. 실행 주기·보존
- 매일 **03:30 Asia/Seoul**
- 덤프 파일: `YYYYMMDD/{db}_YYYYmmdd_HHMMSS.sql.gz`
- 실행 로그: `backup-YYYYMMDD.log` (일자별 로그)
- 보존: **7일**(`KEEP_DAYS` 환경변수로 변경 가능) — 덤프·로그 모두 만료 시 자동 삭제

## 3. 동작 확인
```bash
# 스케줄 설치 확인
cat /etc/cron.d/sh-platform-mysql-backup
# 누적 백업 목록
ls -lR /home/ubuntu/backups/mysql
# 오늘 실행 로그
tail -f /home/ubuntu/backups/mysql/backup-$(date +%Y%m%d).log
# 즉시 수동 실행 (동일 권한/환경으로 테스트)
sudo /home/ubuntu/sh-platform/scripts/backup-mysql.sh
```

## 4. 복구 절차
스냅샷 복원 = "백업 시점으로 되돌리기", PITR = "백업 시점 + binlog 재생으로 원하는 시각까지 복구"

```bash
# 1) 덤프 복원 (백업 시점의 스냅샷)
gzip -dc backup/20260914/resume_platform_20260914_030012.sql.gz | \
  mysql -h 10.0.0.39 -u sh_user -p'SHpass1234!' resume_platform

# 2) 시작 좌표 확인 (백업 시점의 binlog 좌표)
cat backup/20260914/binlog-status.txt
#   binlog_file: mysql-bin.000123
#   binlog_pos: 456789

# 3) binlog 재생 — (백업 시점 + 임의 시각)까지 변경분 적용 (PITR)
#    mysqlbinlog 파일 순서대로, 좌표 ~ 그 뒤 파일을 모두 로그하게 함
mysqlbinlog \
  --start-position=456789 \
  --stop-datetime="2026-09-14 12:00:00" \
  /var/lib/mysql/mysql-bin.000123 \
  /var/lib/mysql/mysql-bin.000124 \
  | mysql -h 10.0.0.39 -u sh_user -p'SHpass1234!' resume_platform
```

> **PITR 성공 조건**: binlog 파일이 스냅샷 좌표부터 **아직 서버에 남아 있어야** 함(아래 6 참고).
> binlog 좌표 기준 파일이 이미 지워졌으면 그 이전 스냅샷으로 재시도해야 함.
> 2026-09-17 현재 binlog는 당일 10:10경 첫 활성화 → **그 이전 시점 복구는 불가** (주기 덤프가 최소 수단).

## 5. binlog 보존 설정 (PITR 기간 확보 — 서버 적용 필요)
스냅샷+binlog PITR이 성립하려면 **binlog 보존 기간 ≥ 스냅샷 보존 기간(7일)** 여야 한다.
덤프 보존(7일) + 이후 변경분을 재생할 여유(7일)를 고려해 **14일** 권장.

```sql
-- 현재 값 확인 (MariaDB 10.6+)
SHOW VARIABLES LIKE 'binlog_expire_logs_seconds';
SHOW VARIABLES LIKE 'log_bin';   -- ON 이어야 기록됨 (기본값 OFF, 명시 필요)
-- 즉시 적용 (서버에서 실행, 재시작 전까지만 유효)
SET GLOBAL binlog_expire_logs_seconds = 1209600;  -- 14일
```
영속화: DB 서버의 `99-binlog.cnf`에 아래 3줄을 두고 재시작하면 유지된다.
```bash
# DB 서버(161.33.138.23)에서 실행 — 웹 서버가 아님에 주의
sudo tee /etc/mysql/mariadb.conf.d/99-binlog.cnf > /dev/null <<'EOF'
[mariadb]
log_bin = mysql-bin
server_id = 1
binlog_expire_logs_seconds = 1209600
EOF
sudo systemctl restart mariadb   # 전체 앱 약 1분 DB 단절, 트래픽 적은 시간에
sudo mysql -e "SHOW VARIABLES LIKE 'log_bin'; SHOW MASTER STATUS;"  # ON + 실좌표 확인
```
> **⚠️ 2026-09-17 확정 상태**: `log_bin`이 설정 파일에 없어 **binlog가 한 번도 켜진 적이 없었음**
> (기존 binlog 파일 없음으로 확인. `[coordinate OK]`는 빈 출력에도 exit 0이라 false positive였음 — 워크플로우 진단 수정済).
> 위 파일로 9/17 10:10 첫 활성화 + 재시작 후에도 `1209600` 유지 확인. PITR 체인은 이 시점부터 시작.
> **일괄 적용 스크립트**: `scripts/db-admin-setup.sql`(BINLOG MONITOR + 보존 14일 + 확인 쿼리) — 관리자로 1회 실행.
> CI 자동 적용: GitHub secret `MYSQL_ADMIN_PASS` 설정 시 다음 배포에서 자동 적용됨(멱등).

## 6. 문제 해결
| 문제 | 원인 | 해결책 |
|------|------|--------|
| 덤프 실패("Access denied") | DB_PASS 미설정/오변경 | `scripts/backup-mysql.sh`의 DB_PASS 설정 확인(.env 우선) |
| "column statistics" 경고 | MariaDB mysqldump 10.5+ 알려진 무해 경고 | 무시 (스크립트가 실제 오류와 구분) |
| 백업이 안 보임 | 보존기간 초과 삭제 또는 cron 미기동 | `/home/ubuntu/backups/mysql/backup-$(date +%Y%m%d).log` 확인, `systemctl status cron` |
| PITR 불가(지정 시각 이전 binlog 없음) | binlog 보존기간 < 복구 대상 시점 | 최근 덤프 중 스냅샷이 좌표 이전인 것 사용, binlog 보존 확대(§5) |

## 7. 웹/WAS 파일 데이터 백업
DB 외 웹 서버에만 존재하는 런타임 파일(코드/설정은 git이 원본이라 제외):
| 경로 | 내용 | 상태 |
|------|------|------|
| `/home/ubuntu/data/` | 스크래퍼 크롤링 MD 등 (`docs/scraper/data-paths.md`가 "전체 백업 권장" 명시) | 백업 대상 |
| `/home/ubuntu/sh-platform/uploads/` | 이력서/포트폴리오 첨부 파일 | 백업 대상 |

- 스크립트: `scripts/backup-files.sh` — 대상 2개 경로를 tar.gz 일자별로 압축, 7일 보존
- 스케줄: cron.d 두 번째 라인 `35 3 * * *` (DB 백업 03:30과 5분 간격)
- 산출물: `/home/ubuntu/backups/files/YYYYMMDD/{data,uploads}_YYYYmmdd_HHMMSS.tar.gz`
- 로그: `/home/ubuntu/backups/files/backup-YYYYMMDD.log`
- 복원: `tar xzf data_*.tar.gz -C /home/ubuntu/data/` 형태로 원위치 해제

## 8. binlog 모니터링 권한 (선택)
운영 계정으로 `SHOW BINARY LOGS` 확인이 필요한 경우:
```sql
GRANT BINLOG MONITOR ON *.* TO 'sh_user'@'10.0.0.%';
FLUSH PRIVILEGES;
```
(9/14 적용済. `'%'`가 아니라 `'10.0.0.%'`로 등록됨 — 처음 `'%'`로 GRANT 시 "Can't find any matching row" 발생했기 때문)

## 9. 정기 실행 확인 체크리스트 (백업 다음 날 1회)
매일 03:30(DB)·03:35(파일) KST 크론 실행 후 확인할 항목:

```bash
# 1) DB 덤프 3종 + binlog 좌표 + 로그 — 오늘 일자 디렉터리
ls -l /home/ubuntu/backups/mysql/$(date +%Y%m%d)/
cat /home/ubuntu/backups/mysql/$(date +%Y%m%d)/binlog-status.txt   # [coordinate OK] 실좌표
tail -n 20 /home/ubuntu/backups/mysql/backup-$(date +%Y%m%d).log

# 2) 파일 백업 2종 (data, uploads tar.gz)
ls -l /home/ubuntu/backups/files/$(date +%Y%m%d)/
tail -n 20 /home/ubuntu/backups/files/backup-$(date +%Y%m%d).log

# 3) cron 스케줄 설치 상태
cat /etc/cron.d/sh-platform-mysql-backup   # 03:30 DB / 03:35 files 2라인
```

| 항목 | 기대값 | 이상 시 |
|------|--------|---------|
| DB 덤프 3종 `.sql.gz` | `sh_pass`·`scraper_platform`·`resume_platform` 존재 | 로그 확인 → 수동 실행 `sudo .../scripts/backup-mysql.sh` |
| `binlog-status.txt` | `binlog_file`/`binlog_pos` 실값 | 백업 로그에서 coordinate 경고 확인 |
| 파일 백업 2종 `.tar.gz` | `data_*`·`uploads_*` 존재 | 로그 확인 → 수동 실행 `sudo .../scripts/backup-files.sh` |
| 보존 정리 | 7일 초과 산출물 자동 삭제 | `KEEP_DAYS` 확인 |

## 10. 설정 근거 (왜 이렇게 했는가)
나중에 "왜 03:30이지?", "왜 7일이지?" 같은 질문이 나오면 여기를 본다.

| 설정 | 값 | 이유 |
|------|-----|------|
| 실행 시각 03:30/03:35 KST | 새벽 최소 트래픽 시간대 | 크롤·사용자 쿼리와 겹치지 않게. DB(03:30) → 파일(03:35) 5분 간격으로 mysqldump I/O와 tar I/O가 겹치지 않게 분산 |
| 덤프 보존 7일 | `KEEP_DAYS=7` | A1.Flex 디스크 한정 + 주 1회 확인 주기. 7일치면 "언제부터 잘못됐는지" 모를 때도 복구起点 확보 가능 |
| binlog 보존 14일 | `1209600` | 덤프 보존(7일) + 재생 여유(7일). 가장 오래된 덤프(7일 전) 기준으로도 그 이후 변경분 7일치가 있어야 PITR 성립 |
| 대상 DB 3종 | sh_pass·scraper·resume | `portfolio_platform`은 8/21 인프라 정리에서 DROP됨. 9/14 Grants 잔재 오인으로 잠시 4종이었다가 9/17 제외 |
| `--single-transaction` | 무잠금 일관 스냅샷 | InnoDB 한정. `LOCK TABLES` 없이 트랜잭션 시작 시점으로 일관된 덤프 → 서비스 무중단 백업 |
| `--routines --triggers --events` | 루틴·트리거·이벤트 포함 | 빼먹으면 복원 후 앱이 조용히 오동작 (프로시저·스케줄 유실) |
| `cron.d` 파일 방식 | git 버전관리 + CI 멱등 설치 | `crontab -e`는 서버 로컬이라 유실·이관 시 사라짐. cron.d는 배포마다 자동 설치되어 서버를 갈아엎어도 복원됨 |
| binlog 좌표 기록 | PITR 시작점 | 스냅샷 "시점"을 알아야 그 이후 binlog만 재생. 좌표 없으면 전체 binlog를 뒤져야 함 |
| `gzip -9` | 최대 압축 | 텍스트 덤프는 압축률이 좋아(scraper 2.2M 등) 디스크 절약. 새벽이라 CPU 여유 있음 |
| `MYSQL_PWD` 환경변수 | 비밀번호 노출 회피 | `-p'...'`는 `ps` 출력에 그대로 보임. 환경변수도 완벽하진 않지만 프로세스 목록 노출은 피함. `.env` → 기본값 순서로 주입 |
| 일자 디렉터리 | 날짜별 격리 | `find -mtime` 보존 정리와 육안 확인이 단순. 로그도 일자별(`backup-YYYYMMDD.log`)로 분리해 cron 실행 여부 추적 가능 |
| `log_bin`+`server_id` 명시 | MariaDB 기본 OFF | 명시 없이는 binlog가 절대 안 켜짐(9/17 확정). `server_id`는 복제 없어도 binlog 필수값이라 함께 지정 |
| 진단의 빈 출력 판정 | false positive 방지 | `SHOW MASTER STATUS` 빈 출력도 exit 0이라 `[coordinate OK]`가 찍혔던 사고(9/17) → `-N` + 비어있음 검사로 수정 |

## 11. 변경 이력
| 날짜 | 내용 |
|------|------|
| 2026-09-14 | 초판 (4종 대상 — portfolio 오인 포함) |
| 2026-09-15 | 99-binlog.cnf 경로·체크리스트 보강 |
| 2026-09-16 | 4개→"4개 DB" 표기 정정 (§9) |
| 2026-09-17 | portfolio 제외(3종) + binlog 최초 활성화 확정 + 배경·§4·§5·§8 정정 + §10 설정 근거·§11 이력 신설. cron 미설치(OCI minimal) + `[coordinate OK]` false positive 기록 |

---
*작성일: 2026-09-14, 갱신: 2026-09-17*