package com.scraper.platform.repository;

import com.scraper.platform.model.SiteDefinition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SiteDefinitionRepository extends JpaRepository<SiteDefinition, Long> {
    Optional<SiteDefinition> findBySiteName(String siteName);
    List<SiteDefinition> findByIsEnabledTrue();
    boolean existsBySiteName(String siteName);
}
