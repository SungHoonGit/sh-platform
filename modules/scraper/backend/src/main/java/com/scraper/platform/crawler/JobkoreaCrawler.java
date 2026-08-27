package com.scraper.platform.crawler;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.scraper.platform.model.CrawlSiteConfig;
import com.scraper.platform.service.SiteSearchMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.time.Duration;

/**
 * 잡코리아 공식 검색 API를 호출해 채용공고를 수집한다.
 * <p>
 * 프론트(Next.js App Router)가 실제 사용하는 {@code POST /Search/api/display/v2/jobs} 엔드포인트를 사용한다.
 * 기존 HTML({@code /Search/}) 파싱 방식과 달리 locationList/careerList/careerMin/careerMax 필터가
 * 서버사이드에서 정상 동작하며, 페이지당 최대 100건을 JSON으로 반환한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JobkoreaCrawler implements SiteCrawler {

    private static final String API_URL = "https://www.jobkorea.co.kr/Search/api/display/v2/jobs";
    private static final int MAX_PAGES = 10;
    private static final int PAGE_SIZE = 100;
    private static final int PAGE_DELAY_MS = 1500;
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final SiteSearchMapper siteSearchMapper;
    private final JobkoreaAreaMapper areaMapper;

    @Override
    public String getSiteName() {
        return "jobkorea";
    }

    @Override
    public List<Map<String, String>> search(CrawlSiteConfig siteConfig) throws Exception {
        Map<String, String> params = parseParams(siteConfig.getParamValues());
        String keyword = params.getOrDefault("keyword", "");
        String career = params.getOrDefault("career", "");
        String location = params.getOrDefault("location", "");
        String careerMinStr = params.get("careerMin");
        String careerMaxStr = params.get("careerMax");
        Integer careerMin = careerMinStr != null ? Integer.parseInt(careerMinStr) : null;
        Integer careerMax = careerMaxStr != null ? Integer.parseInt(careerMaxStr) : null;
        Map<String, String> siteParams = siteSearchMapper.toSiteParams(getSiteName(), params);

        boolean careerFiltered = isCareerFilterActive(career, careerMin, careerMax);
        boolean locationFiltered = !locationCodes(location, siteParams).isEmpty();

        Set<String> seenIds = new HashSet<>();
        List<Map<String, String>> allJobs = new ArrayList<>();
        int totalCount = -1;

        for (int page = 1; page <= MAX_PAGES; page++) {
            String body = buildBody(keyword, career, careerMin, careerMax, location, siteParams, page);
            log.info("Jobkorea crawl API (page {}): {}", page, API_URL);

            String json = fetchWithCurl(body);
            if (json == null || json.isBlank()) {
                log.warn("Empty response at page {}, stopping", page);
                break;
            }

            JsonNode root = OBJECT_MAPPER.readTree(json);
            totalCount = root.path("totalElements").asInt(-1);
            JsonNode content = root.path("content");
            if (!content.isArray() || content.isEmpty()) {
                log.info("No more jobs at page {}, stopping", page);
                break;
            }

            List<Map<String, String>> pageJobs = parseJobs(content, careerFiltered, locationFiltered, location);
            for (Map<String, String> job : pageJobs) {
                String id = job.get("id");
                if (id != null && !seenIds.add(id)) {
                    continue;
                }
                allJobs.add(job);
            }

            int totalPages = root.path("totalPages").asInt(0);
            if (page >= totalPages) {
                log.info("Reached last page {} of {} total pages, stopping", page, totalPages);
                break;
            }

            Thread.sleep(PAGE_DELAY_MS);
        }

        log.info("Total Jobkorea jobs after id dedup: {} (unique, totalCount={})", allJobs.size(), totalCount);
        return allJobs;
    }

    private String buildBody(String keyword, String career, Integer careerMin, Integer careerMax,
                             String location, Map<String, String> siteParams, int page) throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("pageSize", PAGE_SIZE);
        body.put("page", page - 1);
        body.put("sortProperty", "1");
        body.put("sortDirection", "DESC");
        body.put("keyword", keyword == null ? "" : keyword);

        body.put("locationList", locationCodes(location, siteParams));

        appendCareerParams(body, career, careerMin, careerMax);

        body.put("jobClassificationCodeList", List.of());
        body.put("jobClassificationSubCodeList", List.of());
        body.put("industryCodeList", List.of());
        body.put("industrySubCodeList", List.of());
        body.put("companyTypeList", List.of());
        body.put("educationCodeList", List.of());
        body.put("employmentTypeList", List.of());
        body.put("excludeKeywordList", List.of());
        body.put("designationCodeList", List.of());
        body.put("filterList", List.of());
        body.put("benefitCodeList", List.of());
        body.put("payType", "");
        body.put("payMin", 0);
        body.put("payMax", 0);
        body.put("onePick", "");
        body.put("period", "");
        body.put("featureType", "");
        body.put("deviceType", "PC");

        return OBJECT_MAPPER.writeValueAsString(body);
    }

    /**
     * 경력 조건을 API 요청 본문의 careerList/careerMin/careerMax로 변환한다.
     */
    private void appendCareerParams(Map<String, Object> body, String career, Integer careerMin, Integer careerMax) {
        if (careerMin != null || careerMax != null) {
            boolean active = careerMin != null && careerMin > 0;
            body.put("careerList", active ? List.of("2") : List.of());
            body.put("careerMin", careerMin != null ? String.valueOf(careerMin) : "");
            body.put("careerMax", careerMax != null ? String.valueOf(careerMax) : "");
            return;
        }
        if (career == null || career.isEmpty() || career.equals("전체") || career.equals("경력무관")) {
            body.put("careerList", List.of());
            body.put("careerMin", "");
            body.put("careerMax", "");
            return;
        }
        String type = mapCareerType(career);
        body.put("careerList", type.isEmpty() ? List.of() : List.of(type));

        int[] range = careerRange(career);
        body.put("careerMin", range[0] < 0 ? "" : String.valueOf(range[0]));
        body.put("careerMax", range[1] < 0 ? "" : String.valueOf(range[1]));
    }

    private boolean isCareerFilterActive(String career, Integer careerMin, Integer careerMax) {
        if (careerMin != null || careerMax != null) {
            return careerMin != null && careerMin > 0;
        }
        return career != null && !career.isEmpty() && !career.equals("전체") && !career.equals("경력무관");
    }

    private String fetchWithCurl(String body) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(URI.create(API_URL))
            .header("Content-Type", "application/json")
            .header("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
            .header("Accept-Language", "ko-KR,ko;q=0.9")
            .header("Accept", "application/json")
            .header("Referer", "https://www.jobkorea.co.kr/Search/")
            .timeout(Duration.ofSeconds(30))
            .POST(HttpRequest.BodyPublishers.ofString(body))
            .build();
        HttpResponse<String> resp = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build()
            .send(request, HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() >= 400) {
            throw new IOException("HTTP " + resp.statusCode() + " for API: " + API_URL);
        }
        return resp.body();
    }

    private List<Map<String, String>> parseJobs(JsonNode content, boolean careerFiltered, boolean locationFiltered, String location) {
        List<Map<String, String>> jobs = new ArrayList<>();
        for (JsonNode node : content) {
            try {
                Map<String, String> job = parseJob(node, careerFiltered, locationFiltered, location);
                if (job != null && !job.isEmpty()) {
                    jobs.add(job);
                }
            } catch (Exception e) {
                log.debug("Failed to parse job item", e);
            }
        }
        return jobs;
    }

    private Map<String, String> parseJob(JsonNode node, boolean careerFiltered, boolean locationFiltered, String location) {
        String id = node.path("id").asText("");
        if (id.isEmpty()) {
            return null;
        }
        Map<String, String> job = new HashMap<>();
        job.put("id", id);

        if (careerFiltered) {
            job.put("careerFiltered", "true");
        }
        if (locationFiltered) {
            job.put("locationFiltered", "true");
        }

        String title = node.path("title").asText("");
        job.put("title", title);
        job.put("position", title);

        String company = node.path("postingCompanyName").asText("");
        if (company.isEmpty()) {
            company = node.path("companyName").asText("");
        }
        job.put("company", company);

        job.put("career", careerText(node.path("careerType").asText(""), node.path("careerRange").asInt(0)));

        List<String> areaCodes = new ArrayList<>();
        node.path("areaCodeList").forEach(c -> areaCodes.add(c.asText()));
        job.put("location", areaMapper.toAreaText(areaCodes, location));

        String end = node.path("applicationPeriod").path("end").asText("");
        if (!end.isEmpty()) {
            job.put("deadline", end.length() >= 10 ? end.substring(0, 10) : end);
        }

        job.put("url", "https://www.jobkorea.co.kr/Recruit/GI_Read/" + id);

        // 기술 스택 추출 (_internal_featureToolCode + _internal_featureSkillCode)
        List<String> techs = new ArrayList<>();
        String toolCode = node.path("_internal_featureToolCode").asText("");
        if (!toolCode.isEmpty()) {
            for (String t : toolCode.split(",")) {
                String trimmed = t.trim();
                if (!trimmed.isEmpty()) techs.add(trimmed);
            }
        }
        if (techs.isEmpty()) {
            String skillCode = node.path("_internal_featureSkillCode").asText("");
            if (!skillCode.isEmpty()) {
                for (String t : skillCode.split(",")) {
                    String trimmed = t.trim();
                    if (!trimmed.isEmpty()) techs.add(trimmed);
                }
            }
        }
        if (!techs.isEmpty()) {
            job.put("tech", String.join(", ", techs));
        }

        return job;
    }

    /**
     * 잡코리아 careerType/careerRange를 한글 경력 텍스트로 변환한다.
     * (프론트 eL 함수와 동일한 형식)
     *
     * @param careerType  잡코리아 경력 타입 코드 (1:신입, 2:경력, 3:신입·경력, 그 외:경력무관)
     * @param careerRange 최소 경력 연수 (0 또는 100은 연수 미지정)
     * @return 한글 경력 텍스트 (예: 신입, 경력, 경력3년↑)
     */
    static String careerText(String careerType, int careerRange) {
        return switch (careerType) {
            case "1" -> "신입";
            case "2" -> (careerRange > 0 && careerRange < 100) ? "경력" + careerRange + "년↑" : "경력";
            case "3" -> (careerRange > 0 && careerRange < 100) ? "신입·경력" + careerRange + "년↑" : "신입·경력";
            default -> "경력무관";
        };
    }

    /**
     * 경력 한글명을 잡코리아 v2 API careerList 코드로 변환한다.
     *
     * @param career 경력 한글명 (신입, 경력, 1~3년, 3~5년, 5~10년, 10년이상)
     * @return 잡코리아 careerList 코드 (1:신입, 2:경력)
     */
    String mapCareerType(String career) {
        return switch (career) {
            case "신입" -> "1";
            case "경력", "1~3년", "3~5년", "5~10년", "10년이상" -> "2";
            default -> "";
        };
    }

    private int[] careerRange(String career) {
        return switch (career) {
            case "1~3년" -> new int[]{1, 3};
            case "3~5년" -> new int[]{3, 5};
            case "5~10년" -> new int[]{5, 10};
            case "10년이상" -> new int[]{10, -1};
            default -> new int[]{-1, -1};
        };
    }

    /**
     * 지역 한글명을 잡코리아 v2 API 지역 매핑코드로 변환한다.
     * (site_search_mapping DB 값이 우선이며, DB에 없을 때만 사용)
     *
     * @param location 지역 한글명 (서울, 경기, 인천, 부산, ...)
     * @return 잡코리아 지역 매핑코드 (서울:I000, 경기:B000, ...)
     */
    String mapLocationCode(String location) {
        return switch (location) {
            case "서울" -> "I000";
            case "경기" -> "B000";
            case "인천" -> "K000";
            case "부산" -> "H000";
            case "대구" -> "F000";
            case "대전" -> "G000";
            case "광주", "전남" -> "L000";
            case "세종" -> "1000";
            case "강원" -> "A000";
            case "제주" -> "N000";
            case "충남" -> "O000";
            case "충북" -> "P000";
            case "전북" -> "M000";
            case "경남" -> "C000";
            case "경북" -> "D000";
            case "울산" -> "J000";
            default -> "";
        };
    }

    /**
     * 지역 파라미터를 잡코리아 locationList 코드 목록으로 변환한다.
     * 다중 지역("서울,경기")은 각각 코드로 변환하여 목록으로 반환한다.
     * (site_search_mapping DB 값이 우선이며, DB에 없을 때만 하드코딩 매핑 사용)
     */
    private List<String> locationCodes(String location, Map<String, String> siteParams) {
        String code = siteParams.getOrDefault("local", "");
        if (!code.isEmpty()) {
            List<String> codes = new ArrayList<>();
            for (String part : code.split(",")) {
                if (!part.isBlank()) {
                    codes.add(part.trim());
                }
            }
            return codes;
        }
        if (location == null || location.isBlank()) {
            return List.of();
        }
        List<String> codes = new ArrayList<>();
        for (String part : location.split(",")) {
            String c = mapLocationCode(part.trim());
            if (!c.isEmpty()) {
                codes.add(c);
            }
        }
        return codes;
    }

    private Map<String, String> parseParams(String paramValues) {
        if (paramValues == null || paramValues.isEmpty()) {
            return new HashMap<>();
        }
        try {
            JsonNode node = OBJECT_MAPPER.readTree(paramValues);
            Map<String, String> params = new HashMap<>();
            node.fields().forEachRemaining(entry -> params.put(entry.getKey(), entry.getValue().asText()));
            return params;
        } catch (Exception e) {
            log.error("Failed to parse paramValues: {}", paramValues, e);
            return new HashMap<>();
        }
    }
}
