-- 로컬 개발용 DB 초기화 스크립트
-- docker compose up 첫 실행 시 자동 실행 (이미지 볼륨 최초 생성 시에만)
-- 주의: 볼륨(mariadb-data)이 이미 존재하면 재실행되지 않음

CREATE DATABASE IF NOT EXISTS scraper_platform
  CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS resume_platform
  CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- 로컬 사용자(sh_local)에 전체 권한
GRANT ALL PRIVILEGES ON *.* TO 'sh_local'@'%';
FLUSH PRIVILEGES;