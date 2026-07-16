package com.scraper.platform.crawler;

import com.scraper.platform.model.CrawlSiteConfig;
import java.util.List;
import java.util.Map;

public interface SiteCrawler {
    
    String getSiteName();
    
    List<Map<String, String>> search(CrawlSiteConfig siteConfig) throws Exception;
    
    default String buildMdSection(List<Map<String, String>> jobs, String siteDisplayName) {
        StringBuilder sb = new StringBuilder();
        sb.append("## ").append(siteDisplayName).append(" (").append(jobs.size()).append("건)\n\n");

        for (Map<String, String> job : jobs) {
            sb.append("### ").append(job.getOrDefault("company", "Unknown")).append("\n");
            sb.append("- 포지션: ").append(job.getOrDefault("position", job.getOrDefault("title", ""))).append("\n");
            String career = job.getOrDefault("career", "");
            if (!career.isEmpty()) sb.append("- 경력: ").append(career).append("\n");
            String tech = job.getOrDefault("tech", "");
            if (!tech.isEmpty()) sb.append("- 기술: ").append(tech).append("\n");
            String loc = job.getOrDefault("location", "");
            if (!loc.isEmpty()) sb.append("- 지역: ").append(loc).append("\n");
            String deadline = job.getOrDefault("deadline", "");
            if (!deadline.isEmpty()) sb.append("- 마감: ").append(deadline).append("\n");
            sb.append("- 링크: ").append(job.getOrDefault("url", "")).append("\n\n");
        }

        return sb.toString();
    }
}
