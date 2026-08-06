package com.scraper.platform.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CrawlLogGroupResponse {

    private LocalDate date;
    private int totalNewCount;
    private int totalRunCount;
    private List<CrawlRunGroup> runs;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CrawlRunGroup {
        private Long logId;
        private LocalDateTime startedAt;
        private String status;
        private int totalCount;
        private int newCount;
        private int siteCount;
        private List<String> siteNames;
    }
}
