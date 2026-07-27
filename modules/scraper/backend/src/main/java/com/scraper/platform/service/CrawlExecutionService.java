package com.scraper.platform.service;

import com.scraper.platform.crawler.CrawlerFactory;
import com.scraper.platform.crawler.SiteCrawler;
import com.scraper.platform.model.*;
import com.scraper.platform.repository.*;
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

    /** 지역 키워드 → 실제 도시/광역시 매핑 */
    private static final Map<String, Set<String>> LOCATION_MAP = new HashMap<>();
    static {
        LOCATION_MAP.put("서울", Set.of("서울", "강남", "강동", "강북", "강서", "관악", "광진", "구로", "금천", "노원", "도봉", "동대문", "동작", "마포", "서대문", "서초", "성동", "성북", "송파", "양천", "영등포", "용산", "은평", "종로", "중구", "중랑"));
        LOCATION_MAP.put("경기", Set.of("경기", "판교", "성남", "수원", "고양", "용인", "부천", "안양", "안산", "화성", "김포", "파주", "하남", "남양주", "오산", "평택", "의정부", "군포", "시흥", "양주", "동두천", "포천", "구리", "가평", "연천", "여주", "이천", "안성", "양평", "광주"));
        LOCATION_MAP.put("인천", Set.of("인천", "남동", "부평", "계양", "미추홀", "연수", "서구", "강화", "옹진"));
        LOCATION_MAP.put("부산", Set.of("부산", "해운대", "부산진", "동래", "남구", "북구", "사하", "금정", "강서", "연제", "수영", "사상", "기장"));
        LOCATION_MAP.put("대구", Set.of("대구", "중구", "동구", "서구", "남구", "북구", "수성", "달서", "달성"));
        LOCATION_MAP.put("대전", Set.of("대전", "동구", "중구", "서구", "유성", "대덕"));
        LOCATION_MAP.put("광주", Set.of("광주", "동구", "서구", "남구", "북구", "광산"));
        LOCATION_MAP.put("울산", Set.of("울산", "중구", "남구", "동구", "북구", "울주"));
        LOCATION_MAP.put("세종", Set.of("세종"));
        LOCATION_MAP.put("충남", Set.of("충남", "천안", "아산", "서산", "논산", "계룡", "당진", "금산", "부여", "서천", "청양", "홍성", "태안", "보령"));
        LOCATION_MAP.put("충북", Set.of("충북", "청주", "충주", "제천", "보은", "옥천", "영동", "진천", "괴산", "음성", "단양", "증평"));
        LOCATION_MAP.put("전남", Set.of("전남", "여수", "순천", "광양", "목포", "여천", "나주", "담양", "곡성", "구례", "고흥", "보성", "화순", "장흥", "강진", "해남", "영암", "무안", "함평", "영광", "장성", "완도", "진도", "신안"));
        LOCATION_MAP.put("전북", Set.of("전북", "전주", "군산", "익산", "정읍", "김제", "남원", "완주", "무주", "진안", "장수", "임실", "순창", "고창", "부안"));
        LOCATION_MAP.put("경남", Set.of("경남", "창원", "김해", "진주", "통영", "사천", "김천", "밀양", "거제", "양산", "의령", "함안", "창녕", "고성", "남해", "하동", "산청", "함양", "거창", "합천"));
        LOCATION_MAP.put("경북", Set.of("경북", "포항", "경주", "구미", "김천", "안동", "영주", "영천", "상주", "문경", "안성", "의성", "청송", "영덕", "영양", "봉화", "울진", "울릉"));
        LOCATION_MAP.put("강원", Set.of("강원", "춘천", "원주", "강릉", "동해", "태백", "속초", "삼척", "홍천", "횡성", "영월", "평창", "정선", "철원", "화천", "양구", "인제", "고성", "양양"));
        LOCATION_MAP.put("제주", Set.of("제주", "서귀포"));
        LOCATION_MAP.put("해외", Set.of("해외", "일본", "미국", "중국", "싱가포르", "베트남", "유럽"));
    }

    private final CrawlConfigRepository crawlConfigRepository;
    private final CrawlSiteConfigRepository crawlSiteConfigRepository;
    private final CrawlDataRepository crawlDataRepository;
    private final CrawlLogRepository crawlLogRepository;
    private final SiteDefinitionRepository siteDefinitionRepository;
    private final NotificationService notificationService;
    private final CrawlerFactory crawlerFactory;

    public List<Map<String, Object>> searchSites(String keyword, String career, String location, List<String> siteIds) {
        log.info("Real-time search: keyword={}, career={}, location={}, sites={}", keyword, career, location, siteIds);

        Map<String, String> paramMap = new LinkedHashMap<>();
        if (keyword != null && !keyword.isBlank()) paramMap.put("keyword", keyword.trim());
        if (career != null && !career.isBlank() && !career.equals("전체")) paramMap.put("career", career.trim());
        if (location != null && !location.isBlank() && !location.equals("전체")) paramMap.put("location", location.trim());
        String paramValues;
        try {
            paramValues = new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(paramMap);
        } catch (Exception e) {
            throw new RuntimeException("Failed to build paramValues JSON", e);
        }

        // 병렬 크롤링: 각 사이트를 별도 스레드에서 실행
        List<Map<String, Object>> siteResults = new java.util.concurrent.CopyOnWriteArrayList<>();
        List<java.util.concurrent.CompletableFuture<Void>> futures = new ArrayList<>();

        for (String siteId : siteIds) {
            SiteDefinition siteDef = siteDefinitionRepository.findBySiteName(siteId).orElse(null);
            if (siteDef == null) {
                log.warn("Unknown site: {}", siteId);
                continue;
            }
            SiteCrawler crawler = crawlerFactory.getCrawler(siteId);
            if (crawler == null) {
                log.warn("No crawler for: {}", siteId);
                continue;
            }

            futures.add(java.util.concurrent.CompletableFuture.runAsync(() -> {
                CrawlSiteConfig tempConfig = CrawlSiteConfig.builder()
                        .siteDefinition(siteDef)
                        .paramValues(paramValues)
                        .isEnabled(true)
                        .build();
                try {
                    List<Map<String, String>> jobs = crawler.search(tempConfig);

                    if (!jobs.isEmpty()) {
                        Map<String, String> sample = jobs.get(0);
                        log.info("Site {} raw job #0 keys={}, company={}, position={}, title={}, tech={}, location={}, career={}",
                                siteId, sample.keySet(), sample.get("company"),
                                sample.get("position"), sample.get("title"), sample.get("tech"),
                                sample.get("location"), sample.get("career"));
                    }

                    // 서버에서 키워드 필터링
                    String keywordFilter = paramMap.getOrDefault("keyword", "");
                    if (!keywordFilter.isEmpty()) {
                        String kw = keywordFilter.toLowerCase();
                        jobs = jobs.stream()
                                .filter(job -> {
                                    String title = job.getOrDefault("title", "").toLowerCase();
                                    String position = job.getOrDefault("position", "").toLowerCase();
                                    String company = job.getOrDefault("company", "").toLowerCase();
                                    String tech = job.getOrDefault("tech", "").toLowerCase();
                                    return title.contains(kw) || position.contains(kw) || company.contains(kw) || tech.contains(kw);
                                })
                                .toList();
                    }

                    // 서버에서 지역 필터링
                    String locationFilter = paramMap.getOrDefault("location", "");
                    if (!locationFilter.isEmpty() && !locationFilter.equals("전체")) {
                        String loc = locationFilter.toLowerCase();
                        // 매핑된 지역 키워드 목록 조회 (예: "서울" → {"서울","강남","서초",...})
                        Set<String> expandedLocations = LOCATION_MAP.entrySet().stream()
                                .filter(e -> loc.contains(e.getKey().toLowerCase()) || e.getKey().toLowerCase().contains(loc))
                                .flatMap(e -> e.getValue().stream())
                                .map(String::toLowerCase)
                                .collect(Collectors.toSet());
                        if (expandedLocations.isEmpty()) {
                            expandedLocations.add(loc);
                        }
                        jobs = jobs.stream()
                                .filter(job -> {
                                    String jobLocation = job.getOrDefault("location", "").toLowerCase();
                                    if (jobLocation.isEmpty()) return false;
                                    return expandedLocations.stream().anyMatch(l -> jobLocation.contains(l));
                                })
                                .toList();
                    }

                    // 서버에서 경력 필터링
                    String careerFilter = paramMap.getOrDefault("career", "");
                    if (!careerFilter.isEmpty() && !careerFilter.equals("전체")) {
                        jobs = jobs.stream()
                                .filter(job -> {
                                    String jobCareer = job.getOrDefault("career", "").toLowerCase();
                                    if (jobCareer.isEmpty()) return false;
                                    String cf = careerFilter.toLowerCase();
                                    if (jobCareer.contains(cf) || cf.contains(jobCareer)) return true;
                                    try {
                                        int minRequired = Integer.parseInt(cf.replaceAll("[^0-9].*", ""));
                                        int jobMin = Integer.parseInt(jobCareer.replaceAll("[^0-9].*", ""));
                                        return jobMin <= minRequired + 2;
                                    } catch (Exception e) {
                                        return true;
                                    }
                                })
                                .toList();
                    }

                    Map<String, Object> result = new HashMap<>();
                    result.put("site", siteDef.getDisplayName());
                    result.put("siteId", siteId);
                    result.put("count", jobs.size());
                    result.put("jobs", jobs);
                    siteResults.add(result);
                } catch (Exception e) {
                    log.error("Search failed for site: {}", siteId, e);
                    Map<String, Object> errorResult = new HashMap<>();
                    errorResult.put("site", siteDef.getDisplayName());
                    errorResult.put("siteId", siteId);
                    errorResult.put("error", e.getMessage());
                    siteResults.add(errorResult);
                }
            }));
        }

        // 모든 크롤링 완료 대기 (최대 120초)
        try {
            java.util.concurrent.CompletableFuture.allOf(futures.toArray(new java.util.concurrent.CompletableFuture[0]))
                    .get(120, java.util.concurrent.TimeUnit.SECONDS);
        } catch (Exception e) {
            log.error("Some crawlers timed out", e);
        }

        return siteResults;
    }

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
                .findEnabledWithSite(config.getId());

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
