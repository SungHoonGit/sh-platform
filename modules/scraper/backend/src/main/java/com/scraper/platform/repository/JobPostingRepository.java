package com.scraper.platform.repository;

import com.scraper.platform.model.JobPosting;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

/**
 * 채용공고 Repository.
 * DB 기반 중복 체크 및 조회용.
 */
@Repository
public interface JobPostingRepository extends JpaRepository<JobPosting, Long> {

    /**
     * 특정 설정의 공고 조회 (페이지네이션)
     */
    Page<JobPosting> findByConfigId(Long configId, Pageable pageable);

    /**
     * 특정 설정 + 사이트의 공고 조회
     */
    Page<JobPosting> findByConfigIdAndSiteName(Long configId, String siteName, Pageable pageable);

    /**
     * 특정 설정 + 날짜의 공고 조회
     */
    Page<JobPosting> findByConfigIdAndCrawledAt(Long configId, LocalDate crawledAt, Pageable pageable);

    /**
     * 특정 설정 + 사이트 + 날짜의 공고 조회
     */
    Page<JobPosting> findByConfigIdAndSiteNameAndCrawledAt(Long configId, String siteName, LocalDate crawledAt, Pageable pageable);

    /**
     * 특정 설정의 중복 체크용 dedup_key 수집 (최근 N일)
     */
    @Query("SELECT j.dedupKey FROM JobPosting j WHERE j.config.id = :configId AND j.crawledAt >= :sinceDate")
    Set<String> findDedupKeysSince(@Param("configId") Long configId, @Param("sinceDate") LocalDate sinceDate);

    /**
     * 특정 설정의 공고 수 조회
     */
    long countByConfigId(Long configId);

    /**
     * 특정 설정 + 날짜의 공고 수 조회
     */
    long countByConfigIdAndCrawledAt(Long configId, LocalDate crawledAt);

    /**
     * 특정 설정의 최근 수집일 조회
     */
    @Query("SELECT MAX(j.crawledAt) FROM JobPosting j WHERE j.config.id = :configId")
    LocalDate findLastCrawledAt(@Param("configId") Long configId);

    /**
     * 특정 설정의 수집된 날짜 목록 조회
     */
    @Query("SELECT DISTINCT j.crawledAt FROM JobPosting j WHERE j.config.id = :configId ORDER BY j.crawledAt DESC")
    List<LocalDate> findDistinctDatesByConfigId(@Param("configId") Long configId);
}
