# 009-260914-MySQL 백업 가이드

## 개요
- **목적**: MariaDB 전체 DB(3종)를 매일 덤프하여 장애·실수 삭제 시 시점 복구(PITR) 기반을 마련한다.
- **배경**: binlog는 2026-09-13 19:28부터만 기록되어 그 이전 시점은 복구 원천이 없다. mysqldump 주기 백업으로 "스냅샷 + binlog 로그" 복구 체계를 구성.
- **대상 DB**: `sh_pass`(auth), `scraper_platform`, `resume_platform`
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
```bash
# DB 전체 복원 (예: resume_platform)
gzip -dc backup/resume_platform_20260914_030012.sql.gz | \
  mysql -h 10.0.0.39 -u sh_user -p'SHpass1234!' resume_platform
# 특정 테이블만 복원
gzip -dc backup/resume_platform_*.sql.gz | \
  mysql -h 10.0.0.39 -u sh_user -p'SHpass1234!' --database resume_platform \
  --execute="SOURCE /dev/stdin" 2>/dev/null || true
# (권장) 복원 전 현재 DB도 백업해 두고, 앱 재시작 순서 확인
sudo systemctl restart sh-platform-auth sh-platform-scraper sh-platform-resume
```

> 복원 시 주의: 백업 후 발생한 변경분은 binlog로 복구해야 함(스냅샷 시점 이후 손실).
> "스냅샷 + binlog" 복구 구성(마스터 binlog 좌표 기록)은 미구현 — 필요 시 추가.

## 5. binlog 모니터링 권한 (선택)
운영 계정으로 `SHOW BINARY LOGS` 확인이 필요한 경우:
```sql
GRANT BINLOG MONITOR ON *.* TO 'sh_user'@'%';
FLUSH PRIVILEGES;
```

## 6. 문제 해결
| 문제 | 원인 | 해결책 |
|------|------|--------|
| 덤프 실패("Access denied") | DB_PASS 미설정/오변경 | `scripts/backup-mysql.sh`의 DB_PASS 설정 확인(.env 우선) |
| "column statistics" 경고 | MariaDB mysqldump 10.5+ 알려진 무해 경고 | 무시 (스크립트가 실제 오류와 구분) |
| 백업이 안 보임 | 보존기간 초과 삭제 또는 cron 미기동 | `/home/ubuntu/backups/mysql/backup.log` 확인, `systemctl status cron` |

---
*작성일: 2026-09-14*