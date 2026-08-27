-- ============================================================
-- resume 모듈 DDL v5 (2026-08-27)
-- DB: resume_platform  — 학력 학점(GPA) 컬럼 추가
--
-- 실행:
--   mysql -h 10.0.0.39 -u sh_user -p resume_platform < docs/resume/ddl-resume-v5.sql
--
-- MariaDB는 ALTER ... ADD COLUMN IF NOT EXISTS 미지원 → 정보 스키마로 멱등 처리
-- ============================================================

SET @col_exists = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME   = 'resume_educations'
      AND COLUMN_NAME  = 'gpa'
);

SET @ddl = IF(@col_exists = 0,
    'ALTER TABLE resume_educations ADD COLUMN gpa VARCHAR(20) NULL AFTER degree',
    'SELECT 1');

PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
