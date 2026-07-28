package com.scraper.platform.crawler;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.scraper.platform.model.CrawlSiteConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;

@Slf4j
@Component
public class WantedCrawler implements SiteCrawler {

    private static final String API_BASE = "https://www.wanted.co.kr/api/v4/jobs";
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    @Override
    public String getSiteName() {
        return "wanted";
    }

    @Override
    public List<Map<String, String>> search(CrawlSiteConfig siteConfig) throws Exception {
        String paramValues = siteConfig.getParamValues();
        Map<String, String> params = parseParams(paramValues);

        String keyword = params.getOrDefault("keyword", "");
        String career = params.getOrDefault("career", "");
        String location = params.getOrDefault("location", "");
        int page = 1;
        int perPage = 20;
        List<Map<String, String>> allJobs = new ArrayList<>();

        // 최대 5페이지 (100건) 수집
        for (int p = 1; p <= 5; p++) {
            String url = buildUrl(keyword, career, location, p, perPage);
            log.info("Wanted API URL (page {}): {}", p, url);

            String json = fetchJson(url);
            if (json == null) break;

            JsonNode root = objectMapper.readTree(json);
            JsonNode data = root.get("data");
            if (data == null || !data.isArray() || data.isEmpty()) {
                log.info("No more jobs at page {}", p);
                break;
            }

            for (JsonNode jobNode : data) {
                Map<String, String> job = parseJobNode(jobNode);
                if (!job.isEmpty()) {
                    allJobs.add(job);
                }
            }

            // 다음 페이지 링크 확인
            JsonNode links = root.get("links");
            if (links == null || links.get("next") == null || links.get("next").isNull()) {
                break;
            }

            // Rate limit: 0.5초 대기
            Thread.sleep(500);
        }

        log.info("Total jobs from Wanted: {}", allJobs.size());
        return allJobs;
    }

    private String fetchJson(String url) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36")
                .header("Accept", "application/json")
                .header("Accept-Language", "ko-KR,ko;q=0.9")
                .header("Referer", "https://www.wanted.co.kr/")
                .timeout(Duration.ofSeconds(15))
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            log.warn("Wanted API returned status {}: {}", response.statusCode(), url);
            return null;
        }

        return response.body();
    }

    private String buildUrl(String keyword, String career, String location, int page, int perPage) {
        StringBuilder sb = new StringBuilder(API_BASE);
        sb.append("?country=kr");
        sb.append("&job_sort=job.latest_order");
        sb.append("&page=").append(page);
        sb.append("&per_page=").append(perPage);

        if (!keyword.isEmpty()) {
            sb.append("&query=").append(URLEncoder.encode(keyword, StandardCharsets.UTF_8));
        }

        if (!career.isEmpty()) {
            String years = mapCareerToYears(career);
            if (!years.isEmpty()) {
                sb.append("&years=").append(years);
            }
        }

        if (!location.isEmpty()) {
            String locKey = mapLocationCode(location);
            if (!locKey.isEmpty()) {
                sb.append("&locations=").append(locKey);
            } else {
                sb.append("&locations=all");
            }
        } else {
            sb.append("&locations=all");
        }

        return sb.toString();
    }

    /**
     * 경력 한글명을 원티드 years 파라미터 값으로 변환한다.
     *
     * @param career 경력 한글명 (신입, 1~3년, 3~5년, 5~10년, 10년이상)
     * @return 원티드 years 값 (0, 1, 3, 5, 10)
     */
    String mapCareerToYears(String career) {
        return switch (career) {
            case "신입" -> "0";
            case "1~3년" -> "1";
            case "3~5년" -> "3";
            case "5~10년" -> "5";
            case "10년이상" -> "10";
            default -> "";
        };
    }

    /**
     * 지역 한글명을 원티드 locations 키로 변환한다.
     *
     * @param location 지역 한글명 (서울, 경기, 인천, 부산, 대구, 대전, 광주, 세종, 강원, 제주)
     * @return 원티드 locations 키 (seoul, gyeonggi, incheon, busan, ...)
     */
    String mapLocationCode(String location) {
        return switch (location) {
            case "서울" -> "seoul";
            case "경기" -> "gyeonggi";
            case "인천" -> "incheon";
            case "부산" -> "busan";
            case "대구" -> "daegu";
            case "대전" -> "daejeon";
            case "광주" -> "gwangju";
            case "세종" -> "sejong";
            case "강원" -> "gangwon";
            case "제주" -> "jeju";
            default -> "";
        };
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
