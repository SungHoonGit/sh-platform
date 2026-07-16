package com.scraper.platform.crawler;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.scraper.platform.model.CrawlSiteConfig;
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
public class RememberCrawler implements SiteCrawler {

    private static final String API_URL = "https://career-api.rememberapp.co.kr/job_postings/search";
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    @Override
    public String getSiteName() {
        return "remember";
    }

    @Override
    public List<Map<String, String>> search(CrawlSiteConfig siteConfig) throws Exception {
        String paramValues = siteConfig.getParamValues();
        Map<String, String> params = parseParams(paramValues);

        String keyword = params.getOrDefault("keyword", "");
        int maxPages = 5;
        int perPage = 30;
        List<Map<String, String>> allJobs = new ArrayList<>();

        for (int page = 1; page <= maxPages; page++) {
            String requestBody = buildRequestBody(keyword, page, perPage);
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
            log.warn("Remember API returned status {}: {}", response.statusCode(), response.body().substring(0, Math.min(200, response.body().length())));
            return null;
        }

        return response.body();
    }

    private String buildRequestBody(String keyword, int page, int perPage) {
        try {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("page", page);
            body.put("per", perPage);
            body.put("sort", "starts_at_desc");

            // Remember API keyword search not supported - returns error
            // List all jobs and let the viewer filter by site

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
