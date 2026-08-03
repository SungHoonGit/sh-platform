package com.scraper.platform.service;

import com.scraper.platform.model.CrawlConfig;
import com.scraper.platform.model.CrawlSiteConfig;
import com.scraper.platform.model.SiteDefinition;
import com.scraper.platform.repository.CrawlConfigRepository;
import com.scraper.platform.repository.CrawlSiteConfigRepository;
import com.scraper.platform.repository.SiteDefinitionRepository;
import com.shplatform.common.exception.BusinessException;
import com.shplatform.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CrawlSiteConfigService {

    private final CrawlSiteConfigRepository crawlSiteConfigRepository;
    private final CrawlConfigRepository crawlConfigRepository;
    private final SiteDefinitionRepository siteDefinitionRepository;

    public List<CrawlSiteConfig> getConfigSiteConfigs(Long configId) {
        return crawlSiteConfigRepository.findByConfigId(configId);
    }

    public List<CrawlSiteConfig> getEnabledSiteConfigs(Long configId) {
        return crawlSiteConfigRepository.findByConfigIdAndIsEnabledTrue(configId);
    }

    public CrawlSiteConfig getSiteConfig(Long configId, Long siteDefinitionId) {
        return crawlSiteConfigRepository.findByConfigIdAndSiteDefinitionId(configId, siteDefinitionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
    }

    @Transactional
    public CrawlSiteConfig createOrUpdateSiteConfig(Long configId, Long siteDefinitionId, CrawlSiteConfig siteConfig) {
        CrawlConfig config = crawlConfigRepository.findById(configId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));

        SiteDefinition site = siteDefinitionRepository.findById(siteDefinitionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));

        CrawlSiteConfig existing = crawlSiteConfigRepository
                .findByConfigIdAndSiteDefinitionId(configId, siteDefinitionId)
                .orElse(null);

        if (existing != null) {
            existing.setIsEnabled(siteConfig.getIsEnabled());
            existing.setParamValues(siteConfig.getParamValues());
            return crawlSiteConfigRepository.save(existing);
        } else {
            siteConfig.setConfig(config);
            siteConfig.setSiteDefinition(site);
            return crawlSiteConfigRepository.save(siteConfig);
        }
    }

    @Transactional
    public void deleteSiteConfig(Long configId, Long siteDefinitionId) {
        CrawlSiteConfig siteConfig = crawlSiteConfigRepository
                .findByConfigIdAndSiteDefinitionId(configId, siteDefinitionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        crawlSiteConfigRepository.delete(siteConfig);
    }
}
