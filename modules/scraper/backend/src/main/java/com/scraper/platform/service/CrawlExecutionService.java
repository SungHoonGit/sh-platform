package com.scraper.platform.service;

import com.scraper.platform.crawler.CrawlerFactory;
import com.scraper.platform.crawler.SiteCrawler;
import com.scraper.platform.model.*;
import com.scraper.platform.repository.*;
import com.shplatform.common.notification.EmailTemplate;
import com.shplatform.common.notification.NotificationService;
import com.shplatform.common.notification.WebPushService;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

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
    private final WebPushService webPushService;
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
                    executeCrawl(config, CrawlLog.CrawlSource.SCHEDULE);
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

    /**
     * 지정된 설정으로 크롤링을 실행한다.
     *
     * @param config 크롤링 설정
     * @param source 실행 출처 (MANUAL: 수동, SCHEDULE: 스케줄)
     */
    public void executeCrawl(CrawlConfig config, CrawlLog.CrawlSource source) {
        log.info("Executing crawl for config: {} (id: {}) source: {}", config.getName(), config.getId(), source);

        int total = 0;
        int success = 0;
        int error = 0;
        int totalJobs = 0;
        int newJobs = 0;
        int dupJobs = 0;
        String keyword = "전체";
        StringBuilder combinedMd = new StringBuilder();

        // DB 기반 중복 체크: 최근 N일간의 dedup_key 수집 (전역 — dedup_key는 config와 무관한 전역 유니크)
        LocalDate dedupSince = LocalDate.now().minusDays(DEDUP_LOOKBACK_DAYS);
        Set<String> existingDedupKeys = jobPostingRepository.findDedupKeysSince(dedupSince);
        log.info("Dedup: found {} existing dedup keys since {}", existingDedupKeys.size(), dedupSince);

        // 한 번의 크롤링 실행에 속한 사이트별 로그를 묶기 위한 배치 ID
        String batchId = UUID.randomUUID().toString();

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

                SiteDefinition site = siteConfig.getSiteDefinition();
                String searchCriteriaJson = extractSearchCriteria(siteConfigs);
                CrawlLog crawlLog = CrawlLog.builder()
                        .config(config)
                        .siteDefinition(site)
                        .status(CrawlLog.CrawlStatus.RUNNING)
                        .totalCount(0)
                        .newCount(0)
                        .searchCriteria(searchCriteriaJson)
                        .batchId(batchId)
                        .source(source)
                        .build();
                crawlLog = crawlLogRepository.save(crawlLog);
                Long crawlLogId = crawlLog.getId();

                // 각 사이트별 별도 트랜잭션으로 저장
                int[] result = saveJobPostings(config, siteConfig, allJobs, existingDedupKeys, crawlLogId);
                int saved = result[0];
                int dups = result[1];
                newJobs += saved;
                dupJobs += dups;

                // MD 파일 생성 (부가 출력)
                List<Map<String, String>> newJobList = allJobs.subList(0, Math.min(saved, allJobs.size()));
                combinedMd.append(crawlerFactory.getCrawler(siteConfig.getSiteDefinition().getSiteName())
                        .buildMdSection(newJobList, siteConfig.getSiteDefinition().getDisplayName()));

                saveCrawlLogComplete(crawlLogId, saved);
                progressBroadcaster.sendSiteComplete(config.getId(), siteName, saved, true, null);
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

        log.info("[EMAIL] Config check - emailNotification: {}, recipientEmail: {}", 
                config.getEmailNotification(), config.getRecipientEmail());
        
        if (success > 0 && Boolean.TRUE.equals(config.getEmailNotification())) {
            log.info("[EMAIL] Sending notification for config: {}, recipientEmail: {}", config.getName(), config.getRecipientEmail());
            // 최근 수집된 공고 조회 (최대 10건)
            LocalDateTime crawlStartTime = LocalDateTime.now().minusMinutes(30);
            Page<JobPosting> recentJobsPage = jobPostingRepository.findByConfigIdAndCreatedAtBetween(
                    config.getId(), crawlStartTime, LocalDateTime.now(),
                    PageRequest.of(0, 10, org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "createdAt")));
            List<JobPosting> recentJobs = recentJobsPage.getContent();

            String subject = "[SH Platform] 신규 채용공고 수집 완료";
            String html = buildJobAlertHtml(config.getName(), success, newJobs, dupJobs, recentJobs);
            notificationService.sendHtmlEmail(config.getRecipientEmail(), subject, html);
        }

        if (error > 0 && Boolean.TRUE.equals(config.getEmailNotification())) {
            notificationService.sendEmail(config.getRecipientEmail(),
                    String.format("Config '%s': %d/%d sites failed", config.getName(), error, total));
        }

        // 웹 푸쉬 발송
        if (success > 0 && Boolean.TRUE.equals(config.getPushNotification())) {
            String pushTitle = String.format("%s 수집 완료", config.getName());
            String pushBody = String.format("신규 %d건 수집", newJobs);
            if (dupJobs > 0) {
                pushBody += String.format(" (중복 %d건 제외)", dupJobs);
            }
            webPushService.sendPushToUser(config.getAccountId(), pushTitle, pushBody, "/scraper/viewer");
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

    private void saveCrawlLogComplete(Long crawlLogId, int jobCount) {
        CrawlLog crawlLog = crawlLogRepository.findById(crawlLogId).orElse(null);
        if (crawlLog != null) {
            crawlLog.setStatus(CrawlLog.CrawlStatus.SUCCESS);
            crawlLog.setTotalCount(jobCount);
            crawlLog.setNewCount(jobCount);
            crawlLogRepository.save(crawlLog);
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public int[] saveJobPostings(CrawlConfig config, CrawlSiteConfig siteConfig,
                                  List<Map<String, String>> allJobs, Set<String> existingDedupKeys, Long crawlLogId) {
        String siteName = siteConfig.getSiteDefinition().getSiteName();
        int saved = 0;
        int dups = 0;

        for (Map<String, String> job : allJobs) {
            String company = job.getOrDefault("company", "");
            String position = job.getOrDefault("position", job.getOrDefault("title", ""));
            String location = job.getOrDefault("location", "");
            String url = job.getOrDefault("url", "");

            String dedupKey = JobPosting.generateDedupKey(company, position, location, siteName);

            if (existingDedupKeys.contains(dedupKey)) {
                dups++;
                continue;
            }

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
                    .crawlLogId(crawlLogId)
                    .crawledAt(LocalDate.now())
                    .build();

            try {
                jobPostingRepository.save(posting);
                existingDedupKeys.add(dedupKey);
                saved++;
            } catch (Exception e) {
                if (e.getMessage() != null && e.getMessage().contains("uk_job_postings_dedup")) {
                    dups++;
                    log.debug("Duplicate job detected: {}", dedupKey);
                } else {
                    log.error("Failed to save job: {} {}", company, position, e);
                    dups++;
                }
            }
        }

        log.info("Site {}: saved={}, dups={}", siteName, saved, dups);
        return new int[]{saved, dups};
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

    private String extractSearchCriteria(List<CrawlSiteConfig> siteConfigs) {
        java.util.Map<String, String> criteria = new java.util.HashMap<>();
        com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
        
        for (CrawlSiteConfig siteConfig : siteConfigs) {
            if (siteConfig.getParamValues() == null || siteConfig.getParamValues().isEmpty()) continue;
            
            try {
                com.fasterxml.jackson.databind.JsonNode node = mapper.readTree(siteConfig.getParamValues());
                if (node.has("keyword") && !node.get("keyword").asText().isEmpty()) {
                    criteria.putIfAbsent("keyword", node.get("keyword").asText());
                }
                if (node.has("career") && !node.get("career").asText().isEmpty()) {
                    criteria.putIfAbsent("career", node.get("career").asText());
                } else {
                    String career = buildCareerDisplay(node);
                    if (career != null && !career.isEmpty()) {
                        criteria.putIfAbsent("career", career);
                    }
                }
                if (node.has("location") && !node.get("location").asText().isEmpty()) {
                    criteria.putIfAbsent("location", node.get("location").asText());
                }
            } catch (Exception e) {
                // JSON 파싱 실패 시 무시
            }
        }
        
        try {
            return mapper.writeValueAsString(criteria);
        } catch (Exception e) {
            return "{}";
        }
    }

    private String buildCareerDisplay(com.fasterxml.jackson.databind.JsonNode node) {
        boolean hasMin = node.has("careerMin") && !node.get("careerMin").asText().isEmpty();
        boolean hasMax = node.has("careerMax") && !node.get("careerMax").asText().isEmpty();
        if (!hasMin && !hasMax) return null;

        int min = hasMin ? node.get("careerMin").asInt() : 0;
        int max = hasMax ? node.get("careerMax").asInt() : 15;

        if (min <= 0 && max >= 15) return null;

        String minStr = min > 0 ? min + "년" : "신입";
        String maxStr = max >= 15 ? "15년+" : max + "년";
        return minStr + "~" + maxStr;
    }

    private String buildJobAlertHtml(String configName, int success, int newJobs, int dupJobs,
                                     List<JobPosting> recentJobs) {
        StringBuilder body = new StringBuilder();

        // 요약 카드
        body.append(EmailTemplate.statCards(
                String.valueOf(success), "수집 사이트",
                String.valueOf(newJobs), "신규 공고",
                String.valueOf(dupJobs), "중복 제외"));

        // 공고 목록
        if (!recentJobs.isEmpty()) {
            body.append("<h3 style=\"font-size:15px;color:#1e293b;margin:0 0 12px;\">신규 공고 목록 (최대 10건)</h3>");
            body.append("<table style=\"width:100%;border-collapse:collapse;font-size:13px;\">");
            body.append("<tr style=\"background:#f1f5f9;\">");
            body.append("<th style=\"text-align:left;padding:8px 10px;border:1px solid #e2e8f0;color:#475569;\">회사</th>");
            body.append("<th style=\"text-align:left;padding:8px 10px;border:1px solid #e2e8f0;color:#475569;\">직무</th>");
            body.append("<th style=\"text-align:left;padding:8px 10px;border:1px solid #e2e8f0;color:#475569;\">경력</th>");
            body.append("<th style=\"text-align:left;padding:8px 10px;border:1px solid #e2e8f0;color:#475569;\">지역</th>");
            body.append("</tr>");

            int limit = Math.min(recentJobs.size(), 10);
            for (int i = 0; i < limit; i++) {
                JobPosting job = recentJobs.get(i);
                String bg = (i % 2 == 0) ? "#ffffff" : "#f8fafc";
                body.append("<tr style=\"background:").append(bg).append(";\">");
                body.append("<td style=\"padding:8px 10px;border:1px solid #e2e8f0;font-weight:600;color:#1e293b;\">")
                        .append(EmailTemplate.escapeHtml(job.getCompany() != null ? job.getCompany() : "-")).append("</td>");
                body.append("<td style=\"padding:8px 10px;border:1px solid #e2e8f0;color:#334155;\">")
                        .append(EmailTemplate.escapeHtml(job.getPosition() != null ? job.getPosition() : "-")).append("</td>");
                body.append("<td style=\"padding:8px 10px;border:1px solid #e2e8f0;color:#334155;\">")
                        .append(EmailTemplate.escapeHtml(job.getCareer() != null ? job.getCareer() : "-")).append("</td>");
                body.append("<td style=\"padding:8px 10px;border:1px solid #e2e8f0;color:#334155;\">")
                        .append(EmailTemplate.escapeHtml(job.getLocation() != null ? job.getLocation() : "-")).append("</td>");
                body.append("</tr>");
            }
            body.append("</table>");
        } else {
            body.append("<p style=\"font-size:14px;color:#64748b;\">새로 수집된 공고가 없습니다.</p>");
        }

        // 버튼
        body.append(EmailTemplate.button("https://sunghoonyk.duckdns.org/scraper/viewer", "공고 보러 가기 →"));

        return EmailTemplate.frame("신규 채용공고 수집 완료", configName, body.toString(),
                "크롤링 알림 설정에 따라 발송되었습니다.");
    }
}
