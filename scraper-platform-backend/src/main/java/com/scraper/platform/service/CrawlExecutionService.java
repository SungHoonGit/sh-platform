package com.scraper.platform.service;

import com.scraper.platform.model.*;
import com.scraper.platform.repository.*;
import com.shplatform.common.scheduling.ScheduleLog;
import com.shplatform.common.scheduling.ScheduleService;
import com.shplatform.common.notification.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CrawlExecutionService {

    private final CrawlConfigRepository crawlConfigRepository;
    private final CrawlSiteConfigRepository crawlSiteConfigRepository;
    private final CrawlDataRepository crawlDataRepository;
    private final CrawlLogRepository crawlLogRepository;
    private final ScheduleService scheduleService;
    private final NotificationService notificationService;

    @Scheduled(cron = "${scraper.schedule.cron:0 9 * * *}")
    public void executeScheduledCrawls() {
        log.info("Starting scheduled crawls");
        
        List<CrawlConfig> activeConfigs = crawlConfigRepository.findByIsActiveTrue();
        
        for (CrawlConfig config : activeConfigs) {
            try {
                executeCrawl(config);
            } catch (Exception e) {
                log.error("Failed to execute crawl for config: {}", config.getName(), e);
            }
        }
    }

    public void executeCrawl(CrawlConfig config) {
        log.info("Executing crawl for config: {}", config.getName());
        
        ScheduleLog scheduleLog = scheduleService.startLog(config.getId());
        
        int total = 0;
        int success = 0;
        int error = 0;
        
        List<CrawlSiteConfig> siteConfigs = crawlSiteConfigRepository
            .findByConfigIdAndIsEnabledTrue(config.getId());
        
        for (CrawlSiteConfig siteConfig : siteConfigs) {
            try {
                executeSiteCrawl(siteConfig);
                success++;
                total++;
            } catch (Exception e) {
                log.error("Failed to crawl site: {}", 
                    siteConfig.getSiteDefinition().getSiteName(), e);
                error++;
                total++;
            }
        }
        
        scheduleService.updateCounts(scheduleLog.getId(), total, success, error);
        scheduleService.completeLog(scheduleLog.getId(), error == 0, null);
        
        if (success > 0) {
            notificationService.sendNotification(
                "scraper", 
                "new_jobs_found", 
                String.format("Config '%s': %d new jobs found", config.getName(), success)
            );
        }
        
        if (error > 0) {
            notificationService.sendNotification(
                "scraper", 
                "crawl_failed", 
                String.format("Config '%s': %d/%d sites failed", config.getName(), error, total)
            );
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public CrawlData executeSiteCrawl(CrawlSiteConfig siteConfig) {
        SiteDefinition site = siteConfig.getSiteDefinition();
        log.info("Crawling site: {}", site.getSiteName());
        
        String siteName = site.getSiteName();
        String paramValues = siteConfig.getParamValues();
        
        String jobTitle = String.format("[%s] %s 채용공고", site.getDisplayName(), paramValues);
        String fileName = String.format("%s_%s_%d.md", siteName, paramValues, System.currentTimeMillis());
        String filePath = String.format("/data/scraper/%s/%s", siteName, fileName);
        
        CrawlData crawlData = CrawlData.builder()
            .config(siteConfig.getConfig())
            .title(jobTitle)
            .fileName(fileName)
            .filePath(filePath)
            .sourceSite(siteName)
            .sourceUrl(site.getBaseUrl())
            .crawledAt(LocalDateTime.now())
            .build();
        
        CrawlData saved = crawlDataRepository.save(crawlData);
        
        CrawlLog crawlLog = CrawlLog.builder()
            .config(siteConfig.getConfig())
            .siteDefinition(site)
            .status(CrawlLog.CrawlStatus.SUCCESS)
            .totalCount(1)
            .newCount(1)
            .build();
        
        crawlLogRepository.save(crawlLog);
        
        return saved;
    }
}
