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
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class JobkoreaCrawler implements SiteCrawler {

    private static final String BASE_URL = "https://www.jobkorea.co.kr/recruit/joblist";
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

        String url = buildUrl(keyword, career, location);
        log.info("Jobkorea crawl URL: {}", url);

        // curl로 HTML을 가져옴 (Java HttpClient도 anti-bot에 차단될 수 있음)
        String html = fetchWithCurl(url);
        log.info("Fetched HTML size: {}", html.length());

        Document doc = Jsoup.parse(html);
        log.info("Page title: {}", doc.title());

        return parseJobs(doc, keyword);
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

    private String buildUrl(String keyword, String career, String location) {
        StringBuilder sb = new StringBuilder(BASE_URL);
        sb.append("?menucode=duty&dutyCtgr=1003101"); // IT 개발 직무

        if (!keyword.isEmpty()) {
            sb.append("&stext=").append(java.net.URLEncoder.encode(keyword, StandardCharsets.UTF_8));
        }

        if (!career.isEmpty()) {
            sb.append("&careerType=").append(mapCareerType(career));
        }

        return sb.toString();
    }

    private String mapCareerType(String career) {
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

    private List<Map<String, String>> parseJobs(Document doc, String keyword) {
        List<Map<String, String>> jobs = new ArrayList<>();

        Elements rows = doc.select("tr.devloopArea");
        log.info("Found {} job rows", rows.size());

        for (Element row : rows) {
            try {
                Map<String, String> job = parseRow(row);
                if (job != null && !job.isEmpty()) {
                    jobs.add(job);
                }
            } catch (Exception e) {
                log.debug("Failed to parse job row", e);
            }
        }

        return jobs;
    }

    private Map<String, String> parseRow(Element row) {
        Map<String, String> job = new HashMap<>();

        // 회사명
        Element coTd = row.selectFirst("td.tplCo");
        if (coTd != null) {
            Element companyA = coTd.selectFirst("a");
            if (companyA != null) {
                job.put("company", companyA.text().trim().replace("관심기업", "").trim());
            } else {
                job.put("company", coTd.text().trim().replace("관심기업", "").trim());
            }
        }

        // 제목 + 링크
        Element titTd = row.selectFirst("td.tplTit");
        if (titTd != null) {
            Element titleA = titTd.selectFirst("div.titBx a");
            if (titleA != null) {
                job.put("title", titleA.text().trim());
                job.put("position", titleA.text().trim());
                String href = titleA.attr("href");
                if (!href.startsWith("http")) {
                    href = "https://www.jobkorea.co.kr" + href;
                }
                job.put("url", href);
            }

            // 경력, 학력, 지역, 고용형태, 연봉
            Elements cells = titTd.select("p.etc span.cell");
            for (Element cell : cells) {
                String text = cell.text().trim();
                if (text.isEmpty()) continue;
                if (text.contains("신입") || text.contains("경력")) {
                    job.put("career", text);
                } else if (text.contains("대학") || text.contains("고졸") || text.contains("학력무관") || text.contains("석사") || text.contains("박사")) {
                    job.put("education", text);
                } else if (text.contains("서울") || text.contains("경기") || text.contains("부산") || text.contains("대전") || text.contains("대구") || text.contains("광주") || text.contains("인천") || text.contains("울산") || text.contains("세종") || text.contains("강원") || text.contains("충청") || text.contains("전라") || text.contains("경상") || text.contains("제주")) {
                    job.put("location", text);
                } else if (text.contains("정규직") || text.contains("계약직") || text.contains("인턴") || text.contains("파견") || text.contains("무기계약")) {
                    job.put("employmentType", text);
                } else if (text.contains("만원")) {
                    job.put("salary", text);
                }
            }

            // 기술스택
            Element dsc = titTd.selectFirst("p.dsc");
            if (dsc != null) {
                job.put("tech", dsc.text().trim());
            }
        }

        // 마감일
        Element dateTd = row.selectFirst("td.odd");
        if (dateTd != null) {
            String text = dateTd.text().trim();
            if (text.contains("~")) {
                String deadline = text.substring(text.lastIndexOf("~")).trim();
                job.put("deadline", deadline);
            }
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
