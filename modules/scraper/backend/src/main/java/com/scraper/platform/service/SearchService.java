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

@Slf4j
@Service
@RequiredArgsConstructor
public class SearchService {

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
                List<Map<String, String>> jobs = entry.getValue().get(10, TimeUnit.SECONDS);
                jobs.forEach(job -> job.put("site", siteName));
                allJobs.addAll(jobs);
                siteCounts.put(siteName, jobs.size());
                log.info("Site {} returned {} jobs", siteName, jobs.size());
            } catch (Exception e) {
                log.error("Failed to get results from site: {}", siteName, e);
                siteCounts.put(siteName, 0);
                failedSites.add(siteName);
            }
        }

        long searchTime = System.currentTimeMillis() - startTime;
        log.info("Real-time search completed: {} total jobs in {}ms", allJobs.size(), searchTime);

        return SearchResponse.of(allJobs.size(), allJobs, siteCounts, searchTime, failedSites);
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
