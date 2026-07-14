package com.scraper.platform.service;

import com.scraper.platform.model.CrawlConfig;
import com.scraper.platform.repository.CrawlConfigRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CrawlConfigService {

    private final CrawlConfigRepository crawlConfigRepository;

    public List<CrawlConfig> getAllConfigs() {
        return crawlConfigRepository.findAll();
    }

    public CrawlConfig getConfigByCategory(String category) {
        return crawlConfigRepository.findByCategory(category)
                .orElseThrow(() -> new RuntimeException("Config not found: " + category));
    }

    public List<CrawlConfig> getActiveConfigs() {
        return crawlConfigRepository.findByIsActiveTrue();
    }

    @Transactional
    public CrawlConfig createConfig(CrawlConfig config) {
        if (crawlConfigRepository.existsByCategory(config.getCategory())) {
            throw new RuntimeException("Duplicate category: " + config.getCategory());
        }
        return crawlConfigRepository.save(config);
    }

    @Transactional
    public CrawlConfig updateConfig(String category, CrawlConfig updatedConfig) {
        CrawlConfig existing = crawlConfigRepository.findByCategory(category)
                .orElseThrow(() -> new RuntimeException("Config not found: " + category));
        
        existing.setQuery(updatedConfig.getQuery());
        existing.setCareerLevel(updatedConfig.getCareerLevel());
        existing.setCareerFrom(updatedConfig.getCareerFrom());
        existing.setCareerTo(updatedConfig.getCareerTo());
        existing.setSites(updatedConfig.getSites());
        existing.setIncludeTitles(updatedConfig.getIncludeTitles());
        existing.setExcludeTitles(updatedConfig.getExcludeTitles());
        existing.setSchedule(updatedConfig.getSchedule());
        existing.setRetentionDays(updatedConfig.getRetentionDays());
        existing.setMaxPerSite(updatedConfig.getMaxPerSite());
        existing.setIsActive(updatedConfig.getIsActive());
        
        return crawlConfigRepository.save(existing);
    }

    @Transactional
    public void deleteConfig(String category) {
        CrawlConfig config = crawlConfigRepository.findByCategory(category)
                .orElseThrow(() -> new RuntimeException("Config not found: " + category));
        crawlConfigRepository.delete(config);
    }
}
