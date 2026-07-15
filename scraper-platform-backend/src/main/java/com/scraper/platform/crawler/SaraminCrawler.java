package com.scraper.platform.crawler;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.scraper.platform.model.CrawlSiteConfig;
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
public class SaraminCrawler implements SiteCrawler {

    private static final String BASE_URL = "https://www.saramin.co.kr/zf_user/jobs/list/job-category";
    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String getSiteName() {
        return "saramin";
    }

    @Override
    public List<Map<String, String>> search(CrawlSiteConfig siteConfig) throws Exception {
        String paramValues = siteConfig.getParamValues();
        Map<String, String> params = parseParams(paramValues);
        
        String keyword = params.getOrDefault("keyword", "");
        String career = params.getOrDefault("career", "");
        String location = params.getOrDefault("location", "");
        String jobType = params.getOrDefault("job_type", "");
        
        List<Map<String, String>> allJobs = new ArrayList<>();
        
        // 최대 3페이지 (150건) 수집
        for (int page = 1; page <= 3; page++) {
            String url = buildUrl(keyword, career, location, jobType, page);
            log.info("Saramin crawl URL (page {}): {}", page, url);
            
            String html = fetchWithCurl(url);
            log.info("Fetched HTML size: {}", html.length());
            
            Document doc = Jsoup.parse(html);
            log.info("Page {}: list_item={}", page, doc.select("div.list_item").size());
            
            List<Map<String, String>> pageJobs = parseJobs(doc, keyword);
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

    private String buildUrl(String keyword, String career, String location, String jobType, int page) {
        StringBuilder sb = new StringBuilder(BASE_URL);
        sb.append("?cat_kewd=235"); // IT 개발 카테고리
        
        if (!career.isEmpty()) {
            String careerCode = mapCareerCode(career);
            if (!careerCode.isEmpty()) {
                sb.append("&career_level=").append(careerCode);
            }
        }
        
        if (!keyword.isEmpty()) {
            sb.append("&stext=").append(URLEncoder.encode(keyword, StandardCharsets.UTF_8));
        }
        
        if (page > 1) {
            sb.append("&page=").append(page);
        }
        
        return sb.toString();
    }

    private String mapCareerCode(String career) {
        return switch (career) {
            case "신입" -> "1";
            case "경력" -> "2";
            case "1~3년" -> "3";
            case "3~5년" -> "5";
            case "5~10년" -> "8";
            case "10년이상" -> "12";
            default -> "";
        };
    }

    private List<Map<String, String>> parseJobs(Document doc, String keyword) {
        List<Map<String, String>> jobs = new ArrayList<>();
        
        Elements items = doc.select("div.list_item");
        log.info("Found {} items on page", items.size());
        
        for (Element item : items) {
            try {
                Map<String, String> job = parseItem(item);
                if (job != null && !job.isEmpty()) {
                    jobs.add(job);
                }
            } catch (Exception e) {
                log.debug("Failed to parse job item", e);
            }
        }
        
        return jobs;
    }

    private Map<String, String> parseItem(Element item) {
        Map<String, String> job = new HashMap<>();
        
        // 회사명
        Element companyEl = item.selectFirst("div.col.company_nm a.str_tit");
        if (companyEl == null) companyEl = item.selectFirst("a.str_tit");
        if (companyEl != null) {
            job.put("company", companyEl.text().trim());
        }
        
        // 제목 + 링크
        Element titleEl = item.selectFirst("div.job_tit a.str_tit");
        if (titleEl == null) titleEl = item.selectFirst("a.str_tit");
        if (titleEl != null) {
            job.put("title", titleEl.text().trim());
            job.put("position", titleEl.text().trim());
            String href = titleEl.attr("href");
            if (!href.startsWith("http")) {
                href = "https://www.saramin.co.kr" + href;
            }
            job.put("url", href);
        }
        
        // 경력
        Element careerEl = item.selectFirst("p.career");
        if (careerEl != null) {
            job.put("career", careerEl.text().trim());
        }
        
        // 학력
        Element eduEl = item.selectFirst("p.education");
        if (eduEl != null) {
            job.put("education", eduEl.text().trim());
        }
        
        // 기술스택
        Elements techEls = item.select("div.job_meta span.job_sector span");
        if (techEls.isEmpty()) techEls = item.select("span.job_sector");
        StringBuilder tech = new StringBuilder();
        for (Element t : techEls) {
            String text = t.text().trim();
            if (!text.isEmpty() && !text.equals(",")) {
                if (tech.length() > 0) tech.append(", ");
                tech.append(text);
            }
        }
        job.put("tech", tech.toString());
        
        // 지역
        Element locEl = item.selectFirst("p.work_place");
        if (locEl != null) {
            job.put("location", locEl.text().trim());
        }
        
        // 마감일
        Element deadlineEl = item.selectFirst("span.date");
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
