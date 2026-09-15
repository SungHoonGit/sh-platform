#!/usr/bin/env bash
#
# 웹/WAS 파일 데이터 주기 백업 (tar.gz)
# - 대상: /home/ubuntu/data (스크래퍼 MD 등) + /home/ubuntu/sh-platform/uploads (이력서/포트폴리오 첨부)
# - 매일 cron.d 로 실행 (infra/cron.d/sh-platform-mysql-backup, Asia/Seoul 03:35)
# - 보존: KEEP_DAYS(기본 7)일 이전 디렉터리 자동 삭제
# - 상세: docs/guides/009-260914-mysql-backup-guide.md
#
set -euo pipefail

BACKUP_ROOT="${BACKUP_ROOT:-/home/ubuntu/backups/files}"
KEEP_DAYS="${KEEP_DAYS:-7}"
STAMP="$(date +%Y%m%d_%H%M%S)"
OUT_DIR="$BACKUP_ROOT/$(date +%Y%m%d)"
mkdir -p "$OUT_DIR"

# 대상 가드: 없으면 스킵
DATA_DIR="${DATA_DIR:-/home/ubuntu/data}"
UPLOAD_DIR="${UPLOAD_DIR:-/home/ubuntu/sh-platform/uploads}"

if [ -d "$DATA_DIR" ]; then
  tar czf "$OUT_DIR/data_${STAMP}.tar.gz" -C "$DATA_DIR" . 2>/dev/null
  echo "[OK] data -> ${OUT_DIR}/data_${STAMP}.tar.gz ($(du -h "$OUT_DIR/data_${STAMP}.tar.gz" | cut -f1))"
else
  echo "[SKIP] $DATA_DIR 없음"
fi

if [ -d "$UPLOAD_DIR" ]; then
  tar czf "$OUT_DIR/uploads_${STAMP}.tar.gz" -C "$UPLOAD_DIR" . 2>/dev/null
  echo "[OK] uploads -> ${OUT_DIR}/uploads_${STAMP}.tar.gz ($(du -h "$OUT_DIR/uploads_${STAMP}.tar.gz" | cut -f1))"
else
  echo "[SKIP] $UPLOAD_DIR 없음"
fi

# 보존 기간 초과 삭제 (일자 디렉터리 + 로그)
find "$BACKUP_ROOT" -mindepth 1 -maxdepth 1 -type d -mtime +"$KEEP_DAYS" -exec rm -rf {} +
find "$BACKUP_ROOT" -maxdepth 1 -type f -name 'backup-*.log' -mtime +"$KEEP_DAYS" -delete
echo "[DONE] 파일 백업 완료. 누적 크기: $(du -sh "$BACKUP_ROOT" | cut -f1)"