package com.scraper.platform.controller;

import com.scraper.platform.api.dto.SearchMappingRequest;
import com.scraper.platform.api.dto.SearchMappingResponse;
import com.scraper.platform.service.SiteSearchMappingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/search-mappings")
@RequiredArgsConstructor
@Tag(name = "SearchMapping", description = "사이트 검색 매핑 관리 API")
public class SearchMappingController {

    private final SiteSearchMappingService searchMappingService;

    @GetMapping
    @Operation(summary = "검색 매핑 목록 조회", description = "전체 또는 사이트별 매핑(value_mapping)을 조회합니다")
    public ResponseEntity<List<SearchMappingResponse>> list(
            @RequestParam(value = "siteName", required = false) String siteName) {
        if (siteName == null || siteName.isBlank()) {
            return ResponseEntity.ok(searchMappingService.listAll());
        }
        return ResponseEntity.ok(searchMappingService.listBySite(siteName));
    }

    @GetMapping("/{id}")
    @Operation(summary = "검색 매핑 단건 조회", description = "매핑 ID로 조회합니다")
    public ResponseEntity<SearchMappingResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(searchMappingService.getById(id));
    }

    @PostMapping
    @Operation(summary = "검색 매핑 생성", description = "새 매핑 행을 등록합니다 (관리용)")
    public ResponseEntity<SearchMappingResponse> create(@RequestBody SearchMappingRequest request) {
        return ResponseEntity.ok(searchMappingService.create(request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "검색 매핑 수정", description = "value_mapping 등 매핑 정보를 수정합니다 (관리용)")
    public ResponseEntity<SearchMappingResponse> update(
            @PathVariable Long id,
            @RequestBody SearchMappingRequest request) {
        return ResponseEntity.ok(searchMappingService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "검색 매핑 삭제", description = "매핑 행을 삭제합니다 (관리용)")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        searchMappingService.delete(id);
        return ResponseEntity.ok().build();
    }
}