package com.scraper.platform.controller;

import com.scraper.platform.model.SiteDefinition;
import com.scraper.platform.model.SiteParameterDefinition;
import com.scraper.platform.service.SiteDefinitionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/sites")
@RequiredArgsConstructor
@Tag(name = "SiteDefinition", description = "사이트 관리 API")
public class SiteDefinitionController {

    private final SiteDefinitionService siteDefinitionService;

    @GetMapping
    @Operation(summary = "전체 사이트 조회", description = "지원하는 사이트 목록을 조회합니다")
    public ResponseEntity<List<SiteDefinition>> getAllSites() {
        return ResponseEntity.ok(siteDefinitionService.getAllSites());
    }

    @GetMapping("/enabled")
    @Operation(summary = "활성 사이트 조회", description = "활성화된 사이트만 조회합니다")
    public ResponseEntity<List<SiteDefinition>> getEnabledSites() {
        return ResponseEntity.ok(siteDefinitionService.getEnabledSites());
    }

    @GetMapping("/{id}")
    @Operation(summary = "사이트 상세 조회", description = "사이트 정보를 조회합니다")
    public ResponseEntity<SiteDefinition> getSiteById(@PathVariable Long id) {
        return ResponseEntity.ok(siteDefinitionService.getSiteById(id));
    }

    @GetMapping("/{id}/parameters")
    @Operation(summary = "사이트별 파라미터 조회", description = "사이트가 지원하는 파라미터 목록을 조회합니다")
    public ResponseEntity<List<SiteParameterDefinition>> getSiteParameters(@PathVariable Long id) {
        return ResponseEntity.ok(siteDefinitionService.getSiteParameters(id));
    }

    @PostMapping
    @Operation(summary = "사이트 생성", description = "새로운 사이트를 등록합니다")
    public ResponseEntity<SiteDefinition> createSite(@RequestBody SiteDefinition site) {
        return ResponseEntity.ok(siteDefinitionService.createSite(site));
    }

    @PutMapping("/{id}")
    @Operation(summary = "사이트 수정", description = "사이트 정보를 수정합니다")
    public ResponseEntity<SiteDefinition> updateSite(
            @PathVariable Long id,
            @RequestBody SiteDefinition site) {
        return ResponseEntity.ok(siteDefinitionService.updateSite(id, site));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "사이트 삭제", description = "사이트를 삭제합니다")
    public ResponseEntity<Void> deleteSite(@PathVariable Long id) {
        siteDefinitionService.deleteSite(id);
        return ResponseEntity.ok().build();
    }
}
