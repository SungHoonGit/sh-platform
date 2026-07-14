package com.scraper.platform.repository;

import com.scraper.platform.model.SiteParameterDefinition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SiteParameterDefinitionRepository extends JpaRepository<SiteParameterDefinition, Long> {
    List<SiteParameterDefinition> findBySiteDefinitionIdOrderByDisplayOrder(Long siteDefinitionId);
    List<SiteParameterDefinition> findBySiteDefinitionSiteNameOrderByDisplayOrder(String siteName);
}
