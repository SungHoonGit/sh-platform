-- ddl-v9.sql
-- 사이트·지역 마스터 DB화 (설계 028)
-- 1) site_definition: UI 메타(display_order, icon, color) 컬럼 추가
-- 2) region 테이블 신설 (17개 시도) + 시드
-- 3) site/region 시드 멱등 UPDATE(idempotent)

USE scraper_platform;

-- 1) site_definition 확장 (멱등 ALTER, MariaDB 10.2.7+)
ALTER TABLE site_definition
  ADD COLUMN IF NOT EXISTS display_order INT NOT NULL DEFAULT 0,
  ADD COLUMN IF NOT EXISTS icon         VARCHAR(20)  NULL,
  ADD COLUMN IF NOT EXISTS color        VARCHAR(50)  NULL;

-- 2) region 신설 (멱등 CREATE)
CREATE TABLE IF NOT EXISTS region (
  id            BIGINT AUTO_INCREMENT PRIMARY KEY,
  name          VARCHAR(20)  NOT NULL UNIQUE,
  display_order INT          NOT NULL DEFAULT 0,
  is_active     TINYINT(1)   NOT NULL DEFAULT 1,
  created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 3) site_definition 시드 메타 (멱등: 존재하면 UPDATE만)
-- color는 Tailwind 색상 토큰(blue/green/...) — 실제 클래스는 프론트 고정 팔레트로 변환
INSERT INTO site_definition (site_name, display_name, base_url, is_enabled, display_order, icon, color)
VALUES
  ('saramin', '사람인', 'https://www.saramin.co.kr', 1, 1, '💼', 'blue'),
  ('jobkorea', '잡코리아', 'https://www.jobkorea.co.kr', 1, 2, '🔍', 'green'),
  ('wanted', '원티드', 'https://www.wanted.co.kr', 0, 3, '🚀', 'red'),
  ('jumpit', '점핏', 'https://www.jumpit.co.kr', 0, 4, '📈', 'orange'),
  ('incruit', '인크루트', 'https://www.incruit.com', 0, 5, '📋', 'purple'),
  ('remember', '리멤버', 'https://www.rememberapp.co.kr', 0, 6, '💡', 'yellow')
ON DUPLICATE KEY UPDATE
  display_order = VALUES(display_order),
  icon          = VALUES(icon),
  color         = VALUES(color);

-- 4) region 시드 17개 (멱등: 없는 시도만 INSERT, 표시 순서는 UPDATE)
INSERT INTO region (name, display_order, is_active)
SELECT tmp.name, tmp.display_order, 1 FROM (
  SELECT '서울' AS name, 1 AS display_order UNION ALL
  SELECT '경기', 2 UNION ALL
  SELECT '인천', 3 UNION ALL
  SELECT '부산', 4 UNION ALL
  SELECT '대구', 5 UNION ALL
  SELECT '대전', 6 UNION ALL
  SELECT '광주', 7 UNION ALL
  SELECT '울산', 8 UNION ALL
  SELECT '세종', 9 UNION ALL
  SELECT '강원', 10 UNION ALL
  SELECT '충북', 11 UNION ALL
  SELECT '충남', 12 UNION ALL
  SELECT '전북', 13 UNION ALL
  SELECT '전남', 14 UNION ALL
  SELECT '경북', 15 UNION ALL
  SELECT '경남', 16 UNION ALL
  SELECT '제주', 17
) tmp
WHERE NOT EXISTS (SELECT 1 FROM region r WHERE r.name = tmp.name);

UPDATE region r
JOIN (
  SELECT '서울' AS name, 1 AS display_order UNION ALL
  SELECT '경기', 2 UNION ALL
  SELECT '인천', 3 UNION ALL
  SELECT '부산', 4 UNION ALL
  SELECT '대구', 5 UNION ALL
  SELECT '대전', 6 UNION ALL
  SELECT '광주', 7 UNION ALL
  SELECT '울산', 8 UNION ALL
  SELECT '세종', 9 UNION ALL
  SELECT '강원', 10 UNION ALL
  SELECT '충북', 11 UNION ALL
  SELECT '충남', 12 UNION ALL
  SELECT '전북', 13 UNION ALL
  SELECT '전남', 14 UNION ALL
  SELECT '경북', 15 UNION ALL
  SELECT '경남', 16 UNION ALL
  SELECT '제주', 17
) tmp ON r.name = tmp.name
SET r.display_order = tmp.display_order;