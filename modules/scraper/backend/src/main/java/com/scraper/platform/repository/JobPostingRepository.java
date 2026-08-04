package com.scraper.platform.repository;

import com.scraper.platform.model.JobPosting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
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
public interface JobPostingRepository extends JpaRepository<JobPosting, Long>, JpaSpecificationExecutor<JobPosting> {

    /**
     * 특정 설정의 공고 조회 (날짜순)
     */
    List<JobPosting> findByConfigIdOrderByCrawledAtDescCreatedAtDesc(Long configId);

    /**
     * 특정 설정 + 날짜의 공고 조회
     */
    List<JobPosting> findByConfigIdAndCrawledAt(Long configId, LocalDate crawledAt);

    /**
     * 특정 설정 + 사이트 + 날짜의 공고 조회
     */
    List<JobPosting> findByConfigIdAndSiteNameAndCrawledAt(Long configId, String siteName, LocalDate crawledAt);

    /**
     * 중복 체크: 특정 설정에서 dedup_key 존재 여부
     */
    boolean existsByConfigIdAndDedupKey(Long configId, String dedupKey);

    /**
     * 중복 체크: 특정 설정에서 특정 기간 내 dedup_key 존재 여부
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
}
