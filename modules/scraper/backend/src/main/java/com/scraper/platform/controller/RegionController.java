package com.scraper.platform.controller;

import com.scraper.platform.model.Region;
import com.scraper.platform.service.RegionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/regions")
@RequiredArgsConstructor
@Tag(name = "Region", description = "지역 마스터 API")
public class RegionController {

    private final RegionService regionService;

    @GetMapping
    @Operation(summary = "활성 지역 목록 조회", description = "필터에 사용할 시/도 목록을 표시 순서대로 반환합니다")
    public ResponseEntity<List<Region>> getActiveRegions() {
        return ResponseEntity.ok(regionService.getActiveRegions());
    }

    @GetMapping("/search")
    @Operation(summary = "지역 검색", description = "이름 부분 일치로 지역을 검색합니다 (최대 20건)")
    public ResponseEntity<List<Region>> searchRegions(
            @RequestParam("q") String query) {
        return ResponseEntity.ok(regionService.search(query));
    }

    @PostMapping
    @Operation(summary = "지역 생성", description = "지역 마스터를 등록합니다 (관리용)")
    public ResponseEntity<Region> createRegion(@RequestBody Region region) {
        return ResponseEntity.ok(regionService.create(region));
    }

    @PutMapping("/{id}")
    @Operation(summary = "지역 수정", description = "지역 마스터 정보를 수정합니다 (관리용)")
    public ResponseEntity<Region> updateRegion(
            @PathVariable Long id,
            @RequestBody Region region) {
        return ResponseEntity.ok(regionService.update(id, region));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "지역 삭제", description = "지역 마스터를 삭제합니다 (관리용)")
    public ResponseEntity<Void> deleteRegion(@PathVariable Long id) {
        regionService.delete(id);
        return ResponseEntity.ok().build();
    }
}