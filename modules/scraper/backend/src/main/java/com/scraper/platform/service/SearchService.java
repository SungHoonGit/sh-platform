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

    private static final Map<String, List<String>> KEYWORD_ALIASES = Map.ofEntries(
        Map.entry("리액트", List.of("react")),
        Map.entry("자바", List.of("java")),
        Map.entry("스프링", List.of("spring")),
        Map.entry("스프링부트", List.of("springboot", "spring boot")),
        Map.entry("파이썬", List.of("python")),
        Map.entry("타입스크립트", List.of("typescript", "ts")),
        Map.entry("노드", List.of("node", "nodejs", "node.js")),
        Map.entry("앵귤러", List.of("angular", "angularjs", "angular.js")),
        Map.entry("뷰", List.of("vue", "vuejs", "vue.js")),
        Map.entry("리액트네이티브", List.of("reactnative", "react native")),
        Map.entry("넥스트", List.of("next", "nextjs", "next.js")),
        Map.entry("넥스트js", List.of("nextjs", "next.js", "next")),
        Map.entry("고", List.of("golang")),
        Map.entry("고랭", List.of("golang")),
        Map.entry("쿠버네티스", List.of("kubernetes", "k8s")),
        Map.entry("도커", List.of("docker")),
        Map.entry("장고", List.of("django")),
        Map.entry("레일즈", List.of("rails")),
        Map.entry("플러터", List.of("flutter")),
        Map.entry("스벨트", List.of("svelte")),
        Map.entry("젠킨스", List.of("jenkins")),
        Map.entry("깃", List.of("git")),
        Map.entry("마리아디비", List.of("mariadb")),
        Map.entry("몽고", List.of("mongo", "mongodb")),
        Map.entry("레디스", List.of("redis")),
        Map.entry("엘라스틱서치", List.of("elasticsearch", "es")),
        Map.entry("카프카", List.of("kafka")),
        Map.entry("하둡", List.of("hadoop")),
        Map.entry("스파크", List.of("spark")),
        Map.entry("제이쿼리", List.of("jquery")),
        Map.entry("마이바티스", List.of("mybatis")),
        Map.entry("하이버네이트", List.of("hibernate")),
        Map.entry("제이피에이", List.of("jpa")),
        Map.entry("에이더블유에스", List.of("aws")),
        Map.entry("제이에스피", List.of("jsp")),
        Map.entry("서블릿", List.of("servlet"))
    );

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
        List<Map<String, String>> filtered = filterJobs(allJobs, request.keyword(), request.career(), request.location());
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

    private List<Map<String, String>> filterJobs(List<Map<String, String>> jobs, String keyword, String career, String location) {
        boolean filterKeyword = keyword != null && !keyword.isEmpty();
        boolean filterCareer = career != null && !career.isEmpty() && !career.equals("전체") && !career.equals("경력무관");
        boolean filterLocation = location != null && !location.isEmpty() && !location.equals("전체");
        if (!filterKeyword && !filterCareer && !filterLocation) return new ArrayList<>(jobs);
        List<String> keywords = filterKeyword ? expandKeywords(keyword) : List.of();
        log.debug("filterJobs: keyword={}, expanded={}, jobs={}", keyword, keywords, jobs.size());
        return jobs.stream().filter(job -> {
            if (filterKeyword) {
                String title = (job.getOrDefault("title", "") + " " + job.getOrDefault("position", "")).toLowerCase();
                String company = job.getOrDefault("company", "").toLowerCase();
                String tech = job.getOrDefault("tech", "").toLowerCase();
                boolean matched = false;
                for (String kw : keywords) {
                    if (title.contains(kw) || company.contains(kw) || tech.contains(kw)) {
                        matched = true;
                        break;
                    }
                }
                if (!matched) return false;
            }
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

    private List<String> expandKeywords(String raw) {
        String kw = raw.toLowerCase(Locale.ROOT).trim();
        Set<String> expanded = new LinkedHashSet<>();
        expanded.add(kw);
        for (var entry : KEYWORD_ALIASES.entrySet()) {
            String korean = entry.getKey().toLowerCase(Locale.ROOT);
            if (kw.contains(korean)) {
                for (String alias : entry.getValue()) {
                    expanded.add(alias.toLowerCase(Locale.ROOT));
                }
            }
            for (String alias : entry.getValue()) {
                if (kw.contains(alias.toLowerCase(Locale.ROOT))) {
                    expanded.add(korean);
                    break;
                }
            }
        }
        return new ArrayList<>(expanded);
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
