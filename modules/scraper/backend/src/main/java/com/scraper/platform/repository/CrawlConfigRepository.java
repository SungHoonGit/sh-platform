package com.scraper.platform.repository;

import com.scraper.platform.model.CrawlConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CrawlConfigRepository extends JpaRepository<CrawlConfig, Long> {
    Optional<CrawlConfig> findByIdAndAccountId(Long id, Long accountId);
    Optional<CrawlConfig> findByNameAndAccountId(String name, Long accountId);
    List<CrawlConfig> findByAccountId(Long accountId);
    List<CrawlConfig> findByAccountIdAndIsActiveTrue(Long accountId);
    boolean existsByAccountIdAndName(Long accountId, String name);

    @Query("SELECT c FROM CrawlConfig c LEFT JOIN FETCH c.siteConfigs WHERE c.id = :id AND c.accountId = :accountId")
    Optional<CrawlConfig> findByIdWithSiteConfigs(@Param("id") Long id, @Param("accountId") Long accountId);

    @Query("SELECT c FROM CrawlConfig c LEFT JOIN FETCH c.siteConfigs WHERE c.isActive = true AND c.accountId = :accountId")
    List<CrawlConfig> findAllActiveWithSiteConfigs(@Param("accountId") Long accountId);

    @Query("SELECT c FROM CrawlConfig c LEFT JOIN FETCH c.siteConfigs WHERE c.isActive = true")
    List<CrawlConfig> findAllActiveWithSiteConfigs();

    List<CrawlConfig> findByIsActiveTrue();
}
