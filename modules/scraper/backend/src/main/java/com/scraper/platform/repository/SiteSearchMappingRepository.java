package com.scraper.platform.repository;

import com.scraper.platform.model.SiteSearchMapping;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SiteSearchMappingRepository extends JpaRepository<SiteSearchMapping, Long> {

    /**
     * 사이트명으로 활성화된 매핑 목록을 표시 순서대로 조회한다.
     */
    List<SiteSearchMapping> findBySiteDefinition_SiteNameAndIsEnabledTrueOrderByDisplayOrder(String siteName);

    /**
     * 사이트 ID로 활성화된 매핑 목록을 표시 순서대로 조회한다.
     */
    List<SiteSearchMapping> findBySiteDefinitionIdAndIsEnabledTrueOrderByDisplayOrder(Long siteDefinitionId);
}
