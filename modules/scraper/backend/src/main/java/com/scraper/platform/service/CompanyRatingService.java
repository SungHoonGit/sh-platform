package com.scraper.platform.service;

import com.scraper.platform.model.CompanyRating;
import com.scraper.platform.repository.CompanyRatingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;

/**
 * 기업 평점 수집 서비스.
 * 잡플래닛, 잡코리아, 사람인에서 기업 평점을 수집하여 캐싱.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CompanyRatingService {

    private final Map<String, CompanyRating> cache = new ConcurrentHashMap<>();
    private final CompanyRatingRepository companyRatingRepository;
    private final Executor taskExecutor;

    /**
     * 여러 기업의 평점을 조회한다.
     * 캐시된 데이터가 있으면 즉시 반환, 없으면 Background에서 수집.
     */
    public List<CompanyRating> getRatings(List<String> companyNames) {
        if (companyNames == null || companyNames.isEmpty()) {
            return Collections.emptyList();
        }

        List<CompanyRating> result = new ArrayList<>();
        List<String> uncachedCompanies = new ArrayList<>();

        for (String companyName : companyNames) {
            if (companyName == null || companyName.trim().isEmpty()) continue;
            
            String normalized = companyName.trim();
            
            // 1. 메모리 캐시 확인
            CompanyRating cached = cache.get(normalized);
            if (cached != null && !cached.isExpired()) {
                result.add(cached);
                continue;
            }

            // 2. DB 확인
            Optional<CompanyRating> dbRating = companyRatingRepository.findByCompanyName(normalized);
            if (dbRating.isPresent() && !dbRating.get().isExpired()) {
                cache.put(normalized, dbRating.get());
                result.add(dbRating.get());
                continue;
            }

            // 3. 미캐시 기업 목록에 추가
            uncachedCompanies.add(normalized);
            result.add(CompanyRating.builder()
                    .companyName(normalized)
                    .build());
        }

        // 4. 미캐시 기업들은 별도 스레드에서 수집 (검색 응답 블로킹 방지)
        if (!uncachedCompanies.isEmpty()) {
            List<String> toScrape = new ArrayList<>(uncachedCompanies);
            taskExecutor.execute(() -> scrapeAndCacheBatch(toScrape));
        }

        return result;
    }

    /**
     * 단일 기업의 평점을 조회한다.
     */
    public CompanyRating getRating(String companyName) {
        if (companyName == null || companyName.trim().isEmpty()) {
            return null;
        }

        String normalized = companyName.trim();

        // 1. 메모리 캐시 확인
        CompanyRating cached = cache.get(normalized);
        if (cached != null && !cached.isExpired()) {
            return cached;
        }

        // 2. DB 확인
        Optional<CompanyRating> dbRating = companyRatingRepository.findByCompanyName(normalized);
        if (dbRating.isPresent() && !dbRating.get().isExpired()) {
            cache.put(normalized, dbRating.get());
            return dbRating.get();
        }

        // 3. Background에서 수집 (별도 스레드)
        taskExecutor.execute(() -> scrapeAndCache(normalized));
        return CompanyRating.builder()
                .companyName(normalized)
                .build();
    }

    /**
     * 여러 기업의 평점을 Background에서 수집하여 캐싱.
     */
    @Async
    public void scrapeAndCacheBatch(List<String> companyNames) {
        for (String companyName : companyNames) {
            try {
                scrapeAndCache(companyName);
                Thread.sleep(500); // Rate limit: 0.5초 대기
            } catch (Exception e) {
                log.warn("Failed to scrape rating for {}: {}", companyName, e.getMessage());
            }
        }
    }

    /**
     * 단일 기업의 평점을 수집하여 캐싱.
     */
    @Async
    public void scrapeAndCache(String companyName) {
        try {
            CompanyRating rating = companyRatingRepository.findByCompanyName(companyName)
                    .orElseGet(() -> CompanyRating.builder()
                            .companyName(companyName)
                            .build());

            // 잡플래닛 평점 수집
            try {
                Double jobplanetScore = scrapeJobPlanet(companyName);
                if (jobplanetScore != null) {
                    rating.setJobplanetScore(jobplanetScore);
                    log.info("JobPlanet score for {}: {}", companyName, jobplanetScore);
                }
            } catch (Exception e) {
                log.debug("Failed to scrape JobPlanet for {}: {}", companyName, e.getMessage());
            }

            // 잡코리아 평점 수집
            try {
                Double jobkoreaScore = scrapeJobKorea(companyName);
                if (jobkoreaScore != null) {
                    rating.setJobkoreaScore(jobkoreaScore);
                    log.info("JobKorea score for {}: {}", companyName, jobkoreaScore);
                }
            } catch (Exception e) {
                log.debug("Failed to scrape JobKorea for {}: {}", companyName, e.getMessage());
            }

            // 사람인 평점 수집
            try {
                Double saraminScore = scrapeSaramin(companyName);
                if (saraminScore != null) {
                    rating.setSaraminScore(saraminScore);
                    log.info("Saramin score for {}: {}", companyName, saraminScore);
                }
            } catch (Exception e) {
                log.debug("Failed to scrape Saramin for {}: {}", companyName, e.getMessage());
            }

            // 평균 계산
            rating.calculateAverage();
            rating.setLastUpdatedAt(LocalDateTime.now());

            // DB 저장
            companyRatingRepository.save(rating);

            // 캐시 업데이트
            cache.put(companyName, rating);

            log.info("Updated rating for {}: avg={}", companyName, rating.getAverageScore());

        } catch (Exception e) {
            log.error("Failed to scrape and cache rating for {}: {}", companyName, e.getMessage());
        }
    }

    /**
     * 잡플래닛에서 기업 평점을 수집한다.
     * curl을 사용하여 잡플래닛 기업 검색 페이지에서 평점 추출.
     */
    private Double scrapeJobPlanet(String companyName) {
        try {
            // 잡플래닛 기업 검색 API (비공식)
            String searchUrl = "https://www.jobplanet.co.kr/companies?query=" + 
                    java.net.URLEncoder.encode(companyName, "UTF-8");

            ProcessBuilder pb = new ProcessBuilder(
                "curl", "-s", "-L",
                "--max-time", "10",
                "-H", "User-Agent: Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36",
                "-H", "Accept: application/json, text/plain, */*",
                "-H", "Accept-Language: ko-KR,ko;q=0.9",
                searchUrl
            );
            pb.redirectErrorStream(true);

            Process process = pb.start();
            byte[] bytes = process.getInputStream().readAllBytes();
            boolean finished = process.waitFor(15, java.util.concurrent.TimeUnit.SECONDS);

            if (!finished) {
                process.destroyForcibly();
                return null;
            }

            String output = new String(bytes, java.nio.charset.StandardCharsets.UTF_8);

            // HTML에서 평점 추출 (정규식 사용)
            // 잡플래닛 기업 페이지에서 평점은 보통 "X.X" 형태로 표시
            java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(
                "\"score\"\\s*:\\s*([\\d.]+)");
            java.util.regex.Matcher matcher = pattern.matcher(output);
            
            if (matcher.find()) {
                return Double.parseDouble(matcher.group(1));
            }

            // 다른 패턴 시도
            pattern = java.util.regex.Pattern.compile(
                "class=\"[^\"]*score[^\"]*\"[^>]*>([\\d.]+)<");
            matcher = pattern.matcher(output);
            
            if (matcher.find()) {
                return Double.parseDouble(matcher.group(1));
            }

            return null;

        } catch (Exception e) {
            log.debug("Failed to scrape JobPlanet for {}: {}", companyName, e.getMessage());
            return null;
        }
    }

    /**
     * 잡코리아에서 기업 평점을 수집한다.
     */
    private Double scrapeJobKorea(String companyName) {
        try {
            // 잡코리아 기업 검색
            String searchUrl = "https://www.jobkorea.co.kr/Search/?stext=" + 
                    java.net.URLEncoder.encode(companyName, "UTF-8");

            ProcessBuilder pb = new ProcessBuilder(
                "curl", "-s", "-L",
                "--max-time", "10",
                "-H", "User-Agent: Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36",
                "-H", "Accept: text/html,application/xhtml+xml,application/xml;q=0.9",
                "-H", "Accept-Language: ko-KR,ko;q=0.9",
                searchUrl
            );
            pb.redirectErrorStream(true);

            Process process = pb.start();
            byte[] bytes = process.getInputStream().readAllBytes();
            boolean finished = process.waitFor(15, java.util.concurrent.TimeUnit.SECONDS);

            if (!finished) {
                process.destroyForcibly();
                return null;
            }

            String output = new String(bytes, java.nio.charset.StandardCharsets.UTF_8);

            // 잡코리아는 기업 평점이 있는지 확인 필요
            // 현재는 평점 데이터가 공개적으로 보이지 않을 수 있음
            log.debug("JobKorea search completed for {}, length: {}", companyName, output.length());
            
            return null;

        } catch (Exception e) {
            log.debug("Failed to scrape JobKorea for {}: {}", companyName, e.getMessage());
            return null;
        }
    }

    /**
     * 사람인에서 기업 평점을 수집한다.
     */
    private Double scrapeSaramin(String companyName) {
        try {
            // 사람인 기업 검색
            String searchUrl = "https://www.saramin.co.kr/zf_user/search?searchType=search&searchword=" + 
                    java.net.URLEncoder.encode(companyName, "UTF-8");

            ProcessBuilder pb = new ProcessBuilder(
                "curl", "-s", "-L",
                "--max-time", "10",
                "-H", "User-Agent: Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36",
                "-H", "Accept: text/html,application/xhtml+xml,application/xml;q=0.9",
                "-H", "Accept-Language: ko-KR,ko;q=0.9",
                searchUrl
            );
            pb.redirectErrorStream(true);

            Process process = pb.start();
            byte[] bytes = process.getInputStream().readAllBytes();
            boolean finished = process.waitFor(15, java.util.concurrent.TimeUnit.SECONDS);

            if (!finished) {
                process.destroyForcibly();
                return null;
            }

            String output = new String(bytes, java.nio.charset.StandardCharsets.UTF_8);

            // 사람인은 기업 평점이 있는지 확인 필요
            log.debug("Saramin search completed for {}, length: {}", companyName, output.length());
            
            return null;

        } catch (Exception e) {
            log.debug("Failed to scrape Saramin for {}: {}", companyName, e.getMessage());
            return null;
        }
    }

    /**
     * 캐시 초기화.
     */
    public void clearCache() {
        cache.clear();
        log.info("Company rating cache cleared");
    }

    /**
     * 캐시된 기업 수 반환.
     */
    public int getCacheSize() {
        return cache.size();
    }
}
