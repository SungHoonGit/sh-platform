-- ============================================================
-- resume DDL v9 (2026-09-08: 이력서 문서 목록 순번 관리)
-- DB: resume_platform (배포 시 .github/workflows에서 자동 실행)
--
-- resume_documents.display_order: 목록 표시 순서 (1부터)
-- 실행:
--   mysql -h 10.0.0.39 -u sh_user -p resume_platform < docs/resume/ddl-resume-v9.sql
-- ============================================================

-- 멱등: 컬럼이 없을 때만 추가 (workflow가 매 배포마다 실행하므로)
SET @col_exists := (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'resume_documents'
      AND column_name = 'display_order'
);
SET @ddl := IF(@col_exists = 0,
    'ALTER TABLE resume_documents ADD COLUMN display_order INT NOT NULL DEFAULT 0',
    'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 기존 문서를 생성순(모든 이력서가 공유하는 정렬 기준)으로 순번 부여 (매 실행 동일 결과 = 멱등)
UPDATE resume_documents d
JOIN (
    SELECT id,
           ROW_NUMBER() OVER (PARTITION BY user_id ORDER BY created_at, id) AS rn
    FROM resume_documents
) r ON r.id = d.id
SET d.display_order = r.rn;