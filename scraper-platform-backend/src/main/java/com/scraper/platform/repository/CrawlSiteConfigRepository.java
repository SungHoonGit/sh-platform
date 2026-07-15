package com.scraper.platform.repository;

import com.scraper.platform.model.CrawlSiteConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CrawlSiteConfigRepository extends JpaRepository<CrawlSiteConfig, Long> {
    List<CrawlSiteConfig> findByConfigId(Long configId);
    List<CrawlSiteConfig> findByConfigIdAndIsEnabledTrue(Long configId);
    Optional<CrawlSiteConfig> findByConfigIdAndSiteDefinitionId(Long configId, Long siteDefinitionId);

    @Query("SELECT s FROM CrawlSiteConfig s JOIN FETCH s.siteDefinition WHERE s.config.id = :configId AND s.isEnabled = true")
    List<CrawlSiteConfig> findEnabledWithSite(@Param("configId") Long configId);
}
