package com.scraper.platform.service;

import com.scraper.platform.api.dto.SearchRequest;
import com.scraper.platform.api.dto.SearchResponse;
import com.scraper.platform.crawler.CrawlerFactory;
import com.scraper.platform.crawler.SiteCrawler;
import com.scraper.platform.model.SiteDefinition;
import com.scraper.platform.repository.SiteDefinitionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SearchService {

    private final SiteDefinitionRepository siteDefinitionRepository;
    private final CrawlerFactory crawlerFactory;
    private final SiteDefinitionRepository siteDefinitionRepository;

    private ExecutorService executorService = Executors.newFixedThreadPool(4);

    /**
     * 실시간 검색을 수행한다.
     *
     * @param request 검색 요청 (키워드, 경력, 지역, 사이트 목록)
     * @return 검색 결과
     */
    public SearchResponse search(SearchRequest request) {
        long startTime = System.currentTimeMillis();
        List<String> sites = request.sites();
        Map<String, String> standardParams = buildStandardParams(request);

        log.info("Real-time search started: keyword={}, career={}, location={}, sites={}",
                request.keyword(), request.career(), request.location(), sites);

        // 병렬 크롤링
        Map<String, CompletableFuture<List<Map<String, String>>>> futures = new LinkedHashMap<>();
        for (String siteName : sites) {
            SiteCrawler crawler = crawlerFactory.getCrawler(siteName);
            if (crawler == null) {
                log.warn("No crawler found for site: {}", siteName);
                continue;
            }

            futures.put(siteName, CompletableFuture.supplyAsync(() -> {
                try {
                    return executeSiteSearch(siteName, standardParams);
                } catch (Exception e) {
                    throw new RuntimeException("Failed to crawl " + siteName, e);
                }
            }, executorService));
        }

        // 결과 수집
        List<Map<String, String>> allJobs = new ArrayList<>();
        Map<String, Integer> siteCounts = new LinkedHashMap<>();
        List<String> failedSites = new ArrayList<>();

        for (Map.Entry<String, CompletableFuture<List<Map<String, String>>>> entry : futures.entrySet()) {
            String siteName = entry.getKey();
            try {
                List<Map<String, String>> jobs = entry.getValue().get(15, TimeUnit.SECONDS);
                List<Map<String, String>> tagged = new ArrayList<>();
                for (Map<String, String> job : jobs) {
                    Map<String, String> mutable = new HashMap<>(job);
                    mutable.put("site", siteName);
                    tagged.add(mutable);
                }
                allJobs.addAll(tagged);
                siteCounts.put(siteName, tagged.size());
                log.info("Site {} returned {} jobs", siteName, jobs.size());
            } catch (Exception e) {
                log.error("Failed to get results from site: {}", siteName, e);
                siteCounts.put(siteName, 0);
                failedSites.add(siteName);
            }
        }

        // 서버사이드 필터링 (크롤러가 사이트 URL 파라미터로 필터링 못 한 경우 보완)
        List<Map<String, String>> filtered = filterJobs(allJobs, request.career(), request.location());
        Map<String, Integer> filteredCounts = new LinkedHashMap<>();
        for (Map.Entry<String, Integer> entry : siteCounts.entrySet()) {
            String sn = entry.getKey();
            long cnt = filtered.stream().filter(j -> sn.equals(j.get("site"))).count();
            filteredCounts.put(sn, (int) cnt);
        }

        long searchTime = System.currentTimeMillis() - startTime;
        log.info("Real-time search completed: {} raw, {} filtered in {}ms", allJobs.size(), filtered.size(), searchTime);

        return SearchResponse.of(filtered.size(), filtered, filteredCounts, searchTime, failedSites);
    }

    private List<Map<String, String>> filterJobs(List<Map<String, String>> jobs, String career, String location) {
        boolean filterCareer = career != null && !career.isEmpty() && !career.equals("전체") && !career.equals("경력무관");
        boolean filterLocation = location != null && !location.isEmpty() && !location.equals("전체");
        if (!filterCareer && !filterLocation) return new ArrayList<>(jobs);
        return jobs.stream().filter(job -> {
            if (filterCareer) {
                String jobCareer = job.getOrDefault("career", "");
                if (!matchesCareer(jobCareer, career)) return false;
            }
            if (filterLocation) {
                String jobLoc = job.getOrDefault("location", "");
                if (!matchesLocation(jobLoc, location)) return false;
            }
            return true;
        }).<Map<String, String>>map(HashMap::new).collect(Collectors.toList());
    }

    private boolean matchesCareer(String jobCareer, String targetCareer) {
        if (jobCareer.isEmpty()) return true;
        String t = targetCareer.replaceAll("[~\\s]", "");
        String j = jobCareer.replaceAll("[~\\s]", "");
        return j.contains(t) || t.contains(j);
    }

    private boolean matchesLocation(String jobLocation, String targetLocation) {
        if (jobLocation.isEmpty()) return false;
        return jobLocation.contains(targetLocation);
    }

    private List<Map<String, String>> executeSiteSearch(String siteName, Map<String, String> standardParams) throws Exception {
        SiteDefinition site = siteDefinitionRepository.findBySiteName(siteName)
                .orElseThrow(() -> new IllegalArgumentException("Site not found: " + siteName));

        SiteCrawler crawler = crawlerFactory.getCrawler(siteName);

        // CrawlSiteConfig 생성 (크롤러가 내부에서 SiteSearchMapper로 파라미터 변환)
        com.scraper.platform.model.CrawlSiteConfig siteConfig =
            com.scraper.platform.model.CrawlSiteConfig.builder()
                .siteDefinition(site)
                .isEnabled(true)
                .paramValues(convertToJson(standardParams))
                .build();

        return crawler.search(siteConfig);
    }

    private Map<String, String> buildStandardParams(SearchRequest request) {
        Map<String, String> params = new HashMap<>();
        if (request.keyword() != null && !request.keyword().isEmpty()) {
            params.put("keyword", request.keyword());
        }
        if (request.career() != null && !request.career().isEmpty()) {
            params.put("career", request.career());
        }
        if (request.location() != null && !request.location().isEmpty()) {
            params.put("location", request.location());
        }
        return params;
    }

    private String convertToJson(Map<String, String> params) {
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            return mapper.writeValueAsString(params);
        } catch (Exception e) {
            return "{}";
        }
    }
}
