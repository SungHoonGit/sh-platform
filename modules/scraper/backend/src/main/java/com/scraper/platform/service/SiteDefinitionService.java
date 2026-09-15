package com.scraper.platform.service;

import com.scraper.platform.model.SiteDefinition;
import com.scraper.platform.model.SiteParameterDefinition;
import com.scraper.platform.repository.SiteDefinitionRepository;
import com.scraper.platform.repository.SiteParameterDefinitionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SiteDefinitionService {

    private final SiteDefinitionRepository siteDefinitionRepository;
    private final SiteParameterDefinitionRepository siteParameterDefinitionRepository;

    public List<SiteDefinition> getAllSites() {
        return siteDefinitionRepository.findAllByOrderByDisplayOrderAsc();
    }

    public List<SiteDefinition> getEnabledSites() {
        return siteDefinitionRepository.findByIsEnabledTrueOrderByDisplayOrderAsc();
    }

    public SiteDefinition getSiteBySiteName(String siteName) {
        return siteDefinitionRepository.findBySiteName(siteName)
                .orElseThrow(() -> new RuntimeException("Site not found: " + siteName));
    }

    /**
     * 사이트 코드로 표시명(displayName)을 조회한다. 없으면 코드를 그대로 반환한다.
     *
     * @param siteName 사이트 코드
     * @return 표시명
     */
    public String getDisplayName(String siteName) {
        return siteDefinitionRepository.findBySiteName(siteName)
                .map(SiteDefinition::getDisplayName)
                .orElse(siteName);
    }

    public SiteDefinition getSiteById(Long id) {
        return siteDefinitionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Site not found: " + id));
    }

    public List<SiteParameterDefinition> getSiteParameters(Long siteDefinitionId) {
        return siteParameterDefinitionRepository.findBySiteDefinitionIdOrderByDisplayOrder(siteDefinitionId);
    }

    public List<SiteParameterDefinition> getSiteParametersBySiteName(String siteName) {
        return siteParameterDefinitionRepository.findBySiteDefinitionSiteNameOrderByDisplayOrder(siteName);
    }

    @Transactional
    public SiteDefinition createSite(SiteDefinition site) {
        if (siteDefinitionRepository.existsBySiteName(site.getSiteName())) {
            throw new RuntimeException("Duplicate site name: " + site.getSiteName());
        }
        return siteDefinitionRepository.save(site);
    }

    @Transactional
    public SiteDefinition updateSite(Long id, SiteDefinition updatedSite) {
        SiteDefinition existing = siteDefinitionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Site not found: " + id));
        
        existing.setDisplayName(updatedSite.getDisplayName());
        existing.setBaseUrl(updatedSite.getBaseUrl());
        existing.setIsEnabled(updatedSite.getIsEnabled());
        existing.setDisplayOrder(updatedSite.getDisplayOrder());
        existing.setIcon(updatedSite.getIcon());
        existing.setColor(updatedSite.getColor());
        
        return siteDefinitionRepository.save(existing);
    }

    @Transactional
    public void deleteSite(Long id) {
        SiteDefinition site = siteDefinitionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Site not found: " + id));
        siteDefinitionRepository.delete(site);
    }
}
