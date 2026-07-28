# 작업 이력

## 2026-07-28

### 작업 내용
1. **스크래퍼 검색 파라미터 표준화 설계** ✅
   - 사이트별 검색 파라미터 실제 적용 여부 분석 완료
   - `site_search_mapping` 테이블 DDL 설계
   - 법정동코드 기반 표준 지역 코드 체계 설계
   - SiteSearchMapper 서비스 아키텍처 설계
   - 크롤러별 URL 파라미터 매핑 규칙 정의 (4개 사이트)
   - 프론트엔드 동적 드롭다운 설계

2. **설계 문서 작성** ✅
   - `docs/scraper/search-parameter-standardization.md` — 종합 설계 문서
   - `docs/daily/2026-07-28-todo.md` — 작업 예정

3. **사이트별 URL 파라미터 실제 테스트** ✅
   - 사람인 `loc_cd=101000`: 경기(23→0), 서울(53→58) — 필터 작동 확인
   - 잡코리아 `local=I000`: 부분적 작동 (서울 86→130, 경기 48→29)
   - 원티드 `query=Java`: 키워드 검색 작동 확인
   - 원티드 `years=3`: 경력 필터 작동 (20→14건)
   - 원티드 `locations=seoul`: 필터 미작동 확인

4. **크롤러 하드코딩 매핑 적용** ✅
   - SaraminCrawler: `loc_cd` 파라미터 + `mapLocationCode()` 추가
   - JobkoreaCrawler: `local` 파라미터 + `mapLocationCode()` 추가
   - WantedCrawler: `query`, `years`, `locations` 파라미터 + 매핑 함수 추가
   - RememberCrawler: `query`, `min_experience`, `sido` 파라미터 + 매핑 함수 추가

### 문제점 확인
- location 파라미터가 어떤 크롤러에서도 미적용 → **크롤러 수정으로 해결**
- Wanted/Remember 키워드가 죽은 코드 → **크롤러 수정으로 해결**
- site_parameter_definition 테이블 Dead Code
- Jobkorea 경력 매핑 단순 (신입/경력만 구분)
- 잡코리아 local 파라미터 정확한 매핑 코드 추가 확인 필요

### 다음 단계
- [x] 사이트별 URL 파라미터 확인 (사람인 loc_cd, 잡코리아 local 등)
- [x] 크롤러별 buildUrl() 수정 (하드코딩 매핑 적용)
- [ ] site_search_mapping DDL 실행
- [ ] SiteSearchMapper 서비스 구현
- [ ] 프론트엔드 동적 드롭다운

---

## 2026-07-13

### 작업 내용
1. **테넌트 관리 테스트케이스 작성** ✅
   - TenantServiceImplTest.java - 10개 테스트 메서드
   - 테스트 통과 확인

2. **scraper-platform DDL 작성** ✅
   - docs/ddl.sql - category, crawl_data, crawl_log 테이블
   - OCI 콘솔에서 DB 생성 후 실행 대기

3. **문서 정리** ✅
   - 프로젝트별 문서 분리
   - README 인덱스 생성

### 대기 중
- [ ] OCI 콘솔에서 scraper_platform DB 생성
- [ ] DDL 실행
- [ ] scraper-platform 프로젝트 초기화
