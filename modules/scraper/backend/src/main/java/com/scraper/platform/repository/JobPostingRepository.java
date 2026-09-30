package com.scraper.platform.repository;

import com.scraper.platform.model.JobPosting;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * 채용공고 Repository.
 * DB 기반 중복 체크 및 조회용.
 */
@Repository
public interface JobPostingRepository extends JpaRepository<JobPosting, Long> {

    Page<JobPosting> findByConfigId(Long configId, Pageable pageable);

    Page<JobPosting> findByConfigIdAndSiteName(Long configId, String siteName, Pageable pageable);

    Page<JobPosting> findByConfigIdAndCrawledAt(Long configId, LocalDate crawledAt, Pageable pageable);

    Page<JobPosting> findByConfigIdAndSiteNameAndCrawledAt(Long configId, String siteName, LocalDate crawledAt, Pageable pageable);

    Page<JobPosting> findByConfigIdAndCreatedAtBetween(Long configId, LocalDateTime start, LocalDateTime end, Pageable pageable);

    @Query("SELECT j.dedupKey FROM JobPosting j WHERE j.crawledAt >= :sinceDate")
    Set<String> findDedupKeysSince(@Param("sinceDate") LocalDate sinceDate);

    /**
     * 쿼리 조건(예: Java 공고 export)에 맞는 공고를 페이지네이션으로 조회한다.
     * keyword는 company/position/tech 대상 부분 일치(대소문자 무시)로 매칭한다.
     *
     * @param keyword  검색 키워드 (null/blank 시 전체 조회)
     * @param pageable 페이지네이션
     * @return 페이지 공고
     */
    @Query("""
            SELECT j FROM JobPosting j
            WHERE (:keyword IS NULL OR :keyword = '' OR
                   LOWER(j.company) LIKE LOWER(CONCAT('%', :keyword, '%'))
                OR LOWER(j.position) LIKE LOWER(CONCAT('%', :keyword, '%'))
                OR LOWER(j.tech) LIKE LOWER(CONCAT('%', :keyword, '%')))
            """)
    Page<JobPosting> searchExport(@Param("keyword") String keyword, Pageable pageable);

    long countByConfigId(Long configId);

    Optional<JobPosting> findByDedupKeyAndCrawledAt(String dedupKey, java.time.LocalDate crawledAt);

    long countByConfigIdAndCrawledAt(Long configId, LocalDate crawledAt);

    @Query("SELECT MAX(j.crawledAt) FROM JobPosting j WHERE j.config.id = :configId")
    LocalDate findLastCrawledAt(@Param("configId") Long configId);

    /**
     * 최근 수집 공고 + 블랙리스트 제외 (설계 036).
     * 차단 판정은 SQL에서 정규화 표현식 기준으로 수행한다 — 원문 회사명을 정규화명과 비교하던
     * 기존 NOT IN 의 정합성 이슈도 함께 해결한다.
     * exact = 정규화명 정확일치, contains = 정규화명 부분일치(LIKE).
     * REPLACE 체인은 CompanyBlacklistService.normalize 의 SQL 근사 (탭 공백 등 희귀 케이스 근사 오차 가능).
     */
    @Query("""
            SELECT j FROM JobPosting j LEFT JOIN j.config c
            WHERE ((c IS NOT NULL AND c.accountId = :accountId)
                OR j.savedByAccountId = :accountId)
              AND (:keyword IS NULL OR LOWER(j.company) LIKE LOWER(CONCAT('%', :keyword, '%'))
                   OR LOWER(j.position) LIKE LOWER(CONCAT('%', :keyword, '%')))
              AND (:siteName IS NULL OR j.siteName = :siteName)
              AND NOT EXISTS (
                  SELECT b.id FROM com.scraper.platform.model.CompanyBlacklist b
                  WHERE b.accountId = :accountId AND b.matchType = :exactType
                    AND LOWER(REPLACE(REPLACE(REPLACE(REPLACE(j.company, ' ', ''), '(주)', ''), '㈜', ''), '주식회사', ''))
                        = b.companyNameNormalized)
              AND NOT EXISTS (
                  SELECT b2.id FROM com.scraper.platform.model.CompanyBlacklist b2
                  WHERE b2.accountId = :accountId AND b2.matchType = :containsType
                    AND LOWER(REPLACE(REPLACE(REPLACE(REPLACE(j.company, ' ', ''), '(주)', ''), '㈜', ''), '주식회사', ''))
                        LIKE CONCAT('%', b2.companyNameNormalized, '%'))
            """)
    Page<JobPosting> searchRecent(@Param("accountId") Long accountId,
                                  @Param("keyword") String keyword,
                                  @Param("siteName") String siteName,
                                  @Param("exactType") com.scraper.platform.model.CompanyBlacklist.MatchType exactType,
                                  @Param("containsType") com.scraper.platform.model.CompanyBlacklist.MatchType containsType,
                                  Pageable pageable);

    @Query("SELECT DISTINCT j.crawledAt FROM JobPosting j WHERE j.config.id = :configId ORDER BY j.crawledAt DESC")
    List<LocalDate> findDistinctDatesByConfigId(@Param("configId") Long configId);

    @Query("SELECT j.crawledAt, j.siteName, COUNT(j) FROM JobPosting j WHERE j.config.id = :configId GROUP BY j.crawledAt, j.siteName ORDER BY j.crawledAt DESC")
    List<Object[]> countByConfigIdGroupedByDateAndSite(@Param("configId") Long configId);

    @Query("SELECT j.crawledAt, COUNT(j) FROM JobPosting j WHERE j.config.id = :configId GROUP BY j.crawledAt ORDER BY j.crawledAt DESC")
    List<Object[]> countByConfigIdGroupedByDate(@Param("configId") Long configId);

    List<JobPosting> findByConfigId(Long configId, Sort sort);

    List<JobPosting> findByConfigIdAndSiteName(Long configId, String siteName, Sort sort);

    List<JobPosting> findByConfigIdAndCrawledAt(Long configId, LocalDate crawledAt, Sort sort);

    List<JobPosting> findByConfigIdAndSiteNameAndCrawledAt(Long configId, String siteName, LocalDate crawledAt, Sort sort);

    Page<JobPosting> findByConfigIdAndCrawlLogIdIn(Long configId, List<Long> crawlLogIds, Pageable pageable);

    Page<JobPosting> findByConfigIdAndSiteNameAndCrawlLogIdIn(Long configId, String siteName, List<Long> crawlLogIds, Pageable pageable);

    List<JobPosting> findByConfigIdAndCrawlLogIdIn(Long configId, List<Long> crawlLogIds, Sort sort);

    List<JobPosting> findByConfigIdAndSiteNameAndCrawlLogIdIn(Long configId, String siteName, List<Long> crawlLogIds, Sort sort);

    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.transaction.annotation.Transactional
    void deleteByConfigId(Long configId);

    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.transaction.annotation.Transactional
    @Query("UPDATE JobPosting j SET j.crawlLogId = NULL WHERE j.crawlLogId = :crawlLogId")
    void nullifyCrawlLogId(@Param("crawlLogId") Long crawlLogId);

    /**
     * 정규화 회사명별 저장 공고 수. CompanyBlacklistService.normalize 와 동일한 규칙을
     * SQL 로 근사한다 (소문자+공백 제거+법인 표기 제거). 차단 키워드별 숨김 공고 수 표시에 사용.
     * 탭 공백 등 희귀 케이스는 근사 오차 가능.
     *
     * @param keywords 정규화 회사명 목록 (빈 목록이면 호출 금지 — IN () 무효)
     * @return [정규화명, 개수] 행 목록
     */
    @Query(value = """
            SELECT LOWER(REPLACE(REPLACE(REPLACE(REPLACE(company, ' ', ''), '(주)', ''), '㈜', ''), '주식회사', '')) AS norm,
                   COUNT(*) AS cnt
            FROM job_postings
            WHERE LOWER(REPLACE(REPLACE(REPLACE(REPLACE(company, ' ', ''), '(주)', ''), '㈜', ''), '주식회사', '')) IN :keywords
            GROUP BY norm
            """, nativeQuery = true)
    List<Object[]> countByNormalizedCompanyIn(@Param("keywords") List<String> keywords);

    /**
     * 정규화 회사명을 포함하는 저장 공고 수 (부분일치 차단 키워드의 숨김 공고 수, 설계 036).
     * 정규화 근사·와일드카드(`%`/`_`) 희귀 오차는 countByNormalizedCompanyIn와 동일 규칙.
     *
     * @param keyword 정규화 키워드
     * @return 매칭 공고 수
     */
    @Query(value = """
            SELECT COUNT(*) FROM job_postings
            WHERE LOWER(REPLACE(REPLACE(REPLACE(REPLACE(company, ' ', ''), '(주)', ''), '㈜', ''), '주식회사', '')) LIKE CONCAT('%', :keyword, '%')
            """, nativeQuery = true)
    long countByNormalizedCompanyContaining(@Param("keyword") String keyword);

    /**
     * 정규화 회사명의 최근 저장 공고. 슬라이드 보기 탭의 관련 공고 섹션용 (LIMIT 필수).
     * 수천 건 규모에서 수십ms — 슬라이드 열 때 1회만 실행된다.
     *
     * @param normalized 정규화 회사명
     * @param pageable 0페이지 + size 상한 (crawled_at 내림차순 권장)
     * @return 최근 공고 (최대 size건)
     */
    @Query(value = """
            SELECT * FROM job_postings
            WHERE LOWER(REPLACE(REPLACE(REPLACE(REPLACE(company, ' ', ''), '(주)', ''), '㈜', ''), '주식회사', '')) = :normalized
            ORDER BY crawled_at DESC
            """, nativeQuery = true)
    List<JobPosting> findRecentByNormalizedCompany(@Param("normalized") String normalized, Pageable pageable);

    /**
     * 수집된 회사명 자동완성. 회사 추가 모달에서 뷰어 수집 회사 검색용.
     * DISTINCT + LIMIT 8 — 수천 건 규모에서 수십ms (디바운스 호출).
     * 규모 증가 시 company 컬럼 인덱스 추가 고려.
     *
     * @param q 부분 검색어
     * @param pageable 0페이지 + size 상한
     * @return 중복 제거된 회사명 (원문)
     */
    @Query("SELECT DISTINCT j.company FROM JobPosting j WHERE LOWER(j.company) LIKE LOWER(CONCAT('%', :q, '%'))")
    List<String> findDistinctCompanyByCompanyContainingIgnoreCase(@Param("q") String q, Pageable pageable);
}
