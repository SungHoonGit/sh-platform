-- ============================================================================
-- 앱 메뉴 관리 테이블 v1 (2026-10-07)
-- 대상 DB: sh_pass (auth)
-- 용도: 4앱 메뉴 단일 소스 DB화 (설계 039 P4) — GET /api/v1/menus?app={app}
-- 규칙: INSERT IGNORE — 시드는 누락분만 보충(이후 관리자 수정값 덮어쓰지 않음)
-- ============================================================================

CREATE TABLE IF NOT EXISTS menus (
  id BIGINT AUTO_INCREMENT,
  app VARCHAR(32) NOT NULL,
  item_id VARCHAR(64) NOT NULL,
  label VARCHAR(100) NOT NULL,
  href VARCHAR(255) NULL,
  icon VARCHAR(64) NULL,
  is_external TINYINT(1) NOT NULL DEFAULT 0,
  is_primary TINYINT(1) NOT NULL DEFAULT 0,
  section VARCHAR(64) NULL,
  item_order INT NOT NULL DEFAULT 0,
  roles VARCHAR(32) NULL,
  visible TINYINT(1) NOT NULL DEFAULT 1,
  PRIMARY KEY (id),
  UNIQUE KEY uk_menus_app_item (app, item_id),
  KEY idx_menus_app_order (app, item_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ---------------------------------------------------------------------------
-- 시드: 앱별 메뉴 (menus.ts와 동일 데이터)
-- ---------------------------------------------------------------------------

-- scraper (4건)
INSERT IGNORE INTO menus (app, item_id, label, href, icon, is_external, is_primary, section, item_order, roles, visible) VALUES
('scraper', 'scraper.search',    '통합검색',    '/',          'Search',      0, 1, '검색', 10, NULL, 1),
('scraper', 'scraper.schedule',  '스케줄 등록', '/schedule',  'CalendarPlus',0, 1, '수집', 20, NULL, 1),
('scraper', 'scraper.viewer',    '공고 뷰어',   '/viewer',    'FileText',    0, 1, '수집', 30, NULL, 1),
('scraper', 'scraper.companies', '회사 관리',   '/companies', 'Building2',   0, 0, '데이터', 40, NULL, 1);

-- platform (18건)
INSERT IGNORE INTO menus (app, item_id, label, href, icon, is_external, is_primary, section, item_order, roles, visible) VALUES
('platform', 'platform.dashboard',            '개요',           '/platform',                      NULL, 0, 1, NULL,            10, NULL,    1),
('platform', 'platform.me.resumes',           '내 이력서',      '/resume/',                       NULL, 0, 0, '개인 서비스',    20, NULL,    1),
('platform', 'platform.me.postings',          '공고 탐색',      '/resume/#/postings',             NULL, 0, 0, '개인 서비스',    30, NULL,    1),
('platform', 'platform.me.applications',      '지원 관리',      '/resume/#/applications',         NULL, 0, 0, '개인 서비스',    40, NULL,    1),
('platform', 'platform.me.account',           '계정 설정',      '/platform/account',              NULL, 0, 0, '개인 서비스',    50, NULL,    1),
('platform', 'platform.admin',                '관리',           '/platform/admin',                NULL, 0, 1, NULL,            60, 'ADMIN',  1),
('platform', 'platform.admin.roles',          '권한 관리',      '/platform/admin/roles',          NULL, 0, 0, '관리',          70, 'ADMIN',  1),
('platform', 'platform.admin.users',          '사용자 관리',    '/platform/admin/users',          NULL, 0, 0, '관리',          80, 'ADMIN',  1),
('platform', 'platform.admin.tenants',        '테넌트 관리',    '/platform/admin/tenants',        NULL, 0, 0, '관리',          90, 'ADMIN',  1),
('platform', 'platform.admin.audit',          '감사 로그',      '/platform/admin/audit',          NULL, 0, 0, '관리',         100, 'ADMIN',  1),
('platform', 'platform.admin.sessions',       '세션 관리',      '/platform/admin/sessions',       NULL, 0, 0, '관리',         110, 'ADMIN',  1),
('platform', 'platform.admin.master',         '마스터 관리',    '/platform/admin/master',         NULL, 0, 0, '관리',         120, 'ADMIN',  1),
('platform', 'platform.tools.swagger-auth',   'Swagger · Auth', '/swagger-ui/index.html',         NULL, 1, 0, '도구',         130, 'ADMIN',  1),
('platform', 'platform.tools.swagger-scraper','Swagger · Scraper','/scraper/swagger-ui/index.html',NULL, 1, 0, '도구',         140, 'ADMIN',  1),
('platform', 'platform.tools.swagger-resume', 'Swagger · Resume','/resume/swagger-ui/index.html', NULL, 1, 0, '도구',         150, 'ADMIN',  1),
('platform', 'platform.tools.javadoc',        'Javadoc',        '/javadoc/',                      NULL, 1, 0, '도구',         160, 'ADMIN',  1),
('platform', 'platform.tools.test-reports',   '테스트 리포트',  '/test-reports/',                 NULL, 1, 0, '도구',         170, 'ADMIN',  1),
('platform', 'platform.tools.schemaspy',      'SchemaSpy',      '/schemaSpy/',                    NULL, 1, 0, '도구',         180, 'ADMIN',  1);

-- resume (5건)
INSERT IGNORE INTO menus (app, item_id, label, href, icon, is_external, is_primary, section, item_order, roles, visible) VALUES
('resume', 'resume.resumes',     '이력서 만들기',    '#/resumes',                      NULL, 0, 1, '탐색 메뉴', 10, NULL,    1),
('resume', 'resume.portfolio',   '프로젝트',        '#/portfolio',                    NULL, 0, 1, '탐색 메뉴', 20, NULL,    1),
('resume', 'resume.postings',    '채용 탐색',        '#/postings',                     NULL, 0, 1, '탐색 메뉴', 30, NULL,    1),
('resume', 'resume.applications','지원 현황',        '#/applications',                 NULL, 0, 1, '탐색 메뉴', 40, NULL,    1),
('resume', 'resume.tools.swagger','Swagger · Resume','/resume/swagger-ui/index.html',  NULL, 1, 0, '도구',     50, 'ADMIN',  1);
