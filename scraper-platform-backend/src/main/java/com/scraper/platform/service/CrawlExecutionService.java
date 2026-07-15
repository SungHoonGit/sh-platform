package com.scraper.platform.service;

import com.scraper.platform.crawler.CrawlerFactory;
import com.scraper.platform.crawler.SiteCrawler;
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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

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
    private final CrawlerFactory crawlerFactory;



    @Scheduled(cron = "${scraper.schedule.cron:0 9 * * *}")
    public void executeScheduledCrawls() {
        log.info("Starting scheduled crawls at {}", LocalDateTime.now());
        
        List<CrawlConfig> activeConfigs = crawlConfigRepository.findByIsActiveTrue();
        
        for (CrawlConfig config : activeConfigs) {
            try {
                executeCrawl(config);
            } catch (Exception e) {
                log.error("Failed to execute crawl for config: {}", config.getName(), e);
            }
        }
        
        log.info("Completed scheduled crawls");
    }

    public void executeCrawl(CrawlConfig config) {
        log.info("Executing crawl for config: {} (id: {})", config.getName(), config.getId());
        
        ScheduleLog scheduleLog = scheduleService.startLog(config.getId());
        
        int total = 0;
        int success = 0;
        int error = 0;
        
        List<CrawlSiteConfig> siteConfigs = crawlSiteConfigRepository
                .findByConfigIdAndIsEnabledTrue(config.getId());
        
        for (CrawlSiteConfig siteConfig : siteConfigs) {
            try {
                executeSiteCrawl(siteConfig, config.getName());
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
                    String.format("Config '%s': %d sites crawled successfully", config.getName(), success)
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
    public CrawlData executeSiteCrawl(CrawlSiteConfig siteConfig, String configName) throws Exception {
        SiteDefinition site = siteConfig.getSiteDefinition();
        String siteName = site.getSiteName();
        
        log.info("Crawling site: {} for config: {}", siteName, configName);
        
        SiteCrawler crawler = crawlerFactory.getCrawler(siteName);
        if (crawler == null) {
            throw new UnsupportedOperationException("No crawler implemented for site: " + siteName);
        }
        
        // 검색 실행
        List<Map<String, String>> jobs = crawler.search(siteConfig);
        log.info("Found {} jobs from {}", jobs.size(), siteName);
        
        // MD 파일 생성
        String keyword = extractKeyword(siteConfig.getParamValues());
        String mdContent = crawler.buildMdContent(jobs, keyword, siteName);
        
        // 파일 저장 — crawl_config.local_path 사용
        String fileName = LocalDate.now() + ".md";
        String dirPath = siteConfig.getConfig().getLocalPath();
        String filePath = String.format("%s/%s", dirPath, fileName);
        
        saveFile(dirPath, filePath, mdContent);
        
        // CrawlData 저장
        CrawlData crawlData = CrawlData.builder()
                .config(siteConfig.getConfig())
                .title(String.format("[%s] %s 채용공고", site.getDisplayName(), keyword))
                .fileName(fileName)
                .filePath(filePath)
                .sourceSite(siteName)
                .sourceUrl(site.getBaseUrl())
                .crawledAt(LocalDateTime.now())
                .build();
        
        CrawlData saved = crawlDataRepository.save(crawlData);
        
        // CrawlLog 저장
        CrawlLog crawlLog = CrawlLog.builder()
                .config(siteConfig.getConfig())
                .siteDefinition(site)
                .status(CrawlLog.CrawlStatus.SUCCESS)
                .totalCount(jobs.size())
                .newCount(jobs.size())
                .build();
        
        crawlLogRepository.save(crawlLog);
        
        return saved;
    }

    private void saveFile(String dirPath, String filePath, String content) throws IOException {
        Path dir = Paths.get(dirPath);
        if (!Files.exists(dir)) {
            Files.createDirectories(dir);
        }
        
        Path file = Paths.get(filePath);
        Files.writeString(file, content);
        log.info("Saved MD file: {}", filePath);
    }

    private String extractKeyword(String paramValues) {
        if (paramValues == null || paramValues.isEmpty()) {
            return "전체";
        }
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            com.fasterxml.jackson.databind.JsonNode node = mapper.readTree(paramValues);
            return node.has("keyword") ? node.get("keyword").asText() : "전체";
        } catch (Exception e) {
            return "전체";
        }
    }
}
