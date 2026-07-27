package com.scraper.platform.crawler;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.scraper.platform.model.CrawlSiteConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Component
public class JobkoreaCrawler implements SiteCrawler {

    private static final String SEARCH_URL = "https://www.jobkorea.co.kr/Search/";
    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String getSiteName() {
        return "jobkorea";
    }

    @Override
    public List<Map<String, String>> search(CrawlSiteConfig siteConfig) throws Exception {
        String paramValues = siteConfig.getParamValues();
        Map<String, String> params = parseParams(paramValues);
        String keyword = params.getOrDefault("keyword", "");

        if (keyword.isEmpty()) {
            log.warn("Jobkorea search requires keyword");
            return Collections.emptyList();
        }

        String url = SEARCH_URL + "?stext=" + URLEncoder.encode(keyword, StandardCharsets.UTF_8);
        log.info("Jobkorea search URL: {}", url);

        String html = fetchWithCurl(url);
        log.info("Fetched HTML size: {}", html.length());

        List<Map<String, String>> jobs = parseRscPayload(html);
        log.info("Parsed {} jobs from jobkorea", jobs.size());

        return jobs;
    }

    private String fetchWithCurl(String url) throws IOException, InterruptedException {
        ProcessBuilder pb = new ProcessBuilder(
            "curl", "-s", "-L", "--max-time", "30", "--compressed",
            "-H", "User-Agent: Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
            "-H", "Accept-Language: ko-KR,ko;q=0.9",
            "-H", "Accept: text/html,application/xhtml+xml",
            url
        );
        pb.redirectErrorStream(true);
        Process process = pb.start();
        byte[] bytes = process.getInputStream().readAllBytes();
        boolean finished = process.waitFor(35, TimeUnit.SECONDS);
        if (!finished) { process.destroyForcibly(); throw new IOException("curl timed out"); }
        if (process.exitValue() != 0) throw new IOException("curl failed with exit code " + process.exitValue());
        return new String(bytes, StandardCharsets.UTF_8);
    }

    private List<Map<String, String>> parseRscPayload(String html) {
        List<Map<String, String>> jobs = new ArrayList<>();

        // Push block 캡처 — 비탐욕적 매칭
        Pattern pushPattern = Pattern.compile("self\\.__next_f\\.push\\(\\[1,\"(.*?)\"\\]\\)", Pattern.DOTALL);
        Matcher pushMatcher = pushPattern.matcher(html);

        int pushCount = 0;
        while (pushMatcher.find()) {
            pushCount++;
            String raw = pushMatcher.group(1);
            String payload = raw.replace("\\\"", "\"").replace("\\\\", "\\").replace("\\n", "\n");

            if (!payload.contains("postingCompanyName")) continue;

            log.info("Push block {} has postingCompanyName, payload size={}", pushCount, payload.length());

            // postingCompanyName 주변 JSON 객체들을 파싱
            // 방법: "id":"숫자" 패턴을 찾아 각각 주변 JSON을 추출
            Pattern idPattern = Pattern.compile("\\{\"id\":\"(\\d{5,})\"");
            Matcher idMatcher = idPattern.matcher(payload);
            int found = 0;

            while (idMatcher.find()) {
                int start = idMatcher.start();
                // 이 위치에서부터 JSON 객체 닫힘까지 탐색
                int depth = 0;
                int end = -1;
                boolean inStr = false;
                boolean esc = false;
                for (int j = start; j < payload.length(); j++) {
                    char c = payload.charAt(j);
                    if (esc) { esc = false; continue; }
                    if (c == '\\') { esc = true; continue; }
                    if (c == '"') { inStr = !inStr; continue; }
                    if (inStr) continue;
                    if (c == '{') depth++;
                    else if (c == '}') { depth--; if (depth == 0) { end = j; break; } }
                }
                if (end < 0) continue;

                String objStr = payload.substring(start, end + 1);
                try {
                    JsonNode node = objectMapper.readTree(objStr);
                    String id = node.path("id").asText("");
                    String title = node.path("title").asText("");
                    String company = node.path("postingCompanyName").asText("");
                    if (title.isEmpty() || company.isEmpty()) continue;

                    String location = node.path("_internal_featureLocationCode").asText("");
                    if (location.isEmpty()) {
                        JsonNode areaCodes = node.path("areaCodeList");
                        if (areaCodes.isArray() && areaCodes.size() > 0) {
                            location = areaCodes.get(0).asText("");
                        }
                    }

                    String career = "";
                    String careerType = node.path("careerType").asText("");
                    int careerRange = node.path("careerRange").asInt(0);
                    switch (careerType) {
                        case "1": career = "신입"; break;
                        case "2": career = careerRange > 0 ? careerRange + "년 이상" : "경력"; break;
                        case "3": career = "경력무관"; break;
                        default: career = careerType.isEmpty() ? "" : "경력무관";
                    }

                    String tech = node.path("_internal_featureToolCode").asText("");

                    String deadline = "";
                    JsonNode appPeriod = node.path("applicationPeriod");
                    if (!appPeriod.isMissingNode()) {
                        String endStr = appPeriod.path("end").asText("");
                        if (!endStr.isEmpty() && endStr.length() >= 10) {
                            deadline = endStr.substring(0, 10);
                        }
                    }

                    Map<String, String> job = new HashMap<>();
                    job.put("title", decodeUnicode(title));
                    job.put("position", decodeUnicode(title));
                    job.put("company", decodeUnicode(company));
                    job.put("url", "https://www.jobkorea.co.kr/Recruit/GI_Read/" + id);
                    job.put("tech", tech);
                    job.put("location", location);
                    job.put("career", career);
                    job.put("deadline", deadline);

                    boolean dup = jobs.stream().anyMatch(j -> j.get("url").equals(job.get("url")));
                    if (!dup) { jobs.add(job); found++; }
                } catch (Exception ignored) {}
            }
            log.info("Push block {}: extracted {} jobs", pushCount, found);
        }

        return jobs;
    }

    private String decodeUnicode(String s) {
        if (s == null) return "";
        return s.replace("\\u0022", "\"").replace("\\u0026", "&")
                .replace("\\u003C", "<").replace("\\u003E", ">")
                .replace("&amp;", "&").replace("&#x27;", "'");
    }

    private Map<String, String> parseParams(String paramValues) {
        if (paramValues == null || paramValues.isEmpty()) return new HashMap<>();
        try {
            JsonNode node = objectMapper.readTree(paramValues);
            Map<String, String> params = new HashMap<>();
            node.fields().forEachRemaining(e -> params.put(e.getKey(), e.getValue().asText()));
            return params;
        } catch (Exception e) {
            log.error("Failed to parse paramValues: {}", paramValues, e);
            return new HashMap<>();
        }
    }
}
