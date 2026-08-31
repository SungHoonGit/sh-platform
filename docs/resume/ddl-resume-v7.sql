-- ============================================================
-- resume 모듈 DDL v7 (2026-08-31)
-- DB: resume_platform  — 기준 데이터 확장 (고등학교 + 대학원 + 대학교 + 전공 시드)
--
-- 실행:
--   mysql -h 10.0.0.39 -u sh_user -p resume_platform < docs/resume/ddl-resume-v7.sql
--
-- 배경: v6부터 학교/전공을 DB 코드화. 프론트 하드코딩(schools.ts) 제거 후
--       '고등학교' 유형이 전무해 자동완성이 대학교/대학원만 안내되던 문제 보완.
--       고등학교 목록 + 수도권/지역 대학원, 대전·충청 중심 대학교, 전공 대분류를 추가 시드한다.
--       모든 INSERT는 멱등 (UNIQUE KEY 기준 NOT EXISTS).
-- ============================================================

-- ---------- 1. 고등학교 시드 ----------
INSERT INTO schools (name, school_type)
SELECT * FROM (SELECT '경기고등학교', '고등학교') AS t
WHERE NOT EXISTS (SELECT 1 FROM schools WHERE name='경기고등학교' AND school_type='고등학교');
INSERT INTO schools (name, school_type)
SELECT * FROM (SELECT '경복고등학교', '고등학교') AS t
WHERE NOT EXISTS (SELECT 1 FROM schools WHERE name='경복고등학교' AND school_type='고등학교');
INSERT INTO schools (name, school_type)
SELECT * FROM (SELECT '경성고등학교', '고등학교') AS t
WHERE NOT EXISTS (SELECT 1 FROM schools WHERE name='경성고등학교' AND school_type='고등학교');
INSERT INTO schools (name, school_type)
SELECT * FROM (SELECT '고려대학교사범대학부속고등학교', '고등학교') AS t
WHERE NOT EXISTS (SELECT 1 FROM schools WHERE name='고려대학교사범대학부속고등학교' AND school_type='고등학교');
INSERT INTO schools (name, school_type)
SELECT * FROM (SELECT '광성고등학교', '고등학교') AS t
WHERE NOT EXISTS (SELECT 1 FROM schools WHERE name='광성고등학교' AND school_type='고등학교');
INSERT INTO schools (name, school_type)
SELECT * FROM (SELECT '대광고등학교', '고등학교') AS t
WHERE NOT EXISTS (SELECT 1 FROM schools WHERE name='대광고등학교' AND school_type='고등학교');
INSERT INTO schools (name, school_type)
SELECT * FROM (SELECT '대원고등학교', '고등학교') AS t
WHERE NOT EXISTS (SELECT 1 FROM schools WHERE name='대원고등학교' AND school_type='고등학교');
INSERT INTO schools (name, school_type)
SELECT * FROM (SELECT '동성고등학교', '고등학교') AS t
WHERE NOT EXISTS (SELECT 1 FROM schools WHERE name='동성고등학교' AND school_type='고등학교');
INSERT INTO schools (name, school_type)
SELECT * FROM (SELECT '명덕고등학교', '고등학교') AS t
WHERE NOT EXISTS (SELECT 1 FROM schools WHERE name='명덕고등학교' AND school_type='고등학교');
INSERT INTO schools (name, school_type)
SELECT * FROM (SELECT '보성고등학교', '고등학교') AS t
WHERE NOT EXISTS (SELECT 1 FROM schools WHERE name='보성고등학교' AND school_type='고등학교');
INSERT INTO schools (name, school_type)
SELECT * FROM (SELECT '서울고등학교', '고등학교') AS t
WHERE NOT EXISTS (SELECT 1 FROM schools WHERE name='서울고등학교' AND school_type='고등학교');
INSERT INTO schools (name, school_type)
SELECT * FROM (SELECT '선정고등학교', '고등학교') AS t
WHERE NOT EXISTS (SELECT 1 FROM schools WHERE name='선정고등학교' AND school_type='고등학교');
INSERT INTO schools (name, school_type)
SELECT * FROM (SELECT '세화고등학교', '고등학교') AS t
WHERE NOT EXISTS (SELECT 1 FROM schools WHERE name='세화고등학교' AND school_type='고등학교');
INSERT INTO schools (name, school_type)
SELECT * FROM (SELECT '숭문고등학교', '고등학교') AS t
WHERE NOT EXISTS (SELECT 1 FROM schools WHERE name='숭문고등학교' AND school_type='고등학교');
INSERT INTO schools (name, school_type)
SELECT * FROM (SELECT '영동고등학교', '고등학교') AS t
WHERE NOT EXISTS (SELECT 1 FROM schools WHERE name='영동고등학교' AND school_type='고등학교');
INSERT INTO schools (name, school_type)
SELECT * FROM (SELECT '용산고등학교', '고등학교') AS t
WHERE NOT EXISTS (SELECT 1 FROM schools WHERE name='용산고등학교' AND school_type='고등학교');
INSERT INTO schools (name, school_type)
SELECT * FROM (SELECT '인창고등학교', '고등학교') AS t
WHERE NOT EXISTS (SELECT 1 FROM schools WHERE name='인창고등학교' AND school_type='고등학교');
INSERT INTO schools (name, school_type)
SELECT * FROM (SELECT '중동고등학교', '고등학교') AS t
WHERE NOT EXISTS (SELECT 1 FROM schools WHERE name='중동고등학교' AND school_type='고등학교');
INSERT INTO schools (name, school_type)
SELECT * FROM (SELECT '충암고등학교', '고등학교') AS t
WHERE NOT EXISTS (SELECT 1 FROM schools WHERE name='충암고등학교' AND school_type='고등학교');
INSERT INTO schools (name, school_type)
SELECT * FROM (SELECT '휘문고등학교', '고등학교') AS t
WHERE NOT EXISTS (SELECT 1 FROM schools WHERE name='휘문고등학교' AND school_type='고등학교');
INSERT INTO schools (name, school_type)
SELECT * FROM (SELECT '경동고등학교', '고등학교') AS t
WHERE NOT EXISTS (SELECT 1 FROM schools WHERE name='경동고등학교' AND school_type='고등학교');
INSERT INTO schools (name, school_type)
SELECT * FROM (SELECT '배재고등학교', '고등학교') AS t
WHERE NOT EXISTS (SELECT 1 FROM schools WHERE name='배재고등학교' AND school_type='고등학교');
INSERT INTO schools (name, school_type)
SELECT * FROM (SELECT '배화여자고등학교', '고등학교') AS t
WHERE NOT EXISTS (SELECT 1 FROM schools WHERE name='배화여자고등학교' AND school_type='고등학교');
INSERT INTO schools (name, school_type)
SELECT * FROM (SELECT '숙명여자고등학교', '고등학교') AS t
WHERE NOT EXISTS (SELECT 1 FROM schools WHERE name='숙명여자고등학교' AND school_type='고등학교');
INSERT INTO schools (name, school_type)
SELECT * FROM (SELECT '이화여자고등학교', '고등학교') AS t
WHERE NOT EXISTS (SELECT 1 FROM schools WHERE name='이화여자고등학교' AND school_type='고등학교');
INSERT INTO schools (name, school_type)
SELECT * FROM (SELECT '선일여자고등학교', '고등학교') AS t
WHERE NOT EXISTS (SELECT 1 FROM schools WHERE name='선일여자고등학교' AND school_type='고등학교');
INSERT INTO schools (name, school_type)
SELECT * FROM (SELECT '상명고등학교', '고등학교') AS t
WHERE NOT EXISTS (SELECT 1 FROM schools WHERE name='상명고등학교' AND school_type='고등학교');
INSERT INTO schools (name, school_type)
SELECT * FROM (SELECT '한성과학고등학교', '고등학교') AS t
WHERE NOT EXISTS (SELECT 1 FROM schools WHERE name='한성과학고등학교' AND school_type='고등학교');
INSERT INTO schools (name, school_type)
SELECT * FROM (SELECT '서울과학고등학교', '고등학교') AS t
WHERE NOT EXISTS (SELECT 1 FROM schools WHERE name='서울과학고등학교' AND school_type='고등학교');
INSERT INTO schools (name, school_type)
SELECT * FROM (SELECT '세종과학고등학교', '고등학교') AS t
WHERE NOT EXISTS (SELECT 1 FROM schools WHERE name='세종과학고등학교' AND school_type='고등학교');
INSERT INTO schools (name, school_type)
SELECT * FROM (SELECT '대원외국어고등학교', '고등학교') AS t
WHERE NOT EXISTS (SELECT 1 FROM schools WHERE name='대원외국어고등학교' AND school_type='고등학교');
INSERT INTO schools (name, school_type)
SELECT * FROM (SELECT '명덕외국어고등학교', '고등학교') AS t
WHERE NOT EXISTS (SELECT 1 FROM schools WHERE name='명덕외국어고등학교' AND school_type='고등학교');
INSERT INTO schools (name, school_type)
SELECT * FROM (SELECT '서울예술고등학교', '고등학교') AS t
WHERE NOT EXISTS (SELECT 1 FROM schools WHERE name='서울예술고등학교' AND school_type='고등학교');
INSERT INTO schools (name, school_type)
SELECT * FROM (SELECT '경기북과학고등학교', '고등학교') AS t
WHERE NOT EXISTS (SELECT 1 FROM schools WHERE name='경기북과학고등학교' AND school_type='고등학교');
INSERT INTO schools (name, school_type)
SELECT * FROM (SELECT '과천고등학교', '고등학교') AS t
WHERE NOT EXISTS (SELECT 1 FROM schools WHERE name='과천고등학교' AND school_type='고등학교');
INSERT INTO schools (name, school_type)
SELECT * FROM (SELECT '분당고등학교', '고등학교') AS t
WHERE NOT EXISTS (SELECT 1 FROM schools WHERE name='분당고등학교' AND school_type='고등학교');
INSERT INTO schools (name, school_type)
SELECT * FROM (SELECT '성남고등학교', '고등학교') AS t
WHERE NOT EXISTS (SELECT 1 FROM schools WHERE name='성남고등학교' AND school_type='고등학교');
INSERT INTO schools (name, school_type)
SELECT * FROM (SELECT '수원고등학교', '고등학교') AS t
WHERE NOT EXISTS (SELECT 1 FROM schools WHERE name='수원고등학교' AND school_type='고등학교');
INSERT INTO schools (name, school_type)
SELECT * FROM (SELECT '안양고등학교', '고등학교') AS t
WHERE NOT EXISTS (SELECT 1 FROM schools WHERE name='안양고등학교' AND school_type='고등학교');
INSERT INTO schools (name, school_type)
SELECT * FROM (SELECT '일산동국고등학교', '고등학교') AS t
WHERE NOT EXISTS (SELECT 1 FROM schools WHERE name='일산동국고등학교' AND school_type='고등학교');
INSERT INTO schools (name, school_type)
SELECT * FROM (SELECT '송도고등학교', '고등학교') AS t
WHERE NOT EXISTS (SELECT 1 FROM schools WHERE name='송도고등학교' AND school_type='고등학교');
INSERT INTO schools (name, school_type)
SELECT * FROM (SELECT '인천고등학교', '고등학교') AS t
WHERE NOT EXISTS (SELECT 1 FROM schools WHERE name='인천고등학교' AND school_type='고등학교');
INSERT INTO schools (name, school_type)
SELECT * FROM (SELECT '인천과학예술영재학교', '고등학교') AS t
WHERE NOT EXISTS (SELECT 1 FROM schools WHERE name='인천과학예술영재학교' AND school_type='고등학교');
INSERT INTO schools (name, school_type)
SELECT * FROM (SELECT '부천고등학교', '고등학교') AS t
WHERE NOT EXISTS (SELECT 1 FROM schools WHERE name='부천고등학교' AND school_type='고등학교');
INSERT INTO schools (name, school_type)
SELECT * FROM (SELECT '평촌고등학교', '고등학교') AS t
WHERE NOT EXISTS (SELECT 1 FROM schools WHERE name='평촌고등학교' AND school_type='고등학교');
INSERT INTO schools (name, school_type)
SELECT * FROM (SELECT '대전고등학교', '고등학교') AS t
WHERE NOT EXISTS (SELECT 1 FROM schools WHERE name='대전고등학교' AND school_type='고등학교');
INSERT INTO schools (name, school_type)
SELECT * FROM (SELECT '대전과학고등학교', '고등학교') AS t
WHERE NOT EXISTS (SELECT 1 FROM schools WHERE name='대전과학고등학교' AND school_type='고등학교');
INSERT INTO schools (name, school_type)
SELECT * FROM (SELECT '대전외국어고등학교', '고등학교') AS t
WHERE NOT EXISTS (SELECT 1 FROM schools WHERE name='대전외국어고등학교' AND school_type='고등학교');
INSERT INTO schools (name, school_type)
SELECT * FROM (SELECT '대전둔산고등학교', '고등학교') AS t
WHERE NOT EXISTS (SELECT 1 FROM schools WHERE name='대전둔산고등학교' AND school_type='고등학교');
INSERT INTO schools (name, school_type)
SELECT * FROM (SELECT '대전상원고등학교', '고등학교') AS t
WHERE NOT EXISTS (SELECT 1 FROM schools WHERE name='대전상원고등학교' AND school_type='고등학교');
INSERT INTO schools (name, school_type)
SELECT * FROM (SELECT '대전지족고등학교', '고등학교') AS t
WHERE NOT EXISTS (SELECT 1 FROM schools WHERE name='대전지족고등학교' AND school_type='고등학교');
INSERT INTO schools (name, school_type)
SELECT * FROM (SELECT '대전대성고등학교', '고등학교') AS t
WHERE NOT EXISTS (SELECT 1 FROM schools WHERE name='대전대성고등학교' AND school_type='고등학교');
INSERT INTO schools (name, school_type)
SELECT * FROM (SELECT '서대전고등학교', '고등학교') AS t
WHERE NOT EXISTS (SELECT 1 FROM schools WHERE name='서대전고등학교' AND school_type='고등학교');
INSERT INTO schools (name, school_type)
SELECT * FROM (SELECT '충남고등학교', '고등학교') AS t
WHERE NOT EXISTS (SELECT 1 FROM schools WHERE name='충남고등학교' AND school_type='고등학교');
INSERT INTO schools (name, school_type)
SELECT * FROM (SELECT '충남여자고등학교', '고등학교') AS t
WHERE NOT EXISTS (SELECT 1 FROM schools WHERE name='충남여자고등학교' AND school_type='고등학교');
INSERT INTO schools (name, school_type)
SELECT * FROM (SELECT '세종국제고등학교', '고등학교') AS t
WHERE NOT EXISTS (SELECT 1 FROM schools WHERE name='세종국제고등학교' AND school_type='고등학교');
INSERT INTO schools (name, school_type)
SELECT * FROM (SELECT '세종예술고등학교', '고등학교') AS t
WHERE NOT EXISTS (SELECT 1 FROM schools WHERE name='세종예술고등학교' AND school_type='고등학교');
INSERT INTO schools (name, school_type)
SELECT * FROM (SELECT '청주고등학교', '고등학교') AS t
WHERE NOT EXISTS (SELECT 1 FROM schools WHERE name='청주고등학교' AND school_type='고등학교');
INSERT INTO schools (name, school_type)
SELECT * FROM (SELECT '청주여자고등학교', '고등학교') AS t
WHERE NOT EXISTS (SELECT 1 FROM schools WHERE name='청주여자고등학교' AND school_type='고등학교');
INSERT INTO schools (name, school_type)
SELECT * FROM (SELECT '충북고등학교', '고등학교') AS t
WHERE NOT EXISTS (SELECT 1 FROM schools WHERE name='충북고등학교' AND school_type='고등학교');
INSERT INTO schools (name, school_type)
SELECT * FROM (SELECT '북일고등학교', '고등학교') AS t
WHERE NOT EXISTS (SELECT 1 FROM schools WHERE name='북일고등학교' AND school_type='고등학교');
INSERT INTO schools (name, school_type)
SELECT * FROM (SELECT '공주고등학교', '고등학교') AS t
WHERE NOT EXISTS (SELECT 1 FROM schools WHERE name='공주고등학교' AND school_type='고등학교');
INSERT INTO schools (name, school_type)
SELECT * FROM (SELECT '천안고등학교', '고등학교') AS t
WHERE NOT EXISTS (SELECT 1 FROM schools WHERE name='천안고등학교' AND school_type='고등학교');
INSERT INTO schools (name, school_type)
SELECT * FROM (SELECT '천안중앙고등학교', '고등학교') AS t
WHERE NOT EXISTS (SELECT 1 FROM schools WHERE name='천안중앙고등학교' AND school_type='고등학교');
INSERT INTO schools (name, school_type)
SELECT * FROM (SELECT '당진고등학교', '고등학교') AS t
WHERE NOT EXISTS (SELECT 1 FROM schools WHERE name='당진고등학교' AND school_type='고등학교');
INSERT INTO schools (name, school_type)
SELECT * FROM (SELECT '부산고등학교', '고등학교') AS t
WHERE NOT EXISTS (SELECT 1 FROM schools WHERE name='부산고등학교' AND school_type='고등학교');
INSERT INTO schools (name, school_type)
SELECT * FROM (SELECT '부산과학고등학교', '고등학교') AS t
WHERE NOT EXISTS (SELECT 1 FROM schools WHERE name='부산과학고등학교' AND school_type='고등학교');
INSERT INTO schools (name, school_type)
SELECT * FROM (SELECT '경남과학고등학교', '고등학교') AS t
WHERE NOT EXISTS (SELECT 1 FROM schools WHERE name='경남과학고등학교' AND school_type='고등학교');
INSERT INTO schools (name, school_type)
SELECT * FROM (SELECT '마산고등학교', '고등학교') AS t
WHERE NOT EXISTS (SELECT 1 FROM schools WHERE name='마산고등학교' AND school_type='고등학교');
INSERT INTO schools (name, school_type)
SELECT * FROM (SELECT '울산과학고등학교', '고등학교') AS t
WHERE NOT EXISTS (SELECT 1 FROM schools WHERE name='울산과학고등학교' AND school_type='고등학교');
INSERT INTO schools (name, school_type)
SELECT * FROM (SELECT '울산고등학교', '고등학교') AS t
WHERE NOT EXISTS (SELECT 1 FROM schools WHERE name='울산고등학교' AND school_type='고등학교');
INSERT INTO schools (name, school_type)
SELECT * FROM (SELECT '경북고등학교', '고등학교') AS t
WHERE NOT EXISTS (SELECT 1 FROM schools WHERE name='경북고등학교' AND school_type='고등학교');
INSERT INTO schools (name, school_type)
SELECT * FROM (SELECT '대구고등학교', '고등학교') AS t
WHERE NOT EXISTS (SELECT 1 FROM schools WHERE name='대구고등학교' AND school_type='고등학교');
INSERT INTO schools (name, school_type)
SELECT * FROM (SELECT '대구과학고등학교', '고등학교') AS t
WHERE NOT EXISTS (SELECT 1 FROM schools WHERE name='대구과학고등학교' AND school_type='고등학교');
INSERT INTO schools (name, school_type)
SELECT * FROM (SELECT '포항고등학교', '고등학교') AS t
WHERE NOT EXISTS (SELECT 1 FROM schools WHERE name='포항고등학교' AND school_type='고등학교');
INSERT INTO schools (name, school_type)
SELECT * FROM (SELECT '포항제철고등학교', '고등학교') AS t
WHERE NOT EXISTS (SELECT 1 FROM schools WHERE name='포항제철고등학교' AND school_type='고등학교');
INSERT INTO schools (name, school_type)
SELECT * FROM (SELECT '광주과학고등학교', '고등학교') AS t
WHERE NOT EXISTS (SELECT 1 FROM schools WHERE name='광주과학고등학교' AND school_type='고등학교');
INSERT INTO schools (name, school_type)
SELECT * FROM (SELECT '광주고등학교', '고등학교') AS t
WHERE NOT EXISTS (SELECT 1 FROM schools WHERE name='광주고등학교' AND school_type='고등학교');
INSERT INTO schools (name, school_type)
SELECT * FROM (SELECT '전남고등학교', '고등학교') AS t
WHERE NOT EXISTS (SELECT 1 FROM schools WHERE name='전남고등학교' AND school_type='고등학교');
INSERT INTO schools (name, school_type)
SELECT * FROM (SELECT '목포고등학교', '고등학교') AS t
WHERE NOT EXISTS (SELECT 1 FROM schools WHERE name='목포고등학교' AND school_type='고등학교');
INSERT INTO schools (name, school_type)
SELECT * FROM (SELECT '전주고등학교', '고등학교') AS t
WHERE NOT EXISTS (SELECT 1 FROM schools WHERE name='전주고등학교' AND school_type='고등학교');
INSERT INTO schools (name, school_type)
SELECT * FROM (SELECT '군산고등학교', '고등학교') AS t
WHERE NOT EXISTS (SELECT 1 FROM schools WHERE name='군산고등학교' AND school_type='고등학교');
INSERT INTO schools (name, school_type)
SELECT * FROM (SELECT '강원과학고등학교', '고등학교') AS t
WHERE NOT EXISTS (SELECT 1 FROM schools WHERE name='강원과학고등학교' AND school_type='고등학교');
INSERT INTO schools (name, school_type)
SELECT * FROM (SELECT '춘천고등학교', '고등학교') AS t
WHERE NOT EXISTS (SELECT 1 FROM schools WHERE name='춘천고등학교' AND school_type='고등학교');
INSERT INTO schools (name, school_type)
SELECT * FROM (SELECT '원주고등학교', '고등학교') AS t
WHERE NOT EXISTS (SELECT 1 FROM schools WHERE name='원주고등학교' AND school_type='고등학교');
INSERT INTO schools (name, school_type)
SELECT * FROM (SELECT '제주과학고등학교', '고등학교') AS t
WHERE NOT EXISTS (SELECT 1 FROM schools WHERE name='제주과학고등학교' AND school_type='고등학교');
INSERT INTO schools (name, school_type)
SELECT * FROM (SELECT '제주제일고등학교', '고등학교') AS t
WHERE NOT EXISTS (SELECT 1 FROM schools WHERE name='제주제일고등학교' AND school_type='고등학교');

-- ---------- 2. 대학원 시드 (v6에 없던 대학원 확충) ----------
INSERT INTO schools (name, school_type)
SELECT * FROM (SELECT '중앙대학교 대학원', '대학원') AS t
WHERE NOT EXISTS (SELECT 1 FROM schools WHERE name='중앙대학교 대학원' AND school_type='대학원');
INSERT INTO schools (name, school_type)
SELECT * FROM (SELECT '경희대학교 대학원', '대학원') AS t
WHERE NOT EXISTS (SELECT 1 FROM schools WHERE name='경희대학교 대학원' AND school_type='대학원');
INSERT INTO schools (name, school_type)
SELECT * FROM (SELECT '한국외국어대학교 대학원', '대학원') AS t
WHERE NOT EXISTS (SELECT 1 FROM schools WHERE name='한국외국어대학교 대학원' AND school_type='대학원');
INSERT INTO schools (name, school_type)
SELECT * FROM (SELECT '서울시립대학교 대학원', '대학원') AS t
WHERE NOT EXISTS (SELECT 1 FROM schools WHERE name='서울시립대학교 대학원' AND school_type='대학원');
INSERT INTO schools (name, school_type)
SELECT * FROM (SELECT '건국대학교 대학원', '대학원') AS t
WHERE NOT EXISTS (SELECT 1 FROM schools WHERE name='건국대학교 대학원' AND school_type='대학원');
INSERT INTO schools (name, school_type)
SELECT * FROM (SELECT '동국대학교 대학원', '대학원') AS t
WHERE NOT EXISTS (SELECT 1 FROM schools WHERE name='동국대학교 대학원' AND school_type='대학원');
INSERT INTO schools (name, school_type)
SELECT * FROM (SELECT '홍익대학교 대학원', '대학원') AS t
WHERE NOT EXISTS (SELECT 1 FROM schools WHERE name='홍익대학교 대학원' AND school_type='대학원');
INSERT INTO schools (name, school_type)
SELECT * FROM (SELECT '숭실대학교 대학원', '대학원') AS t
WHERE NOT EXISTS (SELECT 1 FROM schools WHERE name='숭실대학교 대학원' AND school_type='대학원');
INSERT INTO schools (name, school_type)
SELECT * FROM (SELECT '국민대학교 대학원', '대학원') AS t
WHERE NOT EXISTS (SELECT 1 FROM schools WHERE name='국민대학교 대학원' AND school_type='대학원');
INSERT INTO schools (name, school_type)
SELECT * FROM (SELECT '세종대학교 대학원', '대학원') AS t
WHERE NOT EXISTS (SELECT 1 FROM schools WHERE name='세종대학교 대학원' AND school_type='대학원');
INSERT INTO schools (name, school_type)
SELECT * FROM (SELECT '이화여자대학교 대학원', '대학원') AS t
WHERE NOT EXISTS (SELECT 1 FROM schools WHERE name='이화여자대학교 대학원' AND school_type='대학원');
INSERT INTO schools (name, school_type)
SELECT * FROM (SELECT '숙명여자대학교 대학원', '대학원') AS t
WHERE NOT EXISTS (SELECT 1 FROM schools WHERE name='숙명여자대학교 대학원' AND school_type='대학원');
INSERT INTO schools (name, school_type)
SELECT * FROM (SELECT '인하대학교 대학원', '대학원') AS t
WHERE NOT EXISTS (SELECT 1 FROM schools WHERE name='인하대학교 대학원' AND school_type='대학원');
INSERT INTO schools (name, school_type)
SELECT * FROM (SELECT '아주대학교 대학원', '대학원') AS t
WHERE NOT EXISTS (SELECT 1 FROM schools WHERE name='아주대학교 대학원' AND school_type='대학원');
INSERT INTO schools (name, school_type)
SELECT * FROM (SELECT '인천대학교 대학원', '대학원') AS t
WHERE NOT EXISTS (SELECT 1 FROM schools WHERE name='인천대학교 대학원' AND school_type='대학원');
INSERT INTO schools (name, school_type)
SELECT * FROM (SELECT '가천대학교 대학원', '대학원') AS t
WHERE NOT EXISTS (SELECT 1 FROM schools WHERE name='가천대학교 대학원' AND school_type='대학원');
INSERT INTO schools (name, school_type)
SELECT * FROM (SELECT '단국대학교 대학원', '대학원') AS t
WHERE NOT EXISTS (SELECT 1 FROM schools WHERE name='단국대학교 대학원' AND school_type='대학원');
INSERT INTO schools (name, school_type)
SELECT * FROM (SELECT '경기대학교 대학원', '대학원') AS t
WHERE NOT EXISTS (SELECT 1 FROM schools WHERE name='경기대학교 대학원' AND school_type='대학원');
INSERT INTO schools (name, school_type)
SELECT * FROM (SELECT '한양대학교 ERICA 대학원', '대학원') AS t
WHERE NOT EXISTS (SELECT 1 FROM schools WHERE name='한양대학교 ERICA 대학원' AND school_type='대학원');
INSERT INTO schools (name, school_type)
SELECT * FROM (SELECT '부산대학교 대학원', '대학원') AS t
WHERE NOT EXISTS (SELECT 1 FROM schools WHERE name='부산대학교 대학원' AND school_type='대학원');
INSERT INTO schools (name, school_type)
SELECT * FROM (SELECT '경북대학교 대학원', '대학원') AS t
WHERE NOT EXISTS (SELECT 1 FROM schools WHERE name='경북대학교 대학원' AND school_type='대학원');
INSERT INTO schools (name, school_type)
SELECT * FROM (SELECT '전남대학교 대학원', '대학원') AS t
WHERE NOT EXISTS (SELECT 1 FROM schools WHERE name='전남대학교 대학원' AND school_type='대학원');
INSERT INTO schools (name, school_type)
SELECT * FROM (SELECT '전북대학교 대학원', '대학원') AS t
WHERE NOT EXISTS (SELECT 1 FROM schools WHERE name='전북대학교 대학원' AND school_type='대학원');
INSERT INTO schools (name, school_type)
SELECT * FROM (SELECT '충남대학교 대학원', '대학원') AS t
WHERE NOT EXISTS (SELECT 1 FROM schools WHERE name='충남대학교 대학원' AND school_type='대학원');
INSERT INTO schools (name, school_type)
SELECT * FROM (SELECT '충북대학교 대학원', '대학원') AS t
WHERE NOT EXISTS (SELECT 1 FROM schools WHERE name='충북대학교 대학원' AND school_type='대학원');
INSERT INTO schools (name, school_type)
SELECT * FROM (SELECT '강원대학교 대학원', '대학원') AS t
WHERE NOT EXISTS (SELECT 1 FROM schools WHERE name='강원대학교 대학원' AND school_type='대학원');
INSERT INTO schools (name, school_type)
SELECT * FROM (SELECT '제주대학교 대학원', '대학원') AS t
WHERE NOT EXISTS (SELECT 1 FROM schools WHERE name='제주대학교 대학원' AND school_type='대학원');
INSERT INTO schools (name, school_type)
SELECT * FROM (SELECT '목포대학교 대학원', '대학원') AS t
WHERE NOT EXISTS (SELECT 1 FROM schools WHERE name='목포대학교 대학원' AND school_type='대학원');
INSERT INTO schools (name, school_type)
SELECT * FROM (SELECT '동아대학교 대학원', '대학원') AS t
WHERE NOT EXISTS (SELECT 1 FROM schools WHERE name='동아대학교 대학원' AND school_type='대학원');
INSERT INTO schools (name, school_type)
SELECT * FROM (SELECT '부경대학교 대학원', '대학원') AS t
WHERE NOT EXISTS (SELECT 1 FROM schools WHERE name='부경대학교 대학원' AND school_type='대학원');
INSERT INTO schools (name, school_type)
SELECT * FROM (SELECT '울산대학교 대학원', '대학원') AS t
WHERE NOT EXISTS (SELECT 1 FROM schools WHERE name='울산대학교 대학원' AND school_type='대학원');
INSERT INTO schools (name, school_type)
SELECT * FROM (SELECT '포항공과대학교 대학원', '대학원') AS t
WHERE NOT EXISTS (SELECT 1 FROM schools WHERE name='포항공과대학교 대학원' AND school_type='대학원');
INSERT INTO schools (name, school_type)
SELECT * FROM (SELECT 'GIST 대학원', '대학원') AS t
WHERE NOT EXISTS (SELECT 1 FROM schools WHERE name='GIST 대학원' AND school_type='대학원');
INSERT INTO schools (name, school_type)
SELECT * FROM (SELECT 'DGIST 대학원', '대학원') AS t
WHERE NOT EXISTS (SELECT 1 FROM schools WHERE name='DGIST 대학원' AND school_type='대학원');
INSERT INTO schools (name, school_type)
SELECT * FROM (SELECT 'UNIST 대학원', '대학원') AS t
WHERE NOT EXISTS (SELECT 1 FROM schools WHERE name='UNIST 대학원' AND school_type='대학원');
INSERT INTO schools (name, school_type)
SELECT * FROM (SELECT '한남대학교 대학원', '대학원') AS t
WHERE NOT EXISTS (SELECT 1 FROM schools WHERE name='한남대학교 대학원' AND school_type='대학원');
INSERT INTO schools (name, school_type)
SELECT * FROM (SELECT '대전대학교 대학원', '대학원') AS t
WHERE NOT EXISTS (SELECT 1 FROM schools WHERE name='대전대학교 대학원' AND school_type='대학원');
INSERT INTO schools (name, school_type)
SELECT * FROM (SELECT '한밭대학교 대학원', '대학원') AS t
WHERE NOT EXISTS (SELECT 1 FROM schools WHERE name='한밭대학교 대학원' AND school_type='대학원');
INSERT INTO schools (name, school_type)
SELECT * FROM (SELECT '한국기술교육대학교 대학원', '대학원') AS t
WHERE NOT EXISTS (SELECT 1 FROM schools WHERE name='한국기술교육대학교 대학원' AND school_type='대학원');

-- ---------- 3. 대학교 추가 시드 (대전·충청 + 수도권 중심) ----------
INSERT INTO schools (name, school_type)
SELECT * FROM (SELECT '한남대학교', '대학교') AS t
WHERE NOT EXISTS (SELECT 1 FROM schools WHERE name='한남대학교' AND school_type='대학교');
INSERT INTO schools (name, school_type)
SELECT * FROM (SELECT '대전대학교', '대학교') AS t
WHERE NOT EXISTS (SELECT 1 FROM schools WHERE name='대전대학교' AND school_type='대학교');
INSERT INTO schools (name, school_type)
SELECT * FROM (SELECT '배재대학교', '대학교') AS t
WHERE NOT EXISTS (SELECT 1 FROM schools WHERE name='배재대학교' AND school_type='대학교');
INSERT INTO schools (name, school_type)
SELECT * FROM (SELECT '우송대학교', '대학교') AS t
WHERE NOT EXISTS (SELECT 1 FROM schools WHERE name='우송대학교' AND school_type='대학교');
INSERT INTO schools (name, school_type)
SELECT * FROM (SELECT '한밭대학교', '대학교') AS t
WHERE NOT EXISTS (SELECT 1 FROM schools WHERE name='한밭대학교' AND school_type='대학교');
INSERT INTO schools (name, school_type)
SELECT * FROM (SELECT '목원대학교', '대학교') AS t
WHERE NOT EXISTS (SELECT 1 FROM schools WHERE name='목원대학교' AND school_type='대학교');
INSERT INTO schools (name, school_type)
SELECT * FROM (SELECT '청주대학교', '대학교') AS t
WHERE NOT EXISTS (SELECT 1 FROM schools WHERE name='청주대학교' AND school_type='대학교');
INSERT INTO schools (name, school_type)
SELECT * FROM (SELECT '건양대학교', '대학교') AS t
WHERE NOT EXISTS (SELECT 1 FROM schools WHERE name='건양대학교' AND school_type='대학교');
INSERT INTO schools (name, school_type)
SELECT * FROM (SELECT '을지대학교', '대학교') AS t
WHERE NOT EXISTS (SELECT 1 FROM schools WHERE name='을지대학교' AND school_type='대학교');
INSERT INTO schools (name, school_type)
SELECT * FROM (SELECT '서울여자대학교', '대학교') AS t
WHERE NOT EXISTS (SELECT 1 FROM schools WHERE name='서울여자대학교' AND school_type='대학교');
INSERT INTO schools (name, school_type)
SELECT * FROM (SELECT '덕성여자대학교', '대학교') AS t
WHERE NOT EXISTS (SELECT 1 FROM schools WHERE name='덕성여자대학교' AND school_type='대학교');
INSERT INTO schools (name, school_type)
SELECT * FROM (SELECT '한성대학교', '대학교') AS t
WHERE NOT EXISTS (SELECT 1 FROM schools WHERE name='한성대학교' AND school_type='대학교');
INSERT INTO schools (name, school_type)
SELECT * FROM (SELECT '상명대학교', '대학교') AS t
WHERE NOT EXISTS (SELECT 1 FROM schools WHERE name='상명대학교' AND school_type='대학교');

-- ---------- 4. 전공 시드 확장 ----------
INSERT INTO majors (name)
SELECT * FROM (SELECT '화학공학') AS t WHERE NOT EXISTS (SELECT 1 FROM majors WHERE name='화학공학');
INSERT INTO majors (name)
SELECT * FROM (SELECT '화학') AS t WHERE NOT EXISTS (SELECT 1 FROM majors WHERE name='화학');
INSERT INTO majors (name)
SELECT * FROM (SELECT '생명과학') AS t WHERE NOT EXISTS (SELECT 1 FROM majors WHERE name='생명과학');
INSERT INTO majors (name)
SELECT * FROM (SELECT '생물학') AS t WHERE NOT EXISTS (SELECT 1 FROM majors WHERE name='생물학');
INSERT INTO majors (name)
SELECT * FROM (SELECT '간호학') AS t WHERE NOT EXISTS (SELECT 1 FROM majors WHERE name='간호학');
INSERT INTO majors (name)
SELECT * FROM (SELECT '건축학') AS t WHERE NOT EXISTS (SELECT 1 FROM majors WHERE name='건축학');
INSERT INTO majors (name)
SELECT * FROM (SELECT '건축공학') AS t WHERE NOT EXISTS (SELECT 1 FROM majors WHERE name='건축공학');
INSERT INTO majors (name)
SELECT * FROM (SELECT '도시공학') AS t WHERE NOT EXISTS (SELECT 1 FROM majors WHERE name='도시공학');
INSERT INTO majors (name)
SELECT * FROM (SELECT '재료공학') AS t WHERE NOT EXISTS (SELECT 1 FROM majors WHERE name='재료공학');
INSERT INTO majors (name)
SELECT * FROM (SELECT '신소재공학') AS t WHERE NOT EXISTS (SELECT 1 FROM majors WHERE name='신소재공학');
INSERT INTO majors (name)
SELECT * FROM (SELECT '고분자공학') AS t WHERE NOT EXISTS (SELECT 1 FROM majors WHERE name='고분자공학');
INSERT INTO majors (name)
SELECT * FROM (SELECT '조선해양공학') AS t WHERE NOT EXISTS (SELECT 1 FROM majors WHERE name='조선해양공학');
INSERT INTO majors (name)
SELECT * FROM (SELECT '항공우주공학') AS t WHERE NOT EXISTS (SELECT 1 FROM majors WHERE name='항공우주공학');
INSERT INTO majors (name)
SELECT * FROM (SELECT '로봇공학') AS t WHERE NOT EXISTS (SELECT 1 FROM majors WHERE name='로봇공학');
INSERT INTO majors (name)
SELECT * FROM (SELECT '의생명공학') AS t WHERE NOT EXISTS (SELECT 1 FROM majors WHERE name='의생명공학');
INSERT INTO majors (name)
SELECT * FROM (SELECT '바이오공학') AS t WHERE NOT EXISTS (SELECT 1 FROM majors WHERE name='바이오공학');
INSERT INTO majors (name)
SELECT * FROM (SELECT '식품영양학') AS t WHERE NOT EXISTS (SELECT 1 FROM majors WHERE name='식품영양학');
INSERT INTO majors (name)
SELECT * FROM (SELECT '응용통계학') AS t WHERE NOT EXISTS (SELECT 1 FROM majors WHERE name='응용통계학');
INSERT INTO majors (name)
SELECT * FROM (SELECT '산업디자인') AS t WHERE NOT EXISTS (SELECT 1 FROM majors WHERE name='산업디자인');
INSERT INTO majors (name)
SELECT * FROM (SELECT '심리학') AS t WHERE NOT EXISTS (SELECT 1 FROM majors WHERE name='심리학');
INSERT INTO majors (name)
SELECT * FROM (SELECT '사회학') AS t WHERE NOT EXISTS (SELECT 1 FROM majors WHERE name='사회학');
INSERT INTO majors (name)
SELECT * FROM (SELECT '정치외교학') AS t WHERE NOT EXISTS (SELECT 1 FROM majors WHERE name='정치외교학');
INSERT INTO majors (name)
SELECT * FROM (SELECT '행정학') AS t WHERE NOT EXISTS (SELECT 1 FROM majors WHERE name='행정학');
INSERT INTO majors (name)
SELECT * FROM (SELECT '국제통상학') AS t WHERE NOT EXISTS (SELECT 1 FROM majors WHERE name='국제통상학');
INSERT INTO majors (name)
SELECT * FROM (SELECT '교육학') AS t WHERE NOT EXISTS (SELECT 1 FROM majors WHERE name='교육학');
INSERT INTO majors (name)
SELECT * FROM (SELECT '유아교육학') AS t WHERE NOT EXISTS (SELECT 1 FROM majors WHERE name='유아교육학');
INSERT INTO majors (name)
SELECT * FROM (SELECT '중어중문학') AS t WHERE NOT EXISTS (SELECT 1 FROM majors WHERE name='중어중문학');
INSERT INTO majors (name)
SELECT * FROM (SELECT '일어일문학') AS t WHERE NOT EXISTS (SELECT 1 FROM majors WHERE name='일어일문학');
INSERT INTO majors (name)
SELECT * FROM (SELECT '사학') AS t WHERE NOT EXISTS (SELECT 1 FROM majors WHERE name='사학');
INSERT INTO majors (name)
SELECT * FROM (SELECT '철학') AS t WHERE NOT EXISTS (SELECT 1 FROM majors WHERE name='철학');