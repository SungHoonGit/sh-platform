package com.scraper.platform.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

/**
 * Viewer용 채용공고 응답 DTO.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobPostingResponse {

    private List<JobItem> jobs;
    private int total;
    private int page;
    private int size;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class JobItem {
        private Long id;
        private String site;
        private String company;
        private String position;
        private String career;
        private String tech;
        private String location;
        private String deadline;
        private String url;
        private LocalDate crawledAt;
    }
}
