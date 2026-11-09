package com.scraper.platform.ai;

import java.util.List;
import java.util.Map;

public record CompanyAnalysisResult(
        String companyName,
        String markdown,
        Stats stats,
        List<Citation> citations,
        String provider,
        String model,
        String generatedAt
) {
    public record Stats(long totalCount, String recentCrawledAt, Map<String, Long> siteCounts) {
    }

    public record Citation(String position, String siteName, String crawledAt, String url) {
    }
}
