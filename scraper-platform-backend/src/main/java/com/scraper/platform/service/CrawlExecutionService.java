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
import java.nio.file.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@Service
@RequiredArgsConstructor
public class CrawlExecutionService {

    private static final int DEDUP_LOOKBACK_DAYS = 3;

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
        int totalJobs = 0;
        int newJobs = 0;
        int dupJobs = 0;
        String keyword = "전체";
        StringBuilder combinedMd = new StringBuilder();

        // Dedup: 이전 MD 파일에서 URL 수집
        Set<String> existingUrls = collectExistingUrls(config.getLocalPath());
        log.info("Dedup: found {} existing URLs from previous {} days", existingUrls.size(), DEDUP_LOOKBACK_DAYS);

        List<CrawlSiteConfig> siteConfigs = crawlSiteConfigRepository
                .findByConfigIdAndIsEnabledTrue(config.getId());

        for (CrawlSiteConfig siteConfig : siteConfigs) {
            try {
                keyword = extractKeyword(siteConfig.getParamValues());
                List<Map<String, String>> allJobs = executeSiteCrawlJobs(siteConfig);
                totalJobs += allJobs.size();

                // Dedup 필터링
                List<Map<String, String>> newJobList = new ArrayList<>();
                for (Map<String, String> job : allJobs) {
                    String url = job.getOrDefault("url", "");
                    if (!url.isEmpty() && existingUrls.contains(normalizeUrl(url))) {
                        dupJobs++;
                    } else {
                        newJobList.add(job);
                        if (!url.isEmpty()) {
                            existingUrls.add(normalizeUrl(url));
                        }
                    }
                }
                newJobs += newJobList.size();

                combinedMd.append(crawlerFactory.getCrawler(siteConfig.getSiteDefinition().getSiteName())
                        .buildMdSection(newJobList, siteConfig.getSiteDefinition().getDisplayName()));

                saveCrawlData(siteConfig, config, newJobList.size());
                success++;
                total++;
            } catch (Exception e) {
                log.error("Failed to crawl site: {}", siteConfig.getSiteDefinition().getSiteName(), e);
                error++;
                total++;
            }
        }

        // 일별 통합 MD 파일 저장
        try {
            String fileName = LocalDate.now() + ".md";
            String dirPath = config.getLocalPath();
            String filePath = String.format("%s/%s", dirPath, fileName);
            String timeStr = java.time.LocalTime.now().withNano(0).toString().substring(0, 5);
            String header;
            if (dupJobs > 0) {
                header = String.format("# %s %s 채용공고\n\n> 총 %d건 (%s 기준) | 신규 %d건, 중복 %d건 제외\n\n",
                        LocalDate.now(), keyword, newJobs, timeStr, newJobs, dupJobs);
            } else {
                header = String.format("# %s %s 채용공고\n\n> 총 %d건 (%s 기준)\n\n",
                        LocalDate.now(), keyword, newJobs, timeStr);
            }
            saveFile(dirPath, filePath, header + combinedMd);
        } catch (IOException e) {
            log.error("Failed to save combined MD file for config: {}", config.getName(), e);
        }

        scheduleService.updateCounts(scheduleLog.getId(), total, success, error);
        scheduleService.completeLog(scheduleLog.getId(), error == 0, null);

        if (success > 0) {
            String msg = dupJobs > 0
                    ? String.format("Config '%s': %d sites, %d new jobs (dedup: %d removed)", config.getName(), success, newJobs, dupJobs)
                    : String.format("Config '%s': %d sites, %d jobs crawled", config.getName(), success, newJobs);
            notificationService.sendNotification("scraper", "new_jobs_found", msg);
        }

        if (error > 0) {
            notificationService.sendNotification("scraper", "crawl_failed",
                    String.format("Config '%s': %d/%d sites failed", config.getName(), error, total));
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public List<Map<String, String>> executeSiteCrawlJobs(CrawlSiteConfig siteConfig) throws Exception {
        SiteDefinition site = siteConfig.getSiteDefinition();
        String siteName = site.getSiteName();
        log.info("Crawling site: {}", siteName);
        SiteCrawler crawler = crawlerFactory.getCrawler(siteName);
        if (crawler == null) {
            throw new UnsupportedOperationException("No crawler implemented for site: " + siteName);
        }
        List<Map<String, String>> jobs = crawler.search(siteConfig);
        log.info("Found {} jobs from {}", jobs.size(), siteName);
        return jobs;
    }

    private Set<String> collectExistingUrls(String dirPath) {
        Set<String> urls = new HashSet<>();
        Path dir = Paths.get(dirPath);
        if (!Files.exists(dir)) return urls;

        LocalDate today = LocalDate.now();
        Pattern urlPattern = Pattern.compile("\\(https?://[^)]+\\)");

        for (int i = 1; i <= DEDUP_LOOKBACK_DAYS; i++) {
            Path file = dir.resolve(today.minusDays(i) + ".md");
            if (!Files.exists(file)) continue;
            try (Stream<String> lines = Files.lines(file)) {
                lines.forEach(line -> {
                    Matcher m = urlPattern.matcher(line);
                    while (m.find()) {
                        String url = m.group(1).substring(1, m.group(1).length() - 1);
                        urls.add(normalizeUrl(url));
                    }
                });
            } catch (IOException e) {
                log.warn("Failed to read dedup file: {}", file, e);
            }
        }
        return urls;
    }

    private String normalizeUrl(String url) {
        if (url == null) return "";
        return url.trim()
                .replaceAll("&+$", "")
                .replaceAll("\\?$", "")
                .toLowerCase();
    }

    private void saveCrawlData(CrawlSiteConfig siteConfig, CrawlConfig config, int jobCount) {
        SiteDefinition site = siteConfig.getSiteDefinition();
        String keyword = extractKeyword(siteConfig.getParamValues());
        CrawlData crawlData = CrawlData.builder()
                .config(siteConfig.getConfig())
                .title(String.format("[%s] %s 채용공고", site.getDisplayName(), keyword))
                .fileName(LocalDate.now() + ".md")
                .filePath(config.getLocalPath() + "/" + LocalDate.now() + ".md")
                .sourceSite(site.getSiteName())
                .sourceUrl(site.getBaseUrl())
                .crawledAt(LocalDateTime.now())
                .build();
        crawlDataRepository.save(crawlData);

        CrawlLog crawlLog = CrawlLog.builder()
                .config(siteConfig.getConfig())
                .siteDefinition(site)
                .status(CrawlLog.CrawlStatus.SUCCESS)
                .totalCount(jobCount)
                .newCount(jobCount)
                .build();
        crawlLogRepository.save(crawlLog);
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
        if (paramValues == null || paramValues.isEmpty()) return "전체";
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            com.fasterxml.jackson.databind.JsonNode node = mapper.readTree(paramValues);
            return node.has("keyword") ? node.get("keyword").asText() : "전체";
        } catch (Exception e) {
            return "전체";
        }
    }
}
