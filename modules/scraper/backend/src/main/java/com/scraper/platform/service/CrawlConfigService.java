package com.scraper.platform.service;

import com.scraper.platform.model.CrawlConfig;
import com.scraper.platform.repository.*;
import com.shplatform.common.exception.BusinessException;
import com.shplatform.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CrawlConfigService {

    private static final String DATA_ROOT = "/home/ubuntu/data/scraper";

    private final CrawlConfigRepository crawlConfigRepository;
    private final JobPostingRepository jobPostingRepository;
    private final CrawlLogRepository crawlLogRepository;
    private final CrawlDataRepository crawlDataRepository;
    private final CrawlSiteConfigRepository crawlSiteConfigRepository;

    public List<CrawlConfig> getAllConfigs(Long accountId) {
        return crawlConfigRepository.findByAccountId(accountId);
    }

    public CrawlConfig getConfigById(Long id, Long accountId) {
        return crawlConfigRepository.findByIdAndAccountId(id, accountId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
    }

    public CrawlConfig getConfigByName(String name, Long accountId) {
        return crawlConfigRepository.findByNameAndAccountId(name, accountId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
    }

    public List<CrawlConfig> getActiveConfigs(Long accountId) {
        return crawlConfigRepository.findByAccountIdAndIsActiveTrue(accountId);
    }

    public List<CrawlConfig> getActiveConfigsWithSiteConfigs(Long accountId) {
        return crawlConfigRepository.findAllActiveWithSiteConfigs(accountId);
    }

    @Transactional
    public CrawlConfig createConfig(Long accountId, CrawlConfig config) {
        if (crawlConfigRepository.existsByAccountIdAndName(accountId, config.getName())) {
            throw new BusinessException(ErrorCode.DUPLICATE_NAME);
        }
        config.setAccountId(accountId);
        if (config.getLocalPath() == null || config.getLocalPath().isBlank()) {
            config.setLocalPath(buildLocalPath(accountId, config.getName()));
        }
        return crawlConfigRepository.save(config);
    }

    @Transactional
    public CrawlConfig updateConfig(Long id, Long accountId, CrawlConfig updatedConfig) {
        CrawlConfig existing = crawlConfigRepository.findByIdAndAccountId(id, accountId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));

        existing.setName(updatedConfig.getName());
        existing.setDescription(updatedConfig.getDescription());
        existing.setSchedule(updatedConfig.getSchedule());
        existing.setScheduleIcon(updatedConfig.getScheduleIcon());
        existing.setRetentionDays(updatedConfig.getRetentionDays());
        existing.setIsActive(updatedConfig.getIsActive());
        if (updatedConfig.getLocalPath() != null && !updatedConfig.getLocalPath().isBlank()) {
            existing.setLocalPath(updatedConfig.getLocalPath());
        }

        return crawlConfigRepository.save(existing);
    }

    @Transactional
    public void deleteConfig(Long id, Long accountId) {
        CrawlConfig config = crawlConfigRepository.findByIdAndAccountId(id, accountId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));

        Long configId = config.getId();

        // 자식 테이블부터 삭제 (FK 순서 준수)
        jobPostingRepository.deleteByConfigId(configId);
        crawlLogRepository.deleteByConfigId(configId);
        crawlDataRepository.deleteByConfigId(configId);
        crawlSiteConfigRepository.deleteByConfigId(configId);

        // 마지막으로 부모 삭제
        crawlConfigRepository.delete(config);
    }

    private String buildLocalPath(Long accountId, String name) {
        String safeName = name.trim().replaceAll("[^a-zA-Z0-9가-힣._-]", "-");
        return String.format("%s/%d/%s", DATA_ROOT, accountId, safeName);
    }
}
