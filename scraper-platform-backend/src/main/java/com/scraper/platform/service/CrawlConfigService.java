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

    public CrawlConfig getConfigById(Long id) {
        return crawlConfigRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Config not found: " + id));
    }

    public CrawlConfig getConfigByName(String name) {
        return crawlConfigRepository.findByName(name)
                .orElseThrow(() -> new RuntimeException("Config not found: " + name));
    }

    public List<CrawlConfig> getActiveConfigs() {
        return crawlConfigRepository.findByIsActiveTrue();
    }

    public List<CrawlConfig> getActiveConfigsWithSiteConfigs() {
        return crawlConfigRepository.findAllActiveWithSiteConfigs();
    }

    @Transactional
    public CrawlConfig createConfig(CrawlConfig config) {
        if (crawlConfigRepository.existsByName(config.getName())) {
            throw new RuntimeException("Duplicate config name: " + config.getName());
        }
        return crawlConfigRepository.save(config);
    }

    @Transactional
    public CrawlConfig updateConfig(Long id, CrawlConfig updatedConfig) {
        CrawlConfig existing = crawlConfigRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Config not found: " + id));
        
        existing.setName(updatedConfig.getName());
        existing.setDescription(updatedConfig.getDescription());
        existing.setSchedule(updatedConfig.getSchedule());
        existing.setRetentionDays(updatedConfig.getRetentionDays());
        existing.setIsActive(updatedConfig.getIsActive());
        
        return crawlConfigRepository.save(existing);
    }

    @Transactional
    public void deleteConfig(Long id) {
        CrawlConfig config = crawlConfigRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Config not found: " + id));
        crawlConfigRepository.delete(config);
    }
}
