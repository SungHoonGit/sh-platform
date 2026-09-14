#!/usr/bin/env bash
#
# MySQL 주기 전체 백업 (mysqldump)
# - DB 3종 전체 덤프: sh_pass / scraper_platform / resume_platform
# - 매일 cron.d 로 실행 (infra/cron.d/sh-platform-mysql-backup, Asia/Seoul 03:30)
# - 보존: KEEP_DAYS(기본 7)일 이전 디렉터리 자동 삭제
# - 설치/복구 절차: docs/guides/009-260914-mysql-backup-guide.md
#
set -euo pipefail

BACKUP_ROOT="${BACKUP_ROOT:-/home/ubuntu/backups/mysql}"
KEEP_DAYS="${KEEP_DAYS:-7}"
DB_HOST="${DB_HOST:-10.0.0.39}"
DB_USER="${DB_USER:-sh_user}"
# 1) 환경변수 → 2) .env → 3) 기본값(CI 배포와 동일한 운영 계정)
DB_PASS="${DB_PASS:-${DB_USER_PASSWORD:-${MYSQL_PASSWORD:-$(
  [ -f /home/ubuntu/sh-platform/.env ] && sed -n -E \
    's/^[[:space:]]*(DB_PASSWORD|MYSQL_PASSWORD|DB_USER_PASSWORD)[[:space:]]*=[[:space:]]*"?([^"[:space:]]+)"?.*/\2/p' \
    /home/ubuntu/sh-platform/.env | head -1 || true
)}}}"
DB_PASS="${DB_PASS:-SHpass1234!}"

DATABASES=(sh_pass scraper_platform resume_platform)
STAMP="$(date +%Y%m%d_%H%M%S)"
OUT_DIR="$BACKUP_ROOT/$(date +%Y%m%d)"
mkdir -p "$OUT_DIR"

if [ -z "$DB_PASS" ]; then
  echo "[FAIL] DB_PASS 를 찾을 수 없습니다 (.env 확인)." >&2
  exit 1
fi
export MYSQL_PWD="$DB_PASS"

# binlog 좌표 기록 — PITR: 스냅샷 이후 binlog 재생 시작점
MASTER_STATUS="$(mysql --host="$DB_HOST" --user="$DB_USER" --batch --skip-column-names -e "SHOW MASTER STATUS" 2>/dev/null || true)"
{
  echo "# binlog snapshot 좌표 ($(date -Iseconds))"
  if [ -n "$MASTER_STATUS" ]; then
    echo "binlog_file: $(printf '%s\n' "$MASTER_STATUS" | awk '{print $1}')"
    echo "binlog_pos: $(printf '%s\n' "$MASTER_STATUS" | awk '{print $2}')"
  else
    echo "binlog_file: (disabled — binlog 미활성)"
    echo "binlog_pos: -"
  fi
} > "$OUT_DIR/binlog-status.txt"
echo "[OK] binlog 좌표 -> ${OUT_DIR}/binlog-status.txt"

for db in "${DATABASES[@]}"; do
  warn="$OUT_DIR/${db}_${STAMP}.warn"
  sql="$OUT_DIR/${db}_${STAMP}.sql"
  if ! mysqldump --host="$DB_HOST" --user="$DB_USER" \
      --single-transaction --routines --triggers --events \
      --skip-lock-tables "$db" > "$sql" 2> "$warn"; then
    echo "[FAIL] $db 덤프 실패:" >&2
    cat "$warn" >&2
    exit 1
  fi
  # MariaDB "column statistics" 경고는 무해 → 그 외 에러만 실패 처리
  if grep -qv "column statistics" "$warn"; then
    echo "[FAIL] $db:" >&2
    grep -v "column statistics" "$warn" >&2
    exit 1
  fi
  rm -f "$warn"
  gzip -9 -f "$sql"
  echo "[OK] $db -> ${sql}.gz ($(du -h "${sql}.gz" | cut -f1))"
done

unset MYSQL_PWD

# 보존 기간 초과 백업 삭제 (일자 디렉터리 단위)
find "$BACKUP_ROOT" -mindepth 1 -maxdepth 1 -type d -mtime +"$KEEP_DAYS" -exec rm -rf {} +
echo "[DONE] 백업 완료. 누적 크기: $(du -sh "$BACKUP_ROOT" | cut -f1)"