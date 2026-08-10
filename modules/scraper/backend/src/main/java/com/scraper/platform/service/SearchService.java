package com.scraper.platform.service;

import com.scraper.platform.api.dto.SearchRequest;
import com.scraper.platform.api.dto.SearchResponse;
import com.scraper.platform.crawler.CrawlerFactory;
import com.scraper.platform.crawler.SiteCrawler;
import com.scraper.platform.model.CompanyRating;
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
    private final CompanyRatingService companyRatingService;

    private ExecutorService executorService = Executors.newFixedThreadPool(4);

    /**
     * 실시간 검색을 수행한다.
     *
     * @param request 검색 요청 (키워드, 경력 범위, 지역 목록, 사이트 목록)
     * @return 검색 결과
     */
    public SearchResponse search(SearchRequest request) {
        long startTime = System.currentTimeMillis();
        List<String> sites = request.sites();
        Map<String, String> standardParams = buildStandardParams(request);

        log.info("Real-time search started: keyword={}, careerMin={}, careerMax={}, locations={}, sites={}",
                request.keyword(), request.careerMin(), request.careerMax(), request.locations(), sites);

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
        Integer careerMin = request.careerMin();
        Integer careerMax = request.careerMax();
        List<String> locations = request.locations();
        if (careerMin == null && careerMax == null && request.career() != null
                && !request.career().isEmpty() && !request.career().equals("전체") && !request.career().equals("경력무관")) {
            int[] legacyRange = parseCareerRange(request.career());
            if (legacyRange != null) {
                careerMin = legacyRange[0];
                careerMax = legacyRange[1] == Integer.MAX_VALUE ? null : legacyRange[1];
            }
        }
        if (locations == null || locations.isEmpty()) {
            if (request.location() != null && !request.location().isEmpty() && !request.location().equals("전체")) {
                locations = List.of(request.location());
            } else {
                locations = List.of();
            }
        }

        List<Map<String, String>> filtered = filterJobs(allJobs, careerMin, careerMax, locations);
        Map<String, Integer> filteredCounts = new LinkedHashMap<>();
        for (Map.Entry<String, Integer> entry : siteCounts.entrySet()) {
            String sn = entry.getKey();
            long cnt = filtered.stream().filter(j -> sn.equals(j.get("site"))).count();
            filteredCounts.put(sn, (int) cnt);
        }

        // 기업 평점 수집 (Background)
        List<String> companyNames = filtered.stream()
                .map(j -> j.get("company"))
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        
        if (!companyNames.isEmpty()) {
            try {
                List<CompanyRating> ratings = companyRatingService.getRatings(companyNames);
                Map<String, CompanyRating> ratingMap = ratings.stream()
                        .collect(Collectors.toMap(CompanyRating::getCompanyName, r -> r, (a, b) -> a));
                
                // 각 공고에 평점 정보 추가
                for (Map<String, String> job : filtered) {
                    String company = job.get("company");
                    if (company != null && ratingMap.containsKey(company)) {
                        CompanyRating rating = ratingMap.get(company);
                        if (rating.getAverageScore() != null) {
                            job.put("companyScore", String.valueOf(rating.getAverageScore()));
                        }
                        if (rating.getJobplanetScore() != null) {
                            job.put("jobplanetScore", String.valueOf(rating.getJobplanetScore()));
                        }
                        if (rating.getJobkoreaScore() != null) {
                            job.put("jobkoreaScore", String.valueOf(rating.getJobkoreaScore()));
                        }
                        if (rating.getSaraminScore() != null) {
                            job.put("saraminScore", String.valueOf(rating.getSaraminScore()));
                        }
                    }
                }
            } catch (Exception e) {
                log.warn("Failed to fetch company ratings: {}", e.getMessage());
            }
        }

        long searchTime = System.currentTimeMillis() - startTime;
        log.info("Real-time search completed: {} raw, {} filtered in {}ms", allJobs.size(), filtered.size(), searchTime);

        return SearchResponse.of(filtered.size(), filtered, filteredCounts, searchTime, failedSites);
    }

    private List<Map<String, String>> filterJobs(List<Map<String, String>> jobs, Integer careerMin, Integer careerMax, List<String> locations) {
        boolean filterCareer = careerMin != null || careerMax != null;
        boolean filterLocation = locations != null && !locations.isEmpty();
        if (!filterCareer && !filterLocation) return new ArrayList<>(jobs);
        return jobs.stream().filter(job -> {
            if (filterCareer && !"true".equals(job.get("careerFiltered"))) {
                String jobCareer = job.getOrDefault("career", "");
                if (!matchesCareerRange(jobCareer, careerMin, careerMax)) return false;
            }
            if (filterLocation && !"true".equals(job.get("locationFiltered"))) {
                String jobLoc = job.getOrDefault("location", "");
                if (!matchesLocationAny(jobLoc, locations)) return false;
            }
            return true;
        }).<Map<String, String>>map(job -> {
            Map<String, String> clean = new HashMap<>(job);
            clean.remove("careerFiltered");
            clean.remove("locationFiltered");
            return clean;
        }).collect(Collectors.toList());
    }

    /**
     * 잡 경력 텍스트가 [min, max] 연수 범위와 겹치는지 확인한다.
     */
    private boolean matchesCareerRange(String jobCareer, Integer careerMin, Integer careerMax) {
        if (jobCareer.isEmpty() || jobCareer.contains("무관")) return true;
        int[] jr = parseCareerRange(jobCareer);
        if (jr == null) return true;
        int lo = careerMin != null ? careerMin : 0;
        int hi = careerMax != null ? careerMax : Integer.MAX_VALUE;
        return jr[0] <= hi && jr[1] >= lo;
    }

    private int[] parseCareerRange(String career) {
        var m = java.util.regex.Pattern.compile("(\\d+)~(\\d+)").matcher(career);
        if (m.find()) return new int[]{Integer.parseInt(m.group(1)), Integer.parseInt(m.group(2))};
        // ~ 제거 후 나머지 패턴 매칭
        String c = career.replaceAll("[\\s~]", "");
        if (c.contains("신입·경력") || c.contains("신입경력")) return new int[]{0, Integer.MAX_VALUE};
        if (c.contains("신입")) return new int[]{0, 0};
        if (c.equals("경력")) return new int[]{1, Integer.MAX_VALUE};
        m = java.util.regex.Pattern.compile("(\\d+)년↑").matcher(c);
        if (m.find()) return new int[]{Integer.parseInt(m.group(1)), Integer.MAX_VALUE};
        m = java.util.regex.Pattern.compile("경력(\\d+)년").matcher(c);
        if (m.find()) return new int[]{Integer.parseInt(m.group(1)), Integer.MAX_VALUE};
        m = java.util.regex.Pattern.compile("(\\d+)년이상").matcher(c);
        if (m.find()) return new int[]{Integer.parseInt(m.group(1)), Integer.MAX_VALUE};
        m = java.util.regex.Pattern.compile("(\\d+)(년|年)").matcher(c);
        if (m.find()) { int v = Integer.parseInt(m.group(1)); return new int[]{v, v}; }
        return null;
    }

    private boolean matchesLocationAny(String jobLocation, List<String> locations) {
        if (jobLocation.isEmpty()) return true;
        for (String loc : locations) {
            if (loc != null && !loc.isEmpty() && jobLocation.contains(loc)) return true;
        }
        return false;
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
        if (request.careerMin() != null) {
            params.put("careerMin", String.valueOf(request.careerMin()));
        }
        if (request.careerMax() != null) {
            params.put("careerMax", String.valueOf(request.careerMax()));
        }
        if (request.locations() != null && !request.locations().isEmpty()) {
            params.put("location", String.join(",", request.locations()));
        } else if (request.location() != null && !request.location().isEmpty() && !request.location().equals("전체")) {
            params.put("location", request.location());
        }
        if (request.careerMin() == null && request.career() != null
                && !request.career().isEmpty() && !request.career().equals("전체") && !request.career().equals("경력무관")) {
            params.put("career", request.career());
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
