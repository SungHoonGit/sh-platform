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
    Optional<CrawlConfig> findByName(String name);
    List<CrawlConfig> findByIsActiveTrue();
    boolean existsByName(String name);
    
    @Query("SELECT c FROM CrawlConfig c LEFT JOIN FETCH c.siteConfigs WHERE c.id = :id")
    Optional<CrawlConfig> findByIdWithSiteConfigs(@Param("id") Long id);
    
    @Query("SELECT c FROM CrawlConfig c LEFT JOIN FETCH c.siteConfigs WHERE c.isActive = true")
    List<CrawlConfig> findAllActiveWithSiteConfigs();
}
