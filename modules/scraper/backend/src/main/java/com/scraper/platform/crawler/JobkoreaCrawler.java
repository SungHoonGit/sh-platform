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
        String career = params.getOrDefault("career", "");
        String location = params.getOrDefault("location", "");

        if (keyword.isEmpty()) {
            log.warn("Jobkorea search requires keyword");
            return Collections.emptyList();
        }

        String url = SEARCH_URL + "?stext=" + URLEncoder.encode(keyword, StandardCharsets.UTF_8);
        log.info("Jobkorea search URL: {}", url);

        String html = fetchWithCurl(url);
        log.info("Fetched HTML size: {}", html.length());

        List<Map<String, String>> jobs = parseRscPayload(html, keyword);
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
            throw new IOException("curl timed out for URL: " + url);
        }

        int exitCode = process.exitValue();
        if (exitCode != 0) {
            throw new IOException("curl failed with exit code " + exitCode);
        }

        return new String(bytes, StandardCharsets.UTF_8);
    }

    /**
     * Next.js RSC payload에서 채용 데이터를 추출한다.
     * self.__next_f.push() 호출 내부의 JSON content 배열을 파싱한다.
     */
    private List<Map<String, String>> parseRscPayload(String html, String keyword) {
        List<Map<String, String>> jobs = new ArrayList<>();

        // self.__next_f.push() 호출에서 RSC 데이터 추출
        Pattern pushPattern = Pattern.compile("self\\.__next_f\\.push\\(\\[1,\"(.*?)\"\\]\\)", Pattern.DOTALL);
        Matcher pushMatcher = pushPattern.matcher(html);

        while (pushMatcher.find()) {
            String payload = pushMatcher.group(1);
            // 이스케이프된 문자열 복원
            payload = payload.replace("\\\"", "\"").replace("\\\\", "\\").replace("\\n", "\n");

            // content 배열에서 채용 공고 추출
            if (payload.contains("\"content\":") && payload.contains("\"title\":")) {
                try {
                    parseContentArray(payload, jobs);
                } catch (Exception e) {
                    log.debug("Failed to parse RSC content chunk", e);
                }
            }
        }

        return jobs;
    }

    private void parseContentArray(String payload, List<Map<String, String>> jobs) {
        // "content":[...] 블록 추출
        int contentIdx = payload.indexOf("\"content\":[");
        if (contentIdx < 0) return;

        int arrayStart = contentIdx + "\"content\":".length();
        int depth = 0;
        int arrayEnd = -1;
        for (int i = arrayStart; i < payload.length(); i++) {
            char c = payload.charAt(i);
            if (c == '[') depth++;
            else if (c == ']') {
                depth--;
                if (depth == 0) {
                    arrayEnd = i;
                    break;
                }
            }
        }

        if (arrayEnd < 0) return;

        String contentArray = payload.substring(arrayStart, arrayEnd + 1);

        // 개별 채용 공고 객체 추출 - {"id":...} 패턴
        Pattern jobPattern = Pattern.compile("\\{\"id\":\"(\\d+)\"[^}]*\"title\":\"([^\"]+)\"[^}]*\"postingCompanyName\":\"([^\"]+)\"", Pattern.DOTALL);
        Matcher jobMatcher = jobPattern.matcher(contentArray);

        while (jobMatcher.find()) {
            String id = jobMatcher.group(1);
            String title = jobMatcher.group(2);
            String company = jobMatcher.group(3);

            Map<String, String> job = new HashMap<>();
            job.put("title", decodeUnicode(title));
            job.put("position", decodeUnicode(title));
            job.put("company", decodeUnicode(company));
            job.put("url", "https://www.jobkorea.co.kr/Recruit/GI_Read/" + id);
            job.put("tech", "");
            job.put("location", "");
            job.put("career", "");

            // 중복 체크
            boolean duplicate = jobs.stream().anyMatch(j -> j.get("url").equals(job.get("url")));
            if (!duplicate) {
                jobs.add(job);
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
