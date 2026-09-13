-- ============================================================
-- resume DDL v13 (2026-09-13: 아이템 → 문서 소속)
-- DB: resume_platform (배포 시 .github/workflows에서 자동 실행)
--
-- 아이템 테이블 전반에 document_id 컬럼 추가:
--   resume_careers, resume_educations, resume_skills,
--   resume_certificates, resume_projects, resume_introductions,
--   resume_portfolio_items, resume_career_items
-- 실행:
--   mysql -h 10.0.0.39 -u sh_user -p resume_platform < docs/resume/ddl-resume-v13.sql
-- ============================================================

-- 멱등: 각 테이블의 document_id 컬럼이 없을 때만 추가.
-- PREPARE는 한 번에 한 문장만 실행하므로 테이블별로 개별 ALTER를 수행한다.
DROP PROCEDURE IF EXISTS add_document_id_columns;
DELIMITER //
CREATE PROCEDURE add_document_id_columns()
BEGIN
    DECLARE done INT DEFAULT 0;
    DECLARE tbl VARCHAR(64);
    DECLARE cur CURSOR FOR
        SELECT t.table_name
        FROM information_schema.tables t
        WHERE t.table_schema = DATABASE()
          AND t.table_name IN ('resume_careers','resume_educations','resume_skills',
                               'resume_certificates','resume_projects','resume_introductions',
                               'resume_portfolio_items','resume_career_items')
          AND NOT EXISTS (
              SELECT 1 FROM information_schema.columns c
              WHERE c.table_schema = t.table_schema
                AND c.table_name = t.table_name
                AND c.column_name = 'document_id'
          )
        ORDER BY t.table_name;
    DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = 1;
    OPEN cur;
    read_loop: LOOP
        FETCH cur INTO tbl;
        IF done = 1 THEN LEAVE read_loop; END IF;
        SET @s := CONCAT('ALTER TABLE `', tbl, '` ADD COLUMN document_id BIGINT NULL AFTER user_id');
        PREPARE stmt FROM @s;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END LOOP;
    CLOSE cur;
END //
DELIMITER ;
CALL add_document_id_columns();
DROP PROCEDURE IF EXISTS add_document_id_columns;

-- 인덱스 추가 (멱등)
SET @idx_cnt := (
    SELECT COUNT(*)
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'resume_careers'
      AND index_name = 'idx_resume_careers_user_doc'
);
SET @idx_ddl := IF(@idx_cnt = 0,
    'ALTER TABLE resume_careers ADD INDEX idx_resume_careers_user_doc (user_id, document_id)',
    'SELECT 1');
PREPARE stmt2 FROM @idx_ddl;
EXECUTE stmt2;
DEALLOCATE PREPARE stmt2;

SET @idx_cnt2 := (
    SELECT COUNT(*)
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'resume_educations'
      AND index_name = 'idx_resume_educations_user_doc'
);
SET @idx_ddl2 := IF(@idx_cnt2 = 0,
    'ALTER TABLE resume_educations ADD INDEX idx_resume_educations_user_doc (user_id, document_id)',
    'SELECT 1');
PREPARE stmt3 FROM @idx_ddl2;
EXECUTE stmt3;
DEALLOCATE PREPARE stmt3;

SET @idx_cnt3 := (
    SELECT COUNT(*)
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'resume_skills'
      AND index_name = 'idx_resume_skills_user_doc'
);
SET @idx_ddl3 := IF(@idx_cnt3 = 0,
    'ALTER TABLE resume_skills ADD INDEX idx_resume_skills_user_doc (user_id, document_id)',
    'SELECT 1');
PREPARE stmt4 FROM @idx_ddl3;
EXECUTE stmt4;
DEALLOCATE PREPARE stmt4;

SET @idx_cnt4 := (
    SELECT COUNT(*)
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'resume_certificates'
      AND index_name = 'idx_resume_certificates_user_doc'
);
SET @idx_ddl4 := IF(@idx_cnt4 = 0,
    'ALTER TABLE resume_certificates ADD INDEX idx_resume_certificates_user_doc (user_id, document_id)',
    'SELECT 1');
PREPARE stmt5 FROM @idx_ddl4;
EXECUTE stmt5;
DEALLOCATE PREPARE stmt5;

SET @idx_cnt5 := (
    SELECT COUNT(*)
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'resume_projects'
      AND index_name = 'idx_resume_projects_user_doc'
);
SET @idx_ddl5 := IF(@idx_cnt5 = 0,
    'ALTER TABLE resume_projects ADD INDEX idx_resume_projects_user_doc (user_id, document_id)',
    'SELECT 1');
PREPARE stmt6 FROM @idx_ddl5;
EXECUTE stmt6;
DEALLOCATE PREPARE stmt6;

SET @idx_cnt6 := (
    SELECT COUNT(*)
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'resume_introductions'
      AND index_name = 'idx_resume_introductions_user_doc'
);
SET @idx_ddl6 := IF(@idx_cnt6 = 0,
    'ALTER TABLE resume_introductions ADD INDEX idx_resume_introductions_user_doc (user_id, document_id)',
    'SELECT 1');
PREPARE stmt7 FROM @idx_ddl6;
EXECUTE stmt7;
DEALLOCATE PREPARE stmt7;

SET @idx_cnt7 := (
    SELECT COUNT(*)
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'resume_portfolio_items'
      AND index_name = 'idx_resume_portfolio_items_user_doc'
);
SET @idx_ddl7 := IF(@idx_cnt7 = 0,
    'ALTER TABLE resume_portfolio_items ADD INDEX idx_resume_portfolio_items_user_doc (user_id, document_id)',
    'SELECT 1');
PREPARE stmt8 FROM @idx_ddl7;
EXECUTE stmt8;
DEALLOCATE PREPARE stmt8;

SET @idx_cnt8 := (
    SELECT COUNT(*)
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'resume_career_items'
      AND index_name = 'idx_resume_career_items_user_doc'
);
SET @idx_ddl8 := IF(@idx_cnt8 = 0,
    'ALTER TABLE resume_career_items ADD INDEX idx_resume_career_items_user_doc (user_id, document_id)',
    'SELECT 1');
PREPARE stmt9 FROM @idx_ddl8;
EXECUTE stmt9;
DEALLOCATE PREPARE stmt9;

-- ------------------------------------------------------------
-- 기존 데이터 백필: document_id가 NULL인 아이템을
-- (1) 대표 문서(is_primary=1) 또는 (2) display_order가 가장 작은 문서에 소속시킨다.
-- ------------------------------------------------------------
DROP PROCEDURE IF EXISTS backfill_document_id;
DELIMITER //
CREATE PROCEDURE backfill_document_id()
BEGIN
    DECLARE done INT DEFAULT 0;
    DECLARE uid BIGINT;
    DECLARE doc_id BIGINT;
    DECLARE cur CURSOR FOR
        SELECT u.user_id, d.id
        FROM (
            SELECT user_id FROM resume_careers WHERE document_id IS NULL
            UNION SELECT user_id FROM resume_educations WHERE document_id IS NULL
            UNION SELECT user_id FROM resume_skills WHERE document_id IS NULL
            UNION SELECT user_id FROM resume_certificates WHERE document_id IS NULL
            UNION SELECT user_id FROM resume_projects WHERE document_id IS NULL
            UNION SELECT user_id FROM resume_introductions WHERE document_id IS NULL
            UNION SELECT user_id FROM resume_portfolio_items WHERE document_id IS NULL
        ) u
        JOIN (
            SELECT user_id, id,
                   ROW_NUMBER() OVER (PARTITION BY user_id ORDER BY is_primary DESC, display_order ASC, id ASC) rn
            FROM resume_documents
        ) d ON d.user_id = u.user_id AND d.rn = 1
        ORDER BY u.user_id;
    DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = 1;
    OPEN cur;
    read_loop: LOOP
        FETCH cur INTO uid, doc_id;
        IF done = 1 THEN LEAVE read_loop; END IF;
        UPDATE resume_careers SET document_id = doc_id WHERE user_id = uid AND document_id IS NULL;
        UPDATE resume_educations SET document_id = doc_id WHERE user_id = uid AND document_id IS NULL;
        UPDATE resume_skills SET document_id = doc_id WHERE user_id = uid AND document_id IS NULL;
        UPDATE resume_certificates SET document_id = doc_id WHERE user_id = uid AND document_id IS NULL;
        UPDATE resume_projects SET document_id = doc_id WHERE user_id = uid AND document_id IS NULL;
        UPDATE resume_introductions SET document_id = doc_id WHERE user_id = uid AND document_id IS NULL;
        UPDATE resume_portfolio_items SET document_id = doc_id WHERE user_id = uid AND document_id IS NULL;
    END LOOP;
    CLOSE cur;

    -- 경력 상세(기간별 업무)는 상위 경력 문서를 따라간다
    UPDATE resume_career_items ci
    JOIN resume_careers c ON ci.career_id = c.id
    SET ci.document_id = c.document_id
    WHERE ci.document_id IS NULL AND c.document_id IS NOT NULL;
END //
DELIMITER ;
CALL backfill_document_id();
DROP PROCEDURE IF EXISTS backfill_document_id;
