package com.scraper.platform.service;

import com.scraper.platform.crawler.CrawlerFactory;
import com.scraper.platform.crawler.SiteCrawler;
import com.scraper.platform.model.*;
import com.scraper.platform.repository.*;
import com.shplatform.common.notification.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;

import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@Service
@RequiredArgsConstructor
public class CrawlExecutionService {

    private static final int DEDUP_LOOKBACK_DAYS = 7;
    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private final CrawlConfigRepository crawlConfigRepository;
    private final CrawlSiteConfigRepository crawlSiteConfigRepository;
    private final CrawlDataRepository crawlDataRepository;
    private final CrawlLogRepository crawlLogRepository;
    private final JobPostingRepository jobPostingRepository;
    private final NotificationService notificationService;
    private final CrawlerFactory crawlerFactory;
    private final CrawlProgressBroadcaster progressBroadcaster;

    private final Map<Long, LocalDateTime> lastScheduledRun = new HashMap<>();

    @Scheduled(fixedDelay = 60_000)
    public void checkAndExecuteScheduledCrawls() {
        LocalDateTime now = LocalDateTime.now();
        List<CrawlConfig> activeConfigs = crawlConfigRepository.findByIsActiveTrue();

        for (CrawlConfig config : activeConfigs) {
            if (!config.getIsActive()) continue;

            String schedule = config.getSchedule();
            if (schedule == null || schedule.isBlank()) continue;

            if (shouldRun(schedule, now, config.getId())) {
                log.info("Scheduled crawl triggered for config: {} (id: {})", config.getName(), config.getId());
                try {
                    executeCrawl(config);
                } catch (Exception e) {
                    log.error("Failed to execute scheduled crawl for config: {}", config.getName(), e);
                }
            }
        }
    }

    private boolean shouldRun(String schedule, LocalDateTime now, Long configId) {
        String[] cronLines = schedule.split("\n");
        for (String cron5Field : cronLines) {
            String trimmed = cron5Field.trim();
            if (trimmed.isEmpty()) continue;
            if (evaluateCron(trimmed, now, configId)) {
                return true;
            }
        }
        return false;
    }

    private boolean evaluateCron(String cron5Field, LocalDateTime now, Long configId) {
        try {
            String cron6 = "0 " + cron5Field;
            CronExpression expr = CronExpression.parse(cron6);
            LocalDateTime lastRun = lastScheduledRun.get(configId);
            LocalDateTime nextRun = expr.next(lastRun != null ? lastRun : now.minusMinutes(2));
            if (nextRun == null) return false;
            boolean should = !now.isBefore(nextRun) && (lastRun == null || nextRun.isAfter(lastRun));
            if (should) {
                lastScheduledRun.put(configId, now);
            }
            return should;
        } catch (Exception e) {
            log.warn("Invalid cron expression '{}' for config id {}: {}", cron5Field, configId, e.getMessage());
            return false;
        }
    }

    @Transactional
    public void executeCrawl(CrawlConfig config) {
        log.info("Executing crawl for config: {} (id: {})", config.getName(), config.getId());

        int total = 0;
        int success = 0;
        int error = 0;
        int totalJobs = 0;
        int newJobs = 0;
        int dupJobs = 0;
        String keyword = "전체";
        StringBuilder combinedMd = new StringBuilder();

        // DB 기반 중복 체크: 최근 N일간의 dedup_key 수집
        LocalDate dedupSince = LocalDate.now().minusDays(DEDUP_LOOKBACK_DAYS);
        Set<String> existingDedupKeys = jobPostingRepository.findDedupKeysSince(config.getId(), dedupSince);
        log.info("Dedup: found {} existing dedup keys since {}", existingDedupKeys.size(), dedupSince);

        List<CrawlSiteConfig> siteConfigs = crawlSiteConfigRepository
                .findEnabledWithSite(config.getId());

        progressBroadcaster.sendStart(config.getId(), config.getName(), siteConfigs.size());

        for (int i = 0; i < siteConfigs.size(); i++) {
            CrawlSiteConfig siteConfig = siteConfigs.get(i);
            String siteName = siteConfig.getSiteDefinition().getSiteName();
            progressBroadcaster.sendSiteStart(config.getId(), siteName, i + 1, siteConfigs.size());

            try {
                keyword = extractKeyword(siteConfig.getParamValues());
                List<Map<String, String>> allJobs = executeSiteCrawlJobs(siteConfig);
                totalJobs += allJobs.size();

                // 중복 체크 + DB 저장
                List<Map<String, String>> newJobList = new ArrayList<>();
                for (Map<String, String> job : allJobs) {
                    String company = job.getOrDefault("company", "");
                    String position = job.getOrDefault("position", job.getOrDefault("title", ""));
                    String location = job.getOrDefault("location", "");
                    String url = job.getOrDefault("url", "");

                    String dedupKey = JobPosting.generateDedupKey(company, position, location, siteName);

                    if (existingDedupKeys.contains(dedupKey)) {
                        dupJobs++;
                        continue;
                    }

                    // 새 공고 -> DB 저장
                    JobPosting posting = JobPosting.builder()
                            .config(config)
                            .siteName(siteName)
                            .url(url)
                            .company(company)
                            .position(position)
                            .career(job.getOrDefault("career", ""))
                            .tech(job.getOrDefault("tech", ""))
                            .location(location)
                            .deadline(job.getOrDefault("deadline", ""))
                            .dedupKey(dedupKey)
                            .crawledAt(LocalDate.now())
                            .build();

                    try {
                        jobPostingRepository.save(posting);
                        existingDedupKeys.add(dedupKey);
                        newJobList.add(job);
                    } catch (Exception e) {
                        // UK 제약 조건 위반 = 동시 크롤링 중복
                        if (e.getMessage() != null && e.getMessage().contains("uk_job_postings_dedup")) {
                            dupJobs++;
                            log.debug("Duplicate job detected by DB constraint: {}", dedupKey);
                        } else {
                            log.error("Failed to save job posting: {}", company + " " + position, e);
                            dupJobs++;
                        }
                    }
                }

                newJobs += newJobList.size();

                // MD 파일 생성 (부가 출력)
                combinedMd.append(crawlerFactory.getCrawler(siteConfig.getSiteDefinition().getSiteName())
                        .buildMdSection(newJobList, siteConfig.getSiteDefinition().getDisplayName()));

                saveCrawlData(siteConfig, config, newJobList.size());
                progressBroadcaster.sendSiteComplete(config.getId(), siteName, newJobList.size(), true, null);
                success++;
                total++;
            } catch (Exception e) {
                log.error("Failed to crawl site: {}", siteName, e);
                progressBroadcaster.sendSiteComplete(config.getId(), siteName, 0, false, e.getMessage());
                error++;
                total++;
            }
        }

        // 일별 통합 MD 파일 저장 (부가 출력)
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

        progressBroadcaster.sendCrawlComplete(config.getId(), total, success, totalJobs, newJobs, dupJobs);
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
