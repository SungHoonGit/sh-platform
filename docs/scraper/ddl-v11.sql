-- ddl-v11.sql
-- Fix 031 Phase 1 deploy failure (2026-09-16):
-- ddl-auto:validate rejects my_stars TINYINT against entity Integer.
-- Region precedent (Boolean <-> TINYINT(1)) is fine; only my_stars needs INT.
-- Re-runnable (MODIFY COLUMN is idempotent).

USE scraper_platform;

ALTER TABLE company_notes MODIFY COLUMN my_stars INT NULL;
