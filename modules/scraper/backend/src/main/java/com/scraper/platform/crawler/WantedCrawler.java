package com.scraper.platform.crawler;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.scraper.platform.model.CrawlSiteConfig;
import com.scraper.platform.service.SiteSearchMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class WantedCrawler implements SiteCrawler {

    private static final String API_BASE = "https://www.wanted.co.kr/api/v4/jobs";
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private final SiteSearchMapper siteSearchMapper;

    @Override
    public String getSiteName() {
        return "wanted";
    }

    @Override
    public List<Map<String, String>> search(CrawlSiteConfig siteConfig) throws Exception {
        String paramValues = siteConfig.getParamValues();

        // SiteSearchMapper를 사용하여 표준 파라미터를 사이트별 URL 파라미터로 변환
        Map<String, String> siteParams = siteSearchMapper.toSiteParams(getSiteName(), paramValues);

        // 필터링 플래그 추적
        boolean careerFiltered = false;
        boolean locationFiltered = false;

        Map<String, String> params = parseParams(paramValues);

        // 경력 필터 처리
        String career = params.getOrDefault("career", "");
        String careerMin = params.getOrDefault("careerMin", "");
        if (!career.isEmpty() && !career.equals("전체") && !career.equals("경력무관")) {
            careerFiltered = true;
        } else if (!careerMin.isEmpty() && !careerMin.equals("0")) {
            careerFiltered = true;
        }

        // 경력 범위(최소 연수)는 원티드 years 파라미터로 직접 변환
        if (!careerMin.isEmpty() && !careerMin.equals("0")
                && !siteParams.containsKey("years")) {
            siteParams.put("years", careerMin);
        }

        // 지역 필터 처리
        String location = params.getOrDefault("location", "");
        if (!location.isEmpty() && !location.equals("전체")) {
            locationFiltered = true;
        }

        log.info("Wanted crawler start: keyword={}, careerMin={}, siteParams={}", params.get("keyword"), careerMin, siteParams);

        int perPage = 20;
        List<Map<String, String>> allJobs = new ArrayList<>();

        // 최대 5페이지 (100건) 수집
        for (int p = 1; p <= 5; p++) {
            String url = buildUrl(siteParams, p, perPage);
            log.info("Wanted API URL (page {}): {}", p, url);

            String json = fetchWithCurl(url);
            if (json == null) {
                log.warn("Wanted API returned null for page {}", p);
                break;
            }

            JsonNode root = objectMapper.readTree(json);
            JsonNode data = root.get("data");
            if (data == null || !data.isArray() || data.isEmpty()) {
                log.info("No more jobs at page {}: data={}", p, data);
                break;
            }

            log.info("Wanted API returned {} jobs at page {}", data.size(), p);

            for (JsonNode jobNode : data) {
                Map<String, String> job = parseJobNode(jobNode);
                if (!job.isEmpty()) {
                    // 필터링 플래그 설정
                    if (careerFiltered) {
                        job.put("careerFiltered", "true");
                    }
                    if (locationFiltered) {
                        job.put("locationFiltered", "true");
                    }
                    allJobs.add(job);
                }
            }

            // 데이터가 요청한 개수보다 적으면 마지막 페이지
            if (data.size() < perPage) {
                break;
            }

            // Rate limit: 0.5초 대기
            Thread.sleep(500);
        }

        log.info("Total jobs from Wanted: {}", allJobs.size());
        return allJobs;
    }

    private String fetchWithCurl(String url) throws IOException, InterruptedException {
        ProcessBuilder pb = new ProcessBuilder(
            "curl", "-s", "-L",
            "--max-time", "30",
            "--compressed",
            "-H", "User-Agent: Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
            "-H", "Accept: application/json, text/plain, */*",
            "-H", "Accept-Language: ko-KR,ko;q=0.9,en-US;q=0.8,en;q=0.7",
            "-H", "Accept-Encoding: gzip, deflate, br",
            "-H", "Referer: https://www.wanted.co.kr/",
            "-H", "Origin: https://www.wanted.co.kr",
            url
        );
        pb.redirectErrorStream(true);

        Process process = pb.start();
        byte[] bytes = process.getInputStream().readAllBytes();
        boolean finished = process.waitFor(35, TimeUnit.SECONDS);

        if (!finished) {
            process.destroyForcibly();
            throw new IOException("curl timed out for URL: " + url);
        }

        int exitCode = process.exitValue();
        if (exitCode != 0) {
            throw new IOException("curl failed with exit code " + exitCode + " for URL: " + url);
        }

        return new String(bytes, StandardCharsets.UTF_8);
    }

    private String buildUrl(Map<String, String> siteParams, int page, int perPage) {
        StringBuilder sb = new StringBuilder(API_BASE);
        sb.append("?country=kr");
        sb.append("&job_sort=job.latest_order");
        sb.append("&limit=").append(perPage);
        sb.append("&offset=").append((page - 1) * perPage);

        // SiteSearchMapper에서 변환된 파라미터를 URL에 추가
        for (Map.Entry<String, String> entry : siteParams.entrySet()) {
            sb.append("&").append(entry.getKey()).append("=")
              .append(URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8));
        }

        // locations 파라미터가 없으면 all로 기본값 설정
        if (!siteParams.containsKey("locations")) {
            sb.append("&locations=all");
        }

        return sb.toString();
    }

    private Map<String, String> parseJobNode(JsonNode node) {
        Map<String, String> job = new HashMap<>();

        // 회사명
        JsonNode company = node.get("company");
        if (company != null) {
            JsonNode name = company.get("name");
            if (name != null) job.put("company", name.asText());
        }

        // 포지션
        JsonNode position = node.get("position");
        if (position != null) job.put("title", position.asText());
        if (position != null) job.put("position", position.asText());

        // ID → URL
        JsonNode id = node.get("id");
        if (id != null) {
            job.put("url", "https://www.wanted.co.kr/wd/" + id.asLong());
        }

        // 지역
        JsonNode address = node.get("address");
        if (address != null) {
            String loc = getTextNode(address, "location");
            String district = getTextNode(address, "district");
            if (!loc.isEmpty() || !district.isEmpty()) {
                job.put("location", (loc + " " + district).trim());
            }
        }

        // 경력 (연봉으로 대체 — Wanted는 연봉 정보 제공)
        JsonNode annualFrom = node.get("annual_from");
        JsonNode annualTo = node.get("annual_to");
        if (annualFrom != null && annualTo != null) {
            job.put("career", annualFrom.asInt() + "~" + annualTo.asInt() + "년");
        }

        // 마감일
        JsonNode dueTime = node.get("due_time");
        if (dueTime != null && !dueTime.isNull()) {
            job.put("deadline", dueTime.asText());
        }

        // 카테고리 태그 → 기술
        JsonNode categoryTags = node.get("category_tags");
        if (categoryTags != null && categoryTags.isArray()) {
            List<String> tags = new ArrayList<>();
            for (JsonNode tag : categoryTags) {
                // parent_id 521 = 직무 카테고리
                if (tag.has("parent_id") && tag.get("parent_id").asInt() == 521) {
                    // 카테고리 ID → 이름은 API에서 별도로 제공하지 않으므로 ID만
                }
            }
        }

        return job;
    }

    private String getTextNode(JsonNode node, String field) {
        JsonNode child = node.get(field);
        if (child != null && !child.isNull()) {
            return child.asText();
        }
        return "";
    }

    private Map<String, String> parseParams(String paramValues) {
        if (paramValues == null || paramValues.isEmpty()) {
            return new HashMap<>();
        }
        try {
            JsonNode node = objectMapper.readTree(paramValues);
            Map<String, String> params = new HashMap<>();
            node.fields().forEachRemaining(entry -> params.put(entry.getKey(), entry.getValue().asText()));
            return params;
        } catch (Exception e) {
            log.error("Failed to parse paramValues: {}", paramValues, e);
            return new HashMap<>();
        }
    }
}
