-- ============================================================
-- resume DDL v12 (2026-09-09: 기술 스택 마스터 — 회사 차단식 키워드화 + 유사검색)
-- DB: resume_platform (배포 시 .github/workflows에서 자동 실행)
--
-- 1. resume_skill_master : 기술 스택 기준/마스터 (프론트 하드코딩 금지)
--    name      : 정식 기술명 (예: React)
--    category  : 분류 (language / framework / database / infra / tool / etc)
--    aliases   : 유사어/별칭 목록 (쉼표 구분, 예: React.js,ReactJS) — 유사검색용
--    display_order : 표시/정렬 순서
--
-- 실행:
--   mysql -h 10.0.0.39 -u sh_user -p resume_platform < docs/resume/ddl-resume-v12.sql
-- ============================================================

-- 1) resume_skill_master 테이블 신규 (멱등)
CREATE TABLE IF NOT EXISTS resume_skill_master (
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    name          VARCHAR(100) NOT NULL,
    category      VARCHAR(50)  NULL COMMENT 'language/framework/database/infra/tool/etc',
    aliases       VARCHAR(500) NULL COMMENT '유사어/별칭 (쉼표 구분)',
    display_order INT          NOT NULL DEFAULT 0,
    active        TINYINT(1)   NOT NULL DEFAULT 1,
    created_at    DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    UNIQUE KEY uk_resume_skill_master_name (name),
    KEY idx_resume_skill_master_category (category, active)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='기술 스택 마스터';

-- 2) 시드 데이터 (멱등 — 있으면 건너뜀)
INSERT IGNORE INTO resume_skill_master (name, category, aliases, display_order) VALUES
('Java',        'language',   '자바,JAVA', 10),
('JavaScript',  'language',   'JS,자바스크립트,ECMAScript', 20),
('TypeScript',  'language',   'TS,타입스크립트', 30),
('Python',      'language',   '파이썬', 40),
('Kotlin',      'language',   '코틀린', 50),
('Go',          'language',   'Golang,고', 60),
('C++',         'language',   'CPP,Cpp', 70),
('Spring',      'framework',  'Spring Framework,스프링', 100),
('Spring Boot', 'framework',  '스프링부트,SpringBoot', 110),
('React',       'framework',  'React.js,ReactJS,리액트', 120),
('Vue.js',      'framework',  'Vue,VueJS,뷰', 130),
('Angular',     'framework',  'AngularJS,앵귤러', 140),
('Express',     'framework',  'Express.js,Node,Node.js,NodeJS', 150),
('Next.js',     'framework',  'Next,넥스트', 160),
('MySQL',       'database',   'MariaDB,MYSQL', 200),
('PostgreSQL',  'database',   'Postgres,PG', 210),
('MongoDB',     'database',   'Mongo,몽고DB', 220),
('Redis',       'database',   '레디스', 230),
('Docker',      'infra',      '도커', 300),
('Kubernetes',  'infra',      'K8s,쿠버네티스', 310),
('AWS',         'infra',      'Amazon Web Services,아마존웹서비스', 320),
('Nginx',       'infra',      '엔진엑스', 330),
('Git',         'tool',       '깃', 400),
('GitHub',      'tool',       '깃허브,Github', 410),
('GitLab',      'tool',       '깃랩', 420),
('Jenkins',     'tool',       '젠킨스', 430),
('Gradle',      'tool',       '그래들', 440),
('Maven',       'tool',       '메이븐', 450),
('JUnit',       'tool',       '테스트', 460),
('React Native','framework',  'RN,리액트네이티브', 1500);
