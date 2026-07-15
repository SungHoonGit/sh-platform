package com.scraper.platform.crawler;

import com.scraper.platform.model.CrawlSiteConfig;
import java.util.List;
import java.util.Map;

public interface SiteCrawler {
    
    String getSiteName();
    
    List<Map<String, String>> search(CrawlSiteConfig siteConfig) throws Exception;
    
    default String buildMdContent(List<Map<String, String>> jobs, String keyword, String siteName) {
        StringBuilder sb = new StringBuilder();
        sb.append("# ").append(java.time.LocalDate.now()).append(" ").append(keyword).append(" 채용공고\n\n");
        
        for (Map<String, String> job : jobs) {
            sb.append("## ").append(job.getOrDefault("company", "Unknown")).append("\n");
            sb.append("- 포지션: ").append(job.getOrDefault("position", "")).append("\n");
            sb.append("- 경력: ").append(job.getOrDefault("career", "")).append("\n");
            sb.append("- 기술: ").append(job.getOrDefault("tech", "")).append("\n");
            sb.append("- 지역: ").append(job.getOrDefault("location", "")).append("\n");
            sb.append("- 링크: ").append(job.getOrDefault("url", "")).append("\n\n");
        }
        
        return sb.toString();
    }
}
