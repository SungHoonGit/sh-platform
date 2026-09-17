-- ddl-v12.sql
-- Company note categories for 031 feedback (tags on bookmarked/memo companies)
-- Reuses block_reasons master (no new master table) via company_note_reason join.
-- Re-runnable (IF NOT EXISTS).

USE scraper_platform;

CREATE TABLE IF NOT EXISTS company_note_reason (
    note_id         BIGINT NOT NULL,
    block_reason_id BIGINT NOT NULL,
    PRIMARY KEY (note_id, block_reason_id),
    KEY idx_company_note_reason_reason (block_reason_id),
    CONSTRAINT fk_cnr_note FOREIGN KEY (note_id) REFERENCES company_notes (id) ON DELETE CASCADE,
    CONSTRAINT fk_cnr_reason FOREIGN KEY (block_reason_id) REFERENCES block_reasons (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
