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
            "curl", "-s", "-L",
            "--max-time", "30",
            "--compressed",
            "-H", "User-Agent: Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
            "-H", "Accept-Language: ko-KR,ko;q=0.9",
            "-H", "Accept: text/html,application/xhtml+xml",
            url
        );
        pb.redirectErrorStream(true);

        Process process = pb.start();
        byte[] bytes = process.getInputStream().readAllBytes();
        boolean finished = process.waitFor(35, TimeUnit.SECONDS);

        if (!finished) {
            process.destroyForcibly();
            throw new IOException("curl timed out for URL: + url");
        }

        int exitCode = process.exitValue();
        if (exitCode != 0) {
            throw new IOException("curl failed with exit code " + exitCode);
        }

        return new String(bytes, StandardCharsets.UTF_8);
    }

    private List<Map<String, String>> parseRscPayload(String html) {
        List<Map<String, String>> jobs = new ArrayList<>();

        Pattern pushPattern = Pattern.compile("self\\.__next_f\\.push\\(\\[1,\"(.*?)\"\\]\\)", Pattern.DOTALL);
        Matcher pushMatcher = pushPattern.matcher(html);

        while (pushMatcher.find()) {
            String payload = pushMatcher.group(1);
            payload = payload.replace("\\\"", "\"").replace("\\\\", "\\").replace("\\n", "\n");

            parseJsonObjects(payload, jobs);
        }

        log.info("Jobkorea RSC: {} jobs parsed total", jobs.size());
        return jobs;
    }

    private void parseJsonObjects(String text, List<Map<String, String>> jobs) {
        int i = 0;
        while (i < text.length() - 20) {
            if (text.charAt(i) != '{' || text.charAt(i + 1) != '"') { i++; continue; }

            int objStart = i;
            int objDepth = 0;
            int objEnd = -1;
            boolean inString = false;
            boolean escaped = false;
            for (int j = i; j < text.length(); j++) {
                char c = text.charAt(j);
                if (escaped) { escaped = false; continue; }
                if (c == '\\') { escaped = true; continue; }
                if (c == '"') { inString = !inString; continue; }
                if (inString) continue;
                if (c == '{') objDepth++;
                else if (c == '}') {
                    objDepth--;
                    if (objDepth == 0) { objEnd = j; break; }
                }
            }
            if (objEnd < 0) break;

            String objStr = text.substring(objStart, objEnd + 1);
            i = objEnd + 1;

            if (objStr.length() < 50) continue;

            try {
                JsonNode node = objectMapper.readTree(objStr);
                String id = node.path("id").asText("");
                if (id.isEmpty() || id.length() < 5) continue;

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

                boolean duplicate = jobs.stream().anyMatch(j -> j.get("url").equals(job.get("url")));
                if (!duplicate) {
                    jobs.add(job);
                }
            } catch (Exception ignored) {
            }
        }
    }

    private String decodeUnicode(String s) {
        if (s == null) return "";
        try {
            return s.replace("\\u0022", "\"")
                    .replace("\\u0026", "&")
                    .replace("\\u003C", "<")
                    .replace("\\u003E", ">")
                    .replace("&amp;", "&")
                    .replace("&#x27;", "'");
        } catch (Exception e) {
            return s;
        }
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
