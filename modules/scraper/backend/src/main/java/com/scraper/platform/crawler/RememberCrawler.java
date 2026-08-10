package com.scraper.platform.crawler;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.scraper.platform.model.CrawlSiteConfig;
import com.scraper.platform.service.SiteSearchMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class RememberCrawler implements SiteCrawler {

    private static final String API_URL = "https://career-api.rememberapp.co.kr/job_postings/search";
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    private final SiteSearchMapper siteSearchMapper;

    @Override
    public String getSiteName() {
        return "remember";
    }

    @Override
    public List<Map<String, String>> search(CrawlSiteConfig siteConfig) throws Exception {
        String paramValues = siteConfig.getParamValues();

        // SiteSearchMapper를 사용하여 표준 파라미터를 사이트별 URL 파라미터로 변환
        Map<String, String> siteParams = siteSearchMapper.toSiteParams(getSiteName(), paramValues);

        Map<String, String> params = parseParams(paramValues);

        // 필터링 플래그 추적
        boolean careerFiltered = false;
        boolean locationFiltered = false;

        // 경력 필터 처리
        String career = params.getOrDefault("career", "");
        String careerMin = params.getOrDefault("careerMin", "");
        if (!career.isEmpty() && !career.equals("전체") && !career.equals("경력무관")) {
            careerFiltered = true;
        } else if (!careerMin.isEmpty() && !careerMin.equals("0")) {
            careerFiltered = true;
        }

        // 경력 범위(최소 연수)는 리멤버 min_experience로 직접 변환
        if (!careerMin.isEmpty() && !careerMin.equals("0")
                && !siteParams.containsKey("min_experience")) {
            siteParams.put("min_experience", careerMin);
        }

        // 다중 지역(콤마 구분)은 리멤버 sido가 단일 텍스트만 지원하므로
        // 지역 파라미터를 보내지 않고 서버사이드 필터로 걸러낸다.
        String location = params.getOrDefault("location", "");
        if (location.contains(",")) {
            siteParams.remove("sido");
            locationFiltered = true;
        } else if (!location.isEmpty() && !location.equals("전체")) {
            locationFiltered = true;
        }

        int maxPages = 5;
        int perPage = 30;
        List<Map<String, String>> allJobs = new ArrayList<>();

        for (int page = 1; page <= maxPages; page++) {
            String requestBody = buildRequestBody(siteParams, page, perPage);
            log.info("Remember API request (page {}): {}", page, requestBody);

            String json = postJson(requestBody);
            if (json == null) break;

            JsonNode root = objectMapper.readTree(json);
            JsonNode data = root.get("data");
            if (data == null || !data.isArray() || data.isEmpty()) {
                log.info("No more jobs at page {}", page);
                break;
            }

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

            // Check if more pages exist
            JsonNode meta = root.get("meta");
            if (meta != null) {
                int totalPages = meta.has("total_pages") ? meta.get("total_pages").asInt() : 0;
                if (page >= totalPages) break;
            }

            // Rate limit: 0.5초 대기
            Thread.sleep(500);
        }

        log.info("Total jobs from Remember: {}", allJobs.size());
        return allJobs;
    }

    private String postJson(String body) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_URL))
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .header("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7)")
                .header("Origin", "https://career.rememberapp.co.kr")
                .header("Referer", "https://career.rememberapp.co.kr/")
                .timeout(Duration.ofSeconds(15))
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            String responseBody = response.body();
            if (responseBody != null && !responseBody.isEmpty()) {
                log.warn("Remember API returned status {}: {}", response.statusCode(),
                    responseBody.substring(0, Math.min(200, responseBody.length())));
            } else {
                log.warn("Remember API returned status {} with empty body", response.statusCode());
            }
            return null;
        }

        return response.body();
    }

    private String buildRequestBody(Map<String, String> siteParams, int page, int perPage) {
        try {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("page", page);
            body.put("per", perPage);
            body.put("sort", "starts_at_desc");

            // SiteSearchMapper에서 변환된 파라미터를 요청 바디에 추가
            for (Map.Entry<String, String> entry : siteParams.entrySet()) {
                body.put(entry.getKey(), entry.getValue());
            }

            return objectMapper.writeValueAsString(body);
        } catch (Exception e) {
            log.error("Failed to build request body", e);
            return "{\"page\":" + page + ",\"per\":" + perPage + ",\"sort\":\"starts_at_desc\"}";
        }
    }

    private Map<String, String> parseJobNode(JsonNode node) {
        Map<String, String> job = new HashMap<>();

        // 포지션
        String title = getTextNode(node, "title");
        if (!title.isEmpty()) {
            job.put("title", title);
            job.put("position", title);
        }

        // ID → URL
        if (node.has("id") && !node.get("id").isNull()) {
            long id = node.get("id").asLong();
            job.put("url", "https://career.rememberapp.co.kr/job/posting/" + id);
        }

        // 회사명
        JsonNode org = node.get("organization");
        if (org != null && org.isObject()) {
            String companyName = getTextNode(org, "name");
            if (!companyName.isEmpty()) {
                job.put("company", companyName);
            }
        }

        // 경력
        if (node.has("min_experience") && !node.get("min_experience").isNull()) {
            int minExp = node.get("min_experience").asInt();
            String career = minExp + "년 이상";
            if (node.has("max_experience") && !node.get("max_experience").isNull()) {
                int maxExp = node.get("max_experience").asInt();
                career = minExp + "~" + maxExp + "년";
            }
            job.put("career", career);
        }

        // 기술 스택
        JsonNode techStacks = node.get("tech_stacks");
        if (techStacks != null && techStacks.isArray() && techStacks.size() > 0) {
            List<String> techs = new ArrayList<>();
            for (JsonNode tech : techStacks) {
                String techName = getTextNode(tech, "name");
                if (!techName.isEmpty()) {
                    techs.add(techName);
                }
            }
            if (!techs.isEmpty()) {
                job.put("tech", String.join(", ", techs));
            }
        }

        // 지역
        JsonNode address = node.get("address");
        if (address != null && address.isObject()) {
            String sido = getTextNode(address, "sido");
            String gugun = getTextNode(address, "gugun");
            if (!sido.isEmpty()) {
                job.put("location", sido + (gugun.isEmpty() ? "" : " " + gugun));
            }
        }

        // 마감일
        String endsAt = getTextNode(node, "ends_at");
        if (!endsAt.isEmpty()) {
            job.put("deadline", endsAt.substring(0, Math.min(10, endsAt.length())));
        }

        // 직군 카테고리
        JsonNode categories = node.get("job_categories");
        if (categories != null && categories.isArray() && categories.size() > 0) {
            JsonNode first = categories.get(0);
            String cat = getTextNode(first, "level2");
            if (!cat.isEmpty() && !job.containsKey("tech")) {
                job.put("tech", cat);
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
