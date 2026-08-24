package com.shplatform.resume.api;

import com.shplatform.common.dto.ApiResponse;
import com.shplatform.common.security.SecurityUtils;
import com.shplatform.resume.api.dto.PortfolioItemRequest;
import com.shplatform.resume.api.dto.PortfolioItemResponse;
import com.shplatform.resume.domain.PortfolioItemService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/portfolio-items")
@RequiredArgsConstructor
@Tag(name = "PortfolioItem", description = "포트폴리오 작업물 API")
public class PortfolioItemController {

    private final PortfolioItemService portfolioItemService;

    /**
     * (질의형) 내 포트폴리오 작업물 목록을 조회한다.
     */
    @GetMapping
    @Operation(summary = "작업물 목록 조회")
    public ResponseEntity<ApiResponse<List<PortfolioItemResponse>>> getPortfolioItems() {
        var response = portfolioItemService.getPortfolioItems(SecurityUtils.currentAccountId());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * (명령형) 포트폴리오 작업물을 추가한다. (현재 LINK 타입만 지원)
     */
    @PostMapping
    @Operation(summary = "작업물 추가 (LINK)")
    public ResponseEntity<ApiResponse<PortfolioItemResponse>> createPortfolioItem(
            @Valid @RequestBody PortfolioItemRequest request) {
        var response = portfolioItemService.createPortfolioItem(SecurityUtils.currentAccountId(), request);
        return ResponseEntity.ok(ApiResponse.created(response));
    }

    /**
     * (명령형) 포트폴리오 작업물을 수정한다.
     */
    @PutMapping("/{id}")
    @Operation(summary = "작업물 수정")
    public ResponseEntity<ApiResponse<PortfolioItemResponse>> updatePortfolioItem(
            @PathVariable Long id,
            @Valid @RequestBody PortfolioItemRequest request) {
        var response = portfolioItemService.updatePortfolioItem(SecurityUtils.currentAccountId(), id, request);
        return ResponseEntity.ok(ApiResponse.success("수정 완료", response));
    }

    /**
     * (명령형) 포트폴리오 작업물을 삭제한다.
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "작업물 삭제")
    public ResponseEntity<Void> deletePortfolioItem(@PathVariable Long id) {
        portfolioItemService.deletePortfolioItem(SecurityUtils.currentAccountId(), id);
        return ResponseEntity.noContent().build();
    }
}
