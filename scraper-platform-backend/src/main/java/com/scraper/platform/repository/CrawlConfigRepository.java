package com.scraper.platform.repository;

import com.scraper.platform.model.CrawlConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CrawlConfigRepository extends JpaRepository<CrawlConfig, Long> {
    Optional<CrawlConfig> findByCategory(String category);
    List<CrawlConfig> findByIsActiveTrue();
    boolean existsByCategory(String category);
}
