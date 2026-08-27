-- ============================================================
-- resume 모듈 DDL v6 (2026-08-28)
-- DB: resume_platform  — 마스터 데이터(학교/전공) 테이블 + 시드
--
-- 실행:
--   mysql -h 10.0.0.39 -u sh_user -p resume_platform < docs/resume/ddl-resume-v6.sql
--
-- 배경: 프론트 하드코딩(schools.ts) 제거 → DB 코드화. 기준 데이터는
--       배포 없이 DB만 수정하면 추가/변경되도록 한다 (하드코딩 지양 원칙).
-- ============================================================

-- ---------- 1. schools 테이블 ----------
CREATE TABLE IF NOT EXISTS schools (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    name        VARCHAR(100) NOT NULL,
    school_type VARCHAR(20)  NOT NULL,
    UNIQUE KEY uk_schools_name_type (name, school_type),
    KEY idx_schools_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ---------- 2. majors 테이블 ----------
CREATE TABLE IF NOT EXISTS majors (
    id   BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    UNIQUE KEY uk_majors_name (name),
    KEY idx_majors_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ---------- 3. schools 시드 (기존 하드코딩 목록 이전) ----------
INSERT INTO schools (name, school_type)
SELECT * FROM (SELECT '서울대학교', '대학교') AS t
WHERE NOT EXISTS (SELECT 1 FROM schools WHERE name='서울대학교' AND school_type='대학교');

INSERT INTO schools (name, school_type)
SELECT * FROM (SELECT '연세대학교', '대학교') AS t
WHERE NOT EXISTS (SELECT 1 FROM schools WHERE name='연세대학교' AND school_type='대학교');

INSERT INTO schools (name, school_type)
SELECT * FROM (SELECT '고려대학교', '대학교') AS t
WHERE NOT EXISTS (SELECT 1 FROM schools WHERE name='고려대학교' AND school_type='대학교');

INSERT INTO schools (name, school_type)
SELECT * FROM (SELECT '서강대학교', '대학교') AS t
WHERE NOT EXISTS (SELECT 1 FROM schools WHERE name='서강대학교' AND school_type='대학교');

INSERT INTO schools (name, school_type)
SELECT * FROM (SELECT '성균관대학교', '대학교') AS t
WHERE NOT EXISTS (SELECT 1 FROM schools WHERE name='성균관대학교' AND school_type='대학교');

INSERT INTO schools (name, school_type)
SELECT * FROM (SELECT '한양대학교', '대학교') AS t
WHERE NOT EXISTS (SELECT 1 FROM schools WHERE name='한양대학교' AND school_type='대학교');

INSERT INTO schools (name, school_type)
SELECT * FROM (SELECT '중앙대학교', '대학교') AS t
WHERE NOT EXISTS (SELECT 1 FROM schools WHERE name='중앙대학교' AND school_type='대학교');

INSERT INTO schools (name, school_type)
SELECT * FROM (SELECT '경희대학교', '대학교') AS t
WHERE NOT EXISTS (SELECT 1 FROM schools WHERE name='경희대학교' AND school_type='대학교');

INSERT INTO schools (name, school_type)
SELECT * FROM (SELECT '한국외국어대학교', '대학교') AS t
WHERE NOT EXISTS (SELECT 1 FROM schools WHERE name='한국외국어대학교' AND school_type='대학교');

INSERT INTO schools (name, school_type)
SELECT * FROM (SELECT '서울시립대학교', '대학교') AS t
WHERE NOT EXISTS (SELECT 1 FROM schools WHERE name='서울시립대학교' AND school_type='대학교');

INSERT INTO schools (name, school_type)
SELECT * FROM (SELECT '건국대학교', '대학교') AS t
WHERE NOT EXISTS (SELECT 1 FROM schools WHERE name='건국대학교' AND school_type='대학교');

INSERT INTO schools (name, school_type)
SELECT * FROM (SELECT '동국대학교', '대학교') AS t
WHERE NOT EXISTS (SELECT 1 FROM schools WHERE name='동국대학교' AND school_type='대학교');

INSERT INTO schools (name, school_type)
SELECT * FROM (SELECT '홍익대학교', '대학교') AS t
WHERE NOT EXISTS (SELECT 1 FROM schools WHERE name='홍익대학교' AND school_type='대학교');

INSERT INTO schools (name, school_type)
SELECT * FROM (SELECT '숭실대학교', '대학교') AS t
WHERE NOT EXISTS (SELECT 1 FROM schools WHERE name='숭실대학교' AND school_type='대학교');

INSERT INTO schools (name, school_type)
SELECT * FROM (SELECT '국민대학교', '대학교') AS t
WHERE NOT EXISTS (SELECT 1 FROM schools WHERE name='국민대학교' AND school_type='대학교');

INSERT INTO schools (name, school_type)
SELECT * FROM (SELECT '세종대학교', '대학교') AS t
WHERE NOT EXISTS (SELECT 1 FROM schools WHERE name='세종대학교' AND school_type='대학교');

INSERT INTO schools (name, school_type)
SELECT * FROM (SELECT '이화여자대학교', '대학교') AS t
WHERE NOT EXISTS (SELECT 1 FROM schools WHERE name='이화여자대학교' AND school_type='대학교');

INSERT INTO schools (name, school_type)
SELECT * FROM (SELECT '숙명여자대학교', '대학교') AS t
WHERE NOT EXISTS (SELECT 1 FROM schools WHERE name='숙명여자대학교' AND school_type='대학교');

INSERT INTO schools (name, school_type)
SELECT * FROM (SELECT '한국기술교육대학교', '대학교') AS t
WHERE NOT EXISTS (SELECT 1 FROM schools WHERE name='한국기술교육대학교' AND school_type='대학교');

INSERT INTO schools (name, school_type)
SELECT * FROM (SELECT '인하대학교', '대학교') AS t
WHERE NOT EXISTS (SELECT 1 FROM schools WHERE name='인하대학교' AND school_type='대학교');

INSERT INTO schools (name, school_type)
SELECT * FROM (SELECT '아주대학교', '대학교') AS t
WHERE NOT EXISTS (SELECT 1 FROM schools WHERE name='아주대학교' AND school_type='대학교');

INSERT INTO schools (name, school_type)
SELECT * FROM (SELECT '한양대학교 ERICA', '대학교') AS t
WHERE NOT EXISTS (SELECT 1 FROM schools WHERE name='한양대학교 ERICA' AND school_type='대학교');

INSERT INTO schools (name, school_type)
SELECT * FROM (SELECT '인천대학교', '대학교') AS t
WHERE NOT EXISTS (SELECT 1 FROM schools WHERE name='인천대학교' AND school_type='대학교');

INSERT INTO schools (name, school_type)
SELECT * FROM (SELECT '가천대학교', '대학교') AS t
WHERE NOT EXISTS (SELECT 1 FROM schools WHERE name='가천대학교' AND school_type='대학교');

INSERT INTO schools (name, school_type)
SELECT * FROM (SELECT '단국대학교', '대학교') AS t
WHERE NOT EXISTS (SELECT 1 FROM schools WHERE name='단국대학교' AND school_type='대학교');

INSERT INTO schools (name, school_type)
SELECT * FROM (SELECT '경기대학교', '대학교') AS t
WHERE NOT EXISTS (SELECT 1 FROM schools WHERE name='경기대학교' AND school_type='대학교');

INSERT INTO schools (name, school_type)
SELECT * FROM (SELECT '부산대학교', '대학교') AS t
WHERE NOT EXISTS (SELECT 1 FROM schools WHERE name='부산대학교' AND school_type='대학교');

INSERT INTO schools (name, school_type)
SELECT * FROM (SELECT '경북대학교', '대학교') AS t
WHERE NOT EXISTS (SELECT 1 FROM schools WHERE name='경북대학교' AND school_type='대학교');

INSERT INTO schools (name, school_type)
SELECT * FROM (SELECT '전남대학교', '대학교') AS t
WHERE NOT EXISTS (SELECT 1 FROM schools WHERE name='전남대학교' AND school_type='대학교');

INSERT INTO schools (name, school_type)
SELECT * FROM (SELECT '전북대학교', '대학교') AS t
WHERE NOT EXISTS (SELECT 1 FROM schools WHERE name='전북대학교' AND school_type='대학교');

INSERT INTO schools (name, school_type)
SELECT * FROM (SELECT '충남대학교', '대학교') AS t
WHERE NOT EXISTS (SELECT 1 FROM schools WHERE name='충남대학교' AND school_type='대학교');

INSERT INTO schools (name, school_type)
SELECT * FROM (SELECT '충북대학교', '대학교') AS t
WHERE NOT EXISTS (SELECT 1 FROM schools WHERE name='충북대학교' AND school_type='대학교');

INSERT INTO schools (name, school_type)
SELECT * FROM (SELECT '강원대학교', '대학교') AS t
WHERE NOT EXISTS (SELECT 1 FROM schools WHERE name='강원대학교' AND school_type='대학교');

INSERT INTO schools (name, school_type)
SELECT * FROM (SELECT '제주대학교', '대학교') AS t
WHERE NOT EXISTS (SELECT 1 FROM schools WHERE name='제주대학교' AND school_type='대학교');

INSERT INTO schools (name, school_type)
SELECT * FROM (SELECT '목포대학교', '대학교') AS t
WHERE NOT EXISTS (SELECT 1 FROM schools WHERE name='목포대학교' AND school_type='대학교');

INSERT INTO schools (name, school_type)
SELECT * FROM (SELECT '동아대학교', '대학교') AS t
WHERE NOT EXISTS (SELECT 1 FROM schools WHERE name='동아대학교' AND school_type='대학교');

INSERT INTO schools (name, school_type)
SELECT * FROM (SELECT '부경대학교', '대학교') AS t
WHERE NOT EXISTS (SELECT 1 FROM schools WHERE name='부경대학교' AND school_type='대학교');

INSERT INTO schools (name, school_type)
SELECT * FROM (SELECT '울산대학교', '대학교') AS t
WHERE NOT EXISTS (SELECT 1 FROM schools WHERE name='울산대학교' AND school_type='대학교');

INSERT INTO schools (name, school_type)
SELECT * FROM (SELECT '서울사이버대학교', '대학교') AS t
WHERE NOT EXISTS (SELECT 1 FROM schools WHERE name='서울사이버대학교' AND school_type='대학교');

INSERT INTO schools (name, school_type)
SELECT * FROM (SELECT '고려사이버대학교', '대학교') AS t
WHERE NOT EXISTS (SELECT 1 FROM schools WHERE name='고려사이버대학교' AND school_type='대학교');

INSERT INTO schools (name, school_type)
SELECT * FROM (SELECT '한국방송통신대학교', '대학교') AS t
WHERE NOT EXISTS (SELECT 1 FROM schools WHERE name='한국방송통신대학교' AND school_type='대학교');

INSERT INTO schools (name, school_type)
SELECT * FROM (SELECT '세종사이버대학교', '대학교') AS t
WHERE NOT EXISTS (SELECT 1 FROM schools WHERE name='세종사이버대학교' AND school_type='대학교');

INSERT INTO schools (name, school_type)
SELECT * FROM (SELECT '서울대학교 대학원', '대학원') AS t
WHERE NOT EXISTS (SELECT 1 FROM schools WHERE name='서울대학교 대학원' AND school_type='대학원');

INSERT INTO schools (name, school_type)
SELECT * FROM (SELECT '연세대학교 대학원', '대학원') AS t
WHERE NOT EXISTS (SELECT 1 FROM schools WHERE name='연세대학교 대학원' AND school_type='대학원');

INSERT INTO schools (name, school_type)
SELECT * FROM (SELECT '고려대학교 대학원', '대학원') AS t
WHERE NOT EXISTS (SELECT 1 FROM schools WHERE name='고려대학교 대학원' AND school_type='대학원');

INSERT INTO schools (name, school_type)
SELECT * FROM (SELECT '서강대학교 대학원', '대학원') AS t
WHERE NOT EXISTS (SELECT 1 FROM schools WHERE name='서강대학교 대학원' AND school_type='대학원');

INSERT INTO schools (name, school_type)
SELECT * FROM (SELECT '성균관대학교 대학원', '대학원') AS t
WHERE NOT EXISTS (SELECT 1 FROM schools WHERE name='성균관대학교 대학원' AND school_type='대학원');

INSERT INTO schools (name, school_type)
SELECT * FROM (SELECT '한양대학교 대학원', '대학원') AS t
WHERE NOT EXISTS (SELECT 1 FROM schools WHERE name='한양대학교 대학원' AND school_type='대학원');

INSERT INTO schools (name, school_type)
SELECT * FROM (SELECT 'KAIST 대학원', '대학원') AS t
WHERE NOT EXISTS (SELECT 1 FROM schools WHERE name='KAIST 대학원' AND school_type='대학원');

INSERT INTO schools (name, school_type)
SELECT * FROM (SELECT '서울과학기술대학교 대학원', '대학원') AS t
WHERE NOT EXISTS (SELECT 1 FROM schools WHERE name='서울과학기술대학교 대학원' AND school_type='대학원');

-- ---------- 4. majors 시드 ----------
INSERT INTO majors (name)
SELECT * FROM (SELECT '컴퓨터공학') AS t WHERE NOT EXISTS (SELECT 1 FROM majors WHERE name='컴퓨터공학');
INSERT INTO majors (name)
SELECT * FROM (SELECT '컴퓨터과학') AS t WHERE NOT EXISTS (SELECT 1 FROM majors WHERE name='컴퓨터과학');
INSERT INTO majors (name)
SELECT * FROM (SELECT '소프트웨어공학') AS t WHERE NOT EXISTS (SELECT 1 FROM majors WHERE name='소프트웨어공학');
INSERT INTO majors (name)
SELECT * FROM (SELECT '정보통신공학') AS t WHERE NOT EXISTS (SELECT 1 FROM majors WHERE name='정보통신공학');
INSERT INTO majors (name)
SELECT * FROM (SELECT '인공지능') AS t WHERE NOT EXISTS (SELECT 1 FROM majors WHERE name='인공지능');
INSERT INTO majors (name)
SELECT * FROM (SELECT '데이터사이언스') AS t WHERE NOT EXISTS (SELECT 1 FROM majors WHERE name='데이터사이언스');
INSERT INTO majors (name)
SELECT * FROM (SELECT '전자공학') AS t WHERE NOT EXISTS (SELECT 1 FROM majors WHERE name='전자공학');
INSERT INTO majors (name)
SELECT * FROM (SELECT '전기공학') AS t WHERE NOT EXISTS (SELECT 1 FROM majors WHERE name='전기공학');
INSERT INTO majors (name)
SELECT * FROM (SELECT '기계공학') AS t WHERE NOT EXISTS (SELECT 1 FROM majors WHERE name='기계공학');
INSERT INTO majors (name)
SELECT * FROM (SELECT '경영학') AS t WHERE NOT EXISTS (SELECT 1 FROM majors WHERE name='경영학');
INSERT INTO majors (name)
SELECT * FROM (SELECT '경제학') AS t WHERE NOT EXISTS (SELECT 1 FROM majors WHERE name='경제학');
INSERT INTO majors (name)
SELECT * FROM (SELECT '통계학') AS t WHERE NOT EXISTS (SELECT 1 FROM majors WHERE name='통계학');
INSERT INTO majors (name)
SELECT * FROM (SELECT '수학') AS t WHERE NOT EXISTS (SELECT 1 FROM majors WHERE name='수학');
INSERT INTO majors (name)
SELECT * FROM (SELECT '물리학') AS t WHERE NOT EXISTS (SELECT 1 FROM majors WHERE name='물리학');
INSERT INTO majors (name)
SELECT * FROM (SELECT '산업공학') AS t WHERE NOT EXISTS (SELECT 1 FROM majors WHERE name='산업공학');
INSERT INTO majors (name)
SELECT * FROM (SELECT '디자인공학') AS t WHERE NOT EXISTS (SELECT 1 FROM majors WHERE name='디자인공학');
INSERT INTO majors (name)
SELECT * FROM (SELECT '시각디자인') AS t WHERE NOT EXISTS (SELECT 1 FROM majors WHERE name='시각디자인');
INSERT INTO majors (name)
SELECT * FROM (SELECT '영어영문학') AS t WHERE NOT EXISTS (SELECT 1 FROM majors WHERE name='영어영문학');
INSERT INTO majors (name)
SELECT * FROM (SELECT '국어국문학') AS t WHERE NOT EXISTS (SELECT 1 FROM majors WHERE name='국어국문학');
INSERT INTO majors (name)
SELECT * FROM (SELECT '회계학') AS t WHERE NOT EXISTS (SELECT 1 FROM majors WHERE name='회계학');
