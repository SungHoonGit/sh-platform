-- ============================================================
-- sh-platform DB 관리자 1회 실행 스크립트 (백업/PITR 지원)
-- 대상: MariaDB 10.11 (10.0.0.39) — root 또는 GRANT 권한 보유 계정
-- 실행: mysql -h 10.0.0.39 -u root -p < scripts/db-admin-setup.sql
-- (또는 DB 호스트 내 로컬 소켓: sudo mysql < scripts/db-admin-setup.sql)
-- 멱등: 언제 다시 실행해도 안전
-- ============================================================

-- 1) sh_user 에 binlog 좌표 조회 권한 (백업 스크립트의 binlog-status.txt 기록용)
GRANT BINLOG MONITOR ON *.* TO 'sh_user'@'%';

-- 2) binlog 보존 14일 (덤프 7일 보존 + 재생 여유) — 영속화는 my.cnf 필요(아래 주석)
SET GLOBAL binlog_expire_logs_seconds = 1209600;
-- 영속화: /etc/mysql/mariadb.conf.d/50-server.cnf 의 [mysqld] 섹션에
--   binlog_expire_logs_seconds = 1209600
-- 추가 후: sudo systemctl restart mariadb

FLUSH PRIVILEGES;

-- 결과 확인
SHOW GRANTS FOR 'sh_user'@'%';
SHOW VARIABLES LIKE 'binlog_expire_logs_seconds';
SHOW BINARY LOGS;