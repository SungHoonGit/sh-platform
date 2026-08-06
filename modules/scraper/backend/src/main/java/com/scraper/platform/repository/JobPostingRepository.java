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

    @Query("SELECT j.dedupKey FROM JobPosting j WHERE j.config.id = :configId AND j.crawledAt >= :sinceDate")
    Set<String> findDedupKeysSince(@Param("configId") Long configId, @Param("sinceDate") LocalDate sinceDate);

    long countByConfigId(Long configId);

    long countByConfigIdAndCrawledAt(Long configId, LocalDate crawledAt);

    @Query("SELECT MAX(j.crawledAt) FROM JobPosting j WHERE j.config.id = :configId")
    LocalDate findLastCrawledAt(@Param("configId") Long configId);

    @Query("SELECT DISTINCT j.crawledAt FROM JobPosting j WHERE j.config.id = :configId ORDER BY j.crawledAt DESC")
    List<LocalDate> findDistinctDatesByConfigId(@Param("configId") Long configId);

    List<JobPosting> findByConfigId(Long configId, Sort sort);

    List<JobPosting> findByConfigIdAndSiteName(Long configId, String siteName, Sort sort);

    List<JobPosting> findByConfigIdAndCrawledAt(Long configId, LocalDate crawledAt, Sort sort);

    List<JobPosting> findByConfigIdAndSiteNameAndCrawledAt(Long configId, String siteName, LocalDate crawledAt, Sort sort);
}
