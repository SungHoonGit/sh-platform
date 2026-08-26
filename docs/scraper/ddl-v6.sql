-- ============================================================================
-- company_blacklist v6 (2026-08-26): 개인 단위 회사 블랙리스트
-- ============================================================================
CREATE TABLE IF NOT EXISTS company_blacklist (
  id BIGINT AUTO_INCREMENT,
  account_id BIGINT NOT NULL,
  company_name_normalized VARCHAR(200) NOT NULL,
  reason VARCHAR(200) NULL,
  created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  PRIMARY KEY (id),
  UNIQUE KEY uk_blacklist_account_company (account_id, company_name_normalized),
  KEY idx_blacklist_account (account_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
