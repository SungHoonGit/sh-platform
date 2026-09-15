package com.scraper.platform.repository;

import com.scraper.platform.model.SiteSearchMapping;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

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

    /**
     * 사이트명과 표준 키(standard_key)로 활성화된 매핑 하나를 조회한다.
     */
    Optional<SiteSearchMapping> findBySiteDefinition_SiteNameAndStandardKeyAndIsEnabledTrue(String siteName, String standardKey);

    /**
     * 전체 매핑을 사이트 표시명·표시 순서 순으로 조회한다.
     */
    List<SiteSearchMapping> findAllByOrderBySiteDefinition_DisplayNameAscDisplayOrderAscIdAsc();

    /**
     * 사이트별 매핑 목록을 표시 순서로 조회한다.
     */
    List<SiteSearchMapping> findBySiteDefinitionSiteNameOrderByDisplayOrderAscIdAsc(String siteName);

    /**
     * 사이트·표준 키 조합의 중복 여부를 확인한다.
     */
    boolean existsBySiteDefinitionIdAndStandardKey(Long siteDefinitionId, String standardKey);
}
