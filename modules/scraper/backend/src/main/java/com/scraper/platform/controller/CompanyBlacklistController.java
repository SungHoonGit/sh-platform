package com.scraper.platform.controller;

import com.scraper.platform.model.CompanyBlacklist;
import com.scraper.platform.service.BlockReasonService;
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
    private final BlockReasonService blockReasonService;

    public record AddRequest(@NotBlank String companyName, String reason, List<Long> reasonIds,
                             List<String> categoryNames) {}

    /** 기존 차단 항목 카테고리 수정 요청 (자유 메모는 보존) */
    public record UpdateRequest(List<Long> reasonIds, List<String> categoryNames) {}

    /** 차단 사유 마스터 응답 */
    public record BlockReasonResponse(Long id, String name, String category) {}

    @GetMapping("/reasons")
    @Operation(summary = "차단 카테고리 전체", description = "활성 카테고리(회사유형/사유) 전체를 다중 선택 UI 초기 로드에 사용한다.")
    public ResponseEntity<ApiResponse<List<BlockReasonResponse>>> listReasons() {
        var response = blockReasonService.listAll().stream()
                .map(r -> new BlockReasonResponse(r.getId(), r.getName(), r.getCategory()))
                .toList();
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/reasons/search")
    @Operation(summary = "차단 카테고리 검색", description = "마스터에서 활성 카테고리(회사유형/사유)를 검색해 자동완성/선택에 사용한다.")
    public ResponseEntity<ApiResponse<List<BlockReasonResponse>>> searchReasons(
            @RequestParam("q") String q) {
        var response = blockReasonService.search(q).stream()
                .map(r -> new BlockReasonResponse(r.getId(), r.getName(), r.getCategory()))
                .toList();
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping
    @Operation(summary = "내 블랙리스트 목록")
    public ResponseEntity<ApiResponse<List<CompanyBlacklist>>> list() {
        return ResponseEntity.ok(ApiResponse.success(blacklistService.list(SecurityUtils.currentAccountId())));
    }

    @PostMapping
    @Operation(summary = "회사 차단", description = "중복 등록이면 카테고리를 갱신한다(멱등). reasonIds=기존 카테고리, categoryNames=신규 입력(자동 마스터 승격), reason=자유 메모.")
    public ResponseEntity<ApiResponse<CompanyBlacklist>> add(@RequestBody AddRequest request) {
        var saved = blacklistService.add(SecurityUtils.currentAccountId(),
                request.companyName(), request.reason(), request.reasonIds(), request.categoryNames());
        return ResponseEntity.ok(ApiResponse.success(saved));
    }

    @PutMapping("/{id}")
    @Operation(summary = "차단 항목 수정", description = "기존 차단 항목의 카테고리를 교체한다(reasonIds=기존, categoryNames=신규 입력). 자유 메모는 보존한다.")
    public ResponseEntity<ApiResponse<CompanyBlacklist>> update(@PathVariable Long id,
                                                                @RequestBody UpdateRequest request) {
        var saved = blacklistService.update(SecurityUtils.currentAccountId(), id,
                request.reasonIds(), request.categoryNames());
        if (saved == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(ApiResponse.success(saved));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "차단 해제")
    public ResponseEntity<ApiResponse<Void>> remove(@PathVariable Long id) {
        blacklistService.remove(SecurityUtils.currentAccountId(), id);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
