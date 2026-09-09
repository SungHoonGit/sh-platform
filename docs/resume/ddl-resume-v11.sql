-- ============================================================
-- resume DDL v11 (2026-09-09: 경력 기간 상세 + 작업물↔프로젝트 공통 표준화)
-- DB: resume_platform (배포 시 .github/workflows에서 자동 실행)
--
-- 1. resume_career_items   : 경력(회사) 안의 기간별 상세 문서화 항목
--    title        : 업무/프로젝트명
--    start_date   : 시작일
--    end_date     : 종료일 (진행 중이면 NULL)
--    description  : 상세 내용
-- 2. resume_projects  : 프로젝트 표준 필드 확장
--    github_url, demo_url, video_url, thumbnail_path
-- 3. resume_portfolio_items : 작업물 표준 필드 확장 (프로젝트와 동일 세트)
--    role, start_date, end_date, tech_stack
-- 실행:
--   mysql -h 10.0.0.39 -u sh_user -p resume_platform < docs/resume/ddl-resume-v11.sql
-- ============================================================

-- 1) resume_career_items 테이블 신규 (멱등)
CREATE TABLE IF NOT EXISTS resume_career_items (
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    career_id     BIGINT       NOT NULL COMMENT 'resume_careers.id',
    title         VARCHAR(100) NULL,
    start_date    DATE         NULL,
    end_date      DATE         NULL,
    description   TEXT         NULL,
    display_order INT          NOT NULL DEFAULT 0,
    created_at    DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at    DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    KEY idx_resume_career_items_career_id_display (career_id, display_order),
    CONSTRAINT fk_resume_career_items_career
        FOREIGN KEY (career_id) REFERENCES resume_careers (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='경력 내 기간별 상세 항목';

-- 2) resume_projects 표준 필드 추가 (멱등)
SET @cnt := (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'resume_projects'
      AND column_name = 'github_url'
);
SET @ddl := IF(@cnt = 0,
    'ALTER TABLE resume_projects
        ADD COLUMN github_url VARCHAR(300) NULL,
        ADD COLUMN demo_url VARCHAR(300) NULL,
        ADD COLUMN video_url VARCHAR(300) NULL,
        ADD COLUMN thumbnail_path VARCHAR(300) NULL',
    'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 3) resume_portfolio_items 표준 필드 추가 (멱등)
SET @cnt2 := (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'resume_portfolio_items'
      AND column_name = 'role'
);
SET @ddl2 := IF(@cnt2 = 0,
    'ALTER TABLE resume_portfolio_items
        ADD COLUMN role VARCHAR(100) NULL,
        ADD COLUMN start_date DATE NULL,
        ADD COLUMN end_date DATE NULL,
        ADD COLUMN tech_stack VARCHAR(300) NULL',
    'SELECT 1');
PREPARE stmt2 FROM @ddl2;
EXECUTE stmt2;
DEALLOCATE PREPARE stmt2;