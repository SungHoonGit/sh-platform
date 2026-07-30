package com.scraper.platform.crawler;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.scraper.platform.model.CrawlSiteConfig;
import com.scraper.platform.service.SiteSearchMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class JobkoreaCrawler implements SiteCrawler {

    private static final String BASE_URL = "https://www.jobkorea.co.kr/Search/";
    private static final int MAX_PAGES = 5;
    private static final int PAGE_DELAY_MS = 500;
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private final SiteSearchMapper siteSearchMapper;

    @Override
    public String getSiteName() {
        return "jobkorea";
    }

    @Override
    public List<Map<String, String>> search(CrawlSiteConfig siteConfig) throws Exception {
        String paramValues = siteConfig.getParamValues();
        Map<String, String> standardParams = parseParams(paramValues);
        Map<String, String> siteParams = siteSearchMapper.toSiteParams(getSiteName(), paramValues);

        String baseUrl = buildUrl(siteParams, standardParams);
        List<Map<String, String>> allJobs = new ArrayList<>();

        for (int page = 1; page <= MAX_PAGES; page++) {
            String url = page == 1 ? baseUrl : baseUrl + "&Page=" + page;
            log.info("Jobkorea crawl URL (page {}): {}", page, url);

            String html = fetchWithCurl(url);
            Document doc = Jsoup.parse(html);

            List<Map<String, String>> pageJobs = parseJobs(doc);
            allJobs.addAll(pageJobs);
            log.info("Page {}: {} jobs (total so far: {})", page, pageJobs.size(), allJobs.size());

            if (pageJobs.size() < 20) break;

            if (page < MAX_PAGES) {
                Thread.sleep(PAGE_DELAY_MS);
            }
        }

        log.info("Total Jobkorea jobs: {}", allJobs.size());
        return allJobs;
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

    private String buildUrl(Map<String, String> siteParams, Map<String, String> standardParams) {
        StringBuilder sb = new StringBuilder(BASE_URL);
        sb.append("?tabType=recruit");

        // SiteSearchMapper에서 변환된 파라미터 추가 (careerType은 제외 — 직접 처리)
        for (Map.Entry<String, String> entry : siteParams.entrySet()) {
            if ("careerType".equals(entry.getKey())) continue;
            sb.append("&").append(entry.getKey()).append("=")
              .append(java.net.URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8));
        }

        // careerType + careerMin/careerMax — SiteSearchMapper는 단순 매핑만 하므로 range는 직접 처리
        appendCareerParams(sb, standardParams.getOrDefault("career", ""));

        return sb.toString();
    }

    private void appendCareerParams(StringBuilder sb, String career) {
        if (career == null || career.isEmpty() || career.equals("전체") || career.equals("경력무관")) {
            return;
        }
        switch (career) {
            case "신입" -> sb.append("&careerType=0");
            case "경력" -> sb.append("&careerType=2");
            case "1~3년" -> sb.append("&careerType=2&careerMin=1&careerMax=3");
            case "3~5년" -> sb.append("&careerType=2&careerMin=3&careerMax=5");
            case "5~10년" -> sb.append("&careerType=2&careerMin=5&careerMax=10");
            case "10년이상" -> sb.append("&careerType=2&careerMin=10");
        }
    }

    /**
     * 경력 한글명을 잡코리아 careerType 코드로 변환한다.
     *
     * @param career 경력 한글명 (신입, 경력, 1~3년, 3~5년, 5~10년, 10년이상)
     * @return 잡코리아 careerType 코드 (new, career)
     */
    String mapCareerType(String career) {
        return switch (career) {
            case "신입" -> "new";
            case "경력" -> "career";
            case "1~3년" -> "career";
            case "3~5년" -> "career";
            case "5~10년" -> "career";
            case "10년이상" -> "career";
            default -> "";
        };
    }

    /**
     * 지역 한글명을 잡코리아 local 코드로 변환한다.
     *
     * @param location 지역 한글명 (서울, 경기, 인천, 부산, 대구, 대전, 광주, 세종, 강원, 제주, 충남, 충북, 전남, 전북, 경남, 경북)
     * @return 잡코리아 local 코드 (I000, I100, I200, ...)
     */
    String mapLocationCode(String location) {
        return switch (location) {
            case "서울" -> "I000";
            case "경기" -> "I100";
            case "인천" -> "I200";
            case "부산" -> "I300";
            case "대구" -> "I400";
            case "대전" -> "I500";
            case "광주" -> "I600";
            case "세종" -> "I700";
            case "강원" -> "I800";
            case "제주" -> "I900";
            case "충남" -> "I110";
            case "충북" -> "I120";
            case "전남" -> "I130";
            case "전북" -> "I140";
            case "경남" -> "I150";
            case "경북" -> "I160";
            default -> "";
        };
    }

    private List<Map<String, String>> parseJobs(Document doc) {
        List<Map<String, String>> jobs = new ArrayList<>();

        Elements cards = doc.select("div[data-sentry-component=CardJob]");
        log.info("Found {} job cards", cards.size());

        for (Element card : cards) {
            try {
                Map<String, String> job = parseCard(card);
                if (job != null && !job.isEmpty()) {
                    jobs.add(job);
                }
            } catch (Exception e) {
                log.debug("Failed to parse job card", e);
            }
        }

        return jobs;
    }

    private Map<String, String> parseCard(Element card) {
        Map<String, String> job = new HashMap<>();

        // 제목
        Element titleEl = card.selectFirst("a[data-sentry-component=Title] span");
        if (titleEl != null) {
            String title = titleEl.text().trim();
            job.put("title", title);
            job.put("position", title);
        }

        // 링크
        Element linkEl = card.selectFirst("a[data-sentry-component=Title]");
        if (linkEl != null) {
            String href = linkEl.attr("href");
            if (!href.startsWith("http")) {
                href = "https://www.jobkorea.co.kr" + href;
            }
            job.put("url", href);
        }

        // 회사명
        Element companyEl = card.selectFirst("span.mb-5 a span");
        if (companyEl != null) {
            job.put("company", companyEl.text().trim());
        }

        // GrayChip 칩들에서 위치, 기술 분야 추출
        Elements chips = card.select("div[data-sentry-component=GrayChip] span.text-gray900");
        for (int i = 0; i < chips.size(); i++) {
            String text = chips.get(i).text().trim();
            if (i == 0) {
                job.put("location", text);
            } else if (i == 1) {
                job.put("tech", text);
            }
        }

        // 경력
        Element careerEl = card.selectFirst("span.text-gray700.text-typo-c1-13");
        if (careerEl != null) {
            job.put("career", careerEl.text().trim());
        }

        return job;
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
