package com.scraper.platform.controller;

import com.scraper.platform.model.CompanyBlacklist;
import com.scraper.platform.service.CompanyBlacklistService;
import com.shplatform.common.dto.ApiResponse;
import com.shplatform.common.security.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 개인 회사 블랙리스트 API. 차단된 회사의 공고는 목록에서 숨겨진다.
 */
@RestController
@RequestMapping("/company-blacklist")
@RequiredArgsConstructor
@Tag(name = "CompanyBlacklist", description = "개인 블랙리스트 관리")
public class CompanyBlacklistController {

    private final CompanyBlacklistService blacklistService;

    public record AddRequest(@NotBlank String companyName, String reason) {}

    @GetMapping
    @Operation(summary = "내 블랙리스트 목록")
    public ResponseEntity<ApiResponse<List<CompanyBlacklist>>> list() {
        return ResponseEntity.ok(ApiResponse.success(blacklistService.list(SecurityUtils.currentAccountId())));
    }

    @PostMapping
    @Operation(summary = "회사 차단", description = "중복 등록이면 사유를 갱신한다(멱등).")
    public ResponseEntity<ApiResponse<CompanyBlacklist>> add(@RequestBody AddRequest request) {
        var saved = blacklistService.add(SecurityUtils.currentAccountId(),
                request.companyName(), request.reason());
        return ResponseEntity.ok(ApiResponse.success(saved));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "차단 해제")
    public ResponseEntity<ApiResponse<Void>> remove(@PathVariable Long id) {
        blacklistService.remove(SecurityUtils.currentAccountId(), id);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
