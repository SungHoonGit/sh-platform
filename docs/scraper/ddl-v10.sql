-- ddl-v10.sql
-- Company notes for 031 (company management page)
-- 1) company_notes: per-account stars/bookmark/MD analysis (one row per account+company)

USE scraper_platform;

CREATE TABLE IF NOT EXISTS company_notes (
  id                      BIGINT AUTO_INCREMENT PRIMARY KEY,
  account_id              BIGINT         NOT NULL,
  company_name_normalized VARCHAR(200)   NOT NULL,
  company_name_display    VARCHAR(200)   NOT NULL,
  my_stars                TINYINT        NULL,
  is_bookmarked           TINYINT(1)     NOT NULL DEFAULT 0,
  note_md                 MEDIUMTEXT     NULL,
  created_at              DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at              DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_company_notes_account_company (account_id, company_name_normalized),
  KEY idx_company_notes_account (account_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
