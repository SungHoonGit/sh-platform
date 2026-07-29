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
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class SaraminCrawler implements SiteCrawler {

    private static final String BASE_URL = "https://www.saramin.co.kr/zf_user/search";
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private final SiteSearchMapper siteSearchMapper;

    @Override
    public String getSiteName() {
        return "saramin";
    }

    @Override
    public List<Map<String, String>> search(CrawlSiteConfig siteConfig) throws Exception {
        String paramValues = siteConfig.getParamValues();

        // SiteSearchMapper를 사용하여 표준 파라미터를 사이트별 URL 파라미터로 변환
        Map<String, String> siteParams = siteSearchMapper.toSiteParams(getSiteName(), paramValues);

        List<Map<String, String>> allJobs = new ArrayList<>();

        // 최대 3페이지 (150건) 수집
        for (int page = 1; page <= 3; page++) {
            String url = buildUrl(siteParams, page);
            log.info("Saramin crawl URL (page {}): {}", page, url);
            
            String html = fetchWithCurl(url);
            log.info("Fetched HTML size: {}", html.length());
            
            Document doc = Jsoup.parse(html);
            log.info("Page {}: HTML size={}", page, html.length());
            
            List<Map<String, String>> pageJobs = parseJobs(doc);
            if (pageJobs.isEmpty()) {
                log.info("No more jobs at page {}, stopping", page);
                break;
            }
            allJobs.addAll(pageJobs);
            
            // Rate limit
            Thread.sleep(1000);
        }
        
        log.info("Total Saramin jobs: {}", allJobs.size());
        return allJobs;
    }

    /**
     * curl을 사용하여 HTML을 가져옴.
     * Jsoup의 HTTP 클라이언트는 사람인 anti-bot에 탐지되어 내용이 없는 HTML을 반환함.
     * curl은 정상적인 브라우저 요청으로 인식되어 전체 채용공고 목록을 반환함.
     */
    private String fetchWithCurl(String url) throws IOException, InterruptedException {
        ProcessBuilder pb = new ProcessBuilder(
            "curl", "-s", "-L",
            "--max-time", "30",
            "--compressed",
            "-H", "User-Agent: Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
            "-H", "Accept-Language: ko-KR,ko;q=0.9,en-US;q=0.8,en;q=0.7",
            "-H", "Accept: text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8",
            "-H", "Accept-Encoding: gzip, deflate, br",
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

    private String buildUrl(Map<String, String> siteParams, int page) {
        StringBuilder sb = new StringBuilder(BASE_URL);
        sb.append("?search_area=main&cat_kewd=235");

        for (Map.Entry<String, String> entry : siteParams.entrySet()) {
            sb.append("&").append(entry.getKey()).append("=")
              .append(URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8));
        }

        if (page > 1) {
            sb.append("&page=").append(page);
        }

        return sb.toString();
    }

    private List<Map<String, String>> parseJobs(Document doc) {
        List<Map<String, String>> jobs = new ArrayList<>();

        Elements items = doc.select("div.area_job");
        log.info("Found {} items on page", items.size());

        for (Element item : items) {
            try {
                Element corp = item.nextElementSibling();
                Map<String, String> job = parseItem(item, corp);
                if (job != null && !job.isEmpty()) {
                    jobs.add(job);
                }
            } catch (Exception e) {
                log.debug("Failed to parse job item", e);
            }
        }

        return jobs;
    }

    private Map<String, String> parseItem(Element item, Element corp) {
        Map<String, String> job = new HashMap<>();

        // 회사명 (div.area_corp strong.corp_name a)
        if (corp != null) {
            Element companyEl = corp.selectFirst("strong.corp_name a");
            if (companyEl != null) {
                job.put("company", companyEl.text().trim());
            }
        }

        // 제목 + 링크 (h2.job_tit a)
        Element titleEl = item.selectFirst("h2.job_tit a");
        if (titleEl != null) {
            String title = titleEl.attr("title");
            if (title.isEmpty()) title = titleEl.text().trim();
            job.put("title", title);
            job.put("position", title);
            String href = titleEl.attr("href");
            if (!href.startsWith("http")) {
                href = "https://www.saramin.co.kr" + href;
            }
            job.put("url", href);
        }

        // 경력/지역 (div.job_condition spans)
        Element cond = item.selectFirst("div.job_condition");
        if (cond != null) {
            Elements spans = cond.select("> span");
            // 첫번째 span: 지역 (a 태그들)
            if (spans.size() > 0) {
                Element locSpan = spans.get(0);
                StringBuilder loc = new StringBuilder();
                for (Element a : locSpan.select("a")) {
                    if (loc.length() > 0) loc.append(" ");
                    loc.append(a.text().trim());
                }
                job.put("location", loc.toString());
            }
            // 두번째 span: 경력
            if (spans.size() > 1) {
                job.put("career", spans.get(1).text().trim());
            }
        }

        // 기술스택 (div.job_sector a)
        Element sector = item.selectFirst("div.job_sector");
        if (sector != null) {
            StringBuilder tech = new StringBuilder();
            for (Element a : sector.select("a")) {
                String text = a.text().trim();
                if (!text.isEmpty()) {
                    if (tech.length() > 0) tech.append(", ");
                    tech.append(text);
                }
            }
            job.put("tech", tech.toString());
        }

        // 마감일 (div.job_date span.date)
        Element deadlineEl = item.selectFirst("div.job_date span.date");
        if (deadlineEl != null) {
            job.put("deadline", deadlineEl.text().trim());
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
            node.fields().forEachRemaining(entry -> {
                params.put(entry.getKey(), entry.getValue().asText());
            });
            return params;
        } catch (Exception e) {
            log.error("Failed to parse paramValues: {}", paramValues, e);
            return new HashMap<>();
        }
    }
}
