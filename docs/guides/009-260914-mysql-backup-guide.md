# 009-260914-MySQL 백업 가이드

## 개요
- **목적**: MariaDB 전체 DB(4종)를 매일 덤프하여 장애·실수 삭제 시 시점 복구(PITR) 기반을 마련한다.
- **배경**: binlog는 2026-09-13 19:28부터만 기록되어 그 이전 시점은 복구 원천이 없다. mysqldump 주기 백업으로 "스냅샷 + binlog 로그" 복구 체계를 구성.
- **대상 DB**: `sh_pass`(auth), `scraper_platform`, `resume_platform`, `portfolio_platform`
- **작성일**: 2026-09-14

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
- 파일: `YYYYMMDD/{db}_YYYYmmdd_HHMMSS.sql.gz`
- 보존: **7일**(`KEEP_DAYS` 환경변수로 변경 가능) — 이전 일자 디렉터리 자동 삭제

## 3. 동작 확인
```bash
# 스케줄 설치 확인
cat /etc/cron.d/sh-platform-mysql-backup
# 누적 백업 목록
ls -lR /home/ubuntu/backups/mysql
# 실행 로그
tail -f /home/ubuntu/backups/mysql/backup.log
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
> 2026-09-14 현재 binlog는 9/13 19:28부터 기록 → **그 이전 시점 복구는 불가** (주기 덤프가 최소 수단).

## 5. binlog 보존 설정 (PITR 기간 확보 — 서버 적용 필요)
스냅샷+binlog PITR이 성립하려면 **binlog 보존 기간 ≥ 스냅샷 보존 기간(7일)** 여야 한다.
덤프 보존(7일) + 이후 변경분을 재생할 여유(7일)를 고려해 **14일 이상** 권장.

```sql
-- 현재 값 확인 (MariaDB 10.6+)
SHOW VARIABLES LIKE 'binlog_expire_logs_seconds';
-- 즉시 적용 (서버에서 실행)
SET GLOBAL binlog_expire_logs_seconds = 1209600;  -- 14일
```
영속화: `mysqld.cnf`에 `[mysqld] binlog_expire_logs_seconds=1209600` 추가 후 재시작.
> **일괄 적용 스크립트**: `scripts/db-admin-setup.sql`(BINLOG MONITOR + 보존 14일 + 확인 쿼리) — 관리자로 1회 실행.
> CI 자동 적용: GitHub secret `MYSQL_ADMIN_PASS` 설정 시 다음 배포에서 자동 적용됨(멱등).

## 6. 문제 해결
| 문제 | 원인 | 해결책 |
|------|------|--------|
| 덤프 실패("Access denied") | DB_PASS 미설정/오변경 | `scripts/backup-mysql.sh`의 DB_PASS 설정 확인(.env 우선) |
| "column statistics" 경고 | MariaDB mysqldump 10.5+ 알려진 무해 경고 | 무시 (스크립트가 실제 오류와 구분) |
| 백업이 안 보임 | 보존기간 초과 삭제 또는 cron 미기동 | `/home/ubuntu/backups/mysql/backup.log` 확인, `systemctl status cron` |
| PITR 불가(지정 시각 이전 binlog 없음) | binlog 보존기간 < 복구 대상 시점 | 최근 덤프 중 스냅샷이 좌표 이전인 것 사용, binlog 보존 확대(§5) |

## 7. binlog 모니터링 권한 (선택)
운영 계정으로 `SHOW BINARY LOGS` 확인이 필요한 경우:
```sql
GRANT BINLOG MONITOR ON *.* TO 'sh_user'@'%';
FLUSH PRIVILEGES;
```

---
*작성일: 2026-09-14*