package com.scraper.platform.controller;

import com.scraper.platform.api.dto.SearchRequest;
import com.scraper.platform.api.dto.SearchResponse;
import com.scraper.platform.service.SearchService;
import com.shplatform.common.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/search")
@RequiredArgsConstructor
@Tag(name = "Search", description = "실시간 검색 API")
public class SearchController {

    private final SearchService searchService;

    @PostMapping
    @Operation(summary = "실시간 검색", description = "사용자 조건으로 즉시 크롤링하여 결과를 반환합니다")
    public ResponseEntity<ApiResponse<SearchResponse>> search(@RequestBody SearchRequest request) {
        SearchResponse response = searchService.search(request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
