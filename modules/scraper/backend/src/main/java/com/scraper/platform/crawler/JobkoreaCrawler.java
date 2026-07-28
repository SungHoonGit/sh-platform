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

    private static final String BASE_URL = "https://www.jobkorea.co.kr/recruit/joblist";
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private final SiteSearchMapper siteSearchMapper;

    @Override
    public String getSiteName() {
        return "jobkorea";
    }

    @Override
    public List<Map<String, String>> search(CrawlSiteConfig siteConfig) throws Exception {
        String paramValues = siteConfig.getParamValues();

        // SiteSearchMapper를 사용하여 표준 파라미터를 사이트별 URL 파라미터로 변환
        Map<String, String> siteParams = siteSearchMapper.toSiteParams(getSiteName(), paramValues);

        String url = buildUrl(siteParams);
        log.info("Jobkorea crawl URL: {}", url);

        // curl로 HTML을 가져옴 (Java HttpClient도 anti-bot에 차단될 수 있음)
        String html = fetchWithCurl(url);
        log.info("Fetched HTML size: {}", html.length());

        Document doc = Jsoup.parse(html);
        log.info("Page title: {}", doc.title());

        String keyword = siteParams.getOrDefault("stext", "");
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

    private String buildUrl(Map<String, String> siteParams) {
        StringBuilder sb = new StringBuilder(BASE_URL);
        sb.append("?menucode=duty&dutyCtgr=1003101"); // IT 개발 직무

        // SiteSearchMapper에서 변환된 파라미터를 URL에 추가
        for (Map.Entry<String, String> entry : siteParams.entrySet()) {
            sb.append("&").append(entry.getKey()).append("=")
              .append(java.net.URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8));
        }

        return sb.toString();
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
