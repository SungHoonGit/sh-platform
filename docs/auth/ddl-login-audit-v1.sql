-- ============================================================================
-- 감사 로그 테이블 v1 (2026-08-25)
-- 적용 DB: sh_pass (auth)
-- 원칙: Redis = 실시간 판정(hot, TTL 소멸) / MariaDB = 증적·리포트(cold, 영속)
-- 참조: docs/plans/016-260825-redis-monitoring-roadmap-design.md §C-audit
-- ============================================================================

-- 로그인 이력 (성공/실패 전부)
CREATE TABLE IF NOT EXISTS login_logs (
  id BIGINT AUTO_INCREMENT,
  user_id BIGINT NULL,
  email VARCHAR(255) NOT NULL,
  ip VARCHAR(45) NULL,
  user_agent VARCHAR(512) NULL,
  success TINYINT(1) NOT NULL DEFAULT 0,
  created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  PRIMARY KEY (id),
  KEY idx_login_logs_user_created (user_id, created_at),
  KEY idx_login_logs_email_created (email, created_at),
  KEY idx_login_logs_created (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 관리자 행위 감사 (권한 변경 / 강제 로그아웃 / 사용자 삭제)
CREATE TABLE IF NOT EXISTS admin_audit_logs (
  id BIGINT AUTO_INCREMENT,
  actor_user_id BIGINT NOT NULL,
  action VARCHAR(50) NOT NULL,
  target_user_id BIGINT NULL,
  before_value VARCHAR(500) NULL,
  after_value VARCHAR(500) NULL,
  ip VARCHAR(45) NULL,
  created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  PRIMARY KEY (id),
  KEY idx_admin_audit_actor (actor_user_id, created_at),
  KEY idx_admin_audit_target (target_user_id, created_at),
  KEY idx_admin_audit_action (action, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
