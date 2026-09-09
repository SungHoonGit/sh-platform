-- ============================================================
-- resume DDL v10 (2026-09-09: 포트폴리오 작업물 고도화)
-- DB: resume_platform (배포 시 .github/workflows에서 자동 실행)
--
-- resume_portfolio_items 로 포트폴리오 작업물 필드 확장:
--   thumbnail_path: 썸네일 이미지 저장 경로 (/api/v1/files/{id}/download)
--   github_url    : GitHub 저장소 링크
--   demo_url      : 데모/배포 링크
--   video_url     : 시연 영상 링크
-- 실행:
--   mysql -h 10.0.0.39 -u sh_user -p resume_platform < docs/resume/ddl-resume-v10.sql
-- ============================================================

-- 멱등: 컬럼이 없을 때만 추가 (workflow가 매 배포마다 실행하므로)
SET @cnt := (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'resume_portfolio_items'
      AND column_name = 'thumbnail_path'
);
SET @ddl := IF(@cnt = 0,
    'ALTER TABLE resume_portfolio_items
        ADD COLUMN thumbnail_path VARCHAR(300) NULL,
        ADD COLUMN github_url VARCHAR(300) NULL,
        ADD COLUMN demo_url VARCHAR(300) NULL,
        ADD COLUMN video_url VARCHAR(300) NULL',
    'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;