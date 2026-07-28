package com.scraper.platform.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.Map;

@Schema(description = "실시간 검색 응답")
public record SearchResponse(
    @Schema(description = "전체 검색 결과 수")
    int total,

    @Schema(description = "검색 결과 목록")
    List<Map<String, String>> jobs,

    @Schema(description = "사이트별 결과 수")
    Map<String, Integer> siteCounts,

    @Schema(description = "검색 소요 시간 (ms)")
    long searchTime,

    @Schema(description = "에러가 발생한 사이트 목록")
    List<String> failedSites
) {
    public static SearchResponse of(int total, List<Map<String, String>> jobs,
                                     Map<String, Integer> siteCounts,
                                     long searchTime, List<String> failedSites) {
        return new SearchResponse(total, jobs, siteCounts, searchTime, failedSites);
    }
}
