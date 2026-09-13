package com.shplatform.resume.api;

import com.shplatform.common.dto.ApiResponse;
import com.shplatform.common.security.SecurityUtils;
import com.shplatform.resume.api.dto.CareerItemRequest;
import com.shplatform.resume.api.dto.CareerItemResponse;
import com.shplatform.resume.api.dto.ReorderRequest;
import com.shplatform.resume.domain.CareerItemService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/careers/{careerId}/items")
@RequiredArgsConstructor
@Tag(name = "CareerItem", description = "경력 기간별 상세 항목 API")
public class CareerItemController {

    private final CareerItemService careerItemService;

    /**
     * (질의형) 특정 경력의 기간별 상세 항목 목록을 조회한다.
     */
    @GetMapping
    @Operation(summary = "경력 상세 항목 목록 조회")
    public ResponseEntity<ApiResponse<List<CareerItemResponse>>> getCareerItems(
            @PathVariable Long careerId,
            @RequestParam Long documentId) {
        var response = careerItemService.getCareerItems(SecurityUtils.currentAccountId(), careerId, documentId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * (명령형) 경력에 기간별 상세 항목을 추가한다.
     */
    @PostMapping
    @Operation(summary = "경력 상세 항목 추가")
    public ResponseEntity<ApiResponse<CareerItemResponse>> createCareerItem(
            @PathVariable Long careerId,
            @Valid @RequestBody CareerItemRequest request,
            @RequestParam Long documentId) {
        var response = careerItemService.createCareerItem(SecurityUtils.currentAccountId(), careerId, documentId, request);
        return ResponseEntity.ok(ApiResponse.created(response));
    }

    /**
     * (명령형) 경력의 상세 항목을 수정한다.
     */
    @PutMapping("/{id}")
    @Operation(summary = "경력 상세 항목 수정")
    public ResponseEntity<ApiResponse<CareerItemResponse>> updateCareerItem(
            @PathVariable Long careerId,
            @PathVariable Long id,
            @Valid @RequestBody CareerItemRequest request,
            @RequestParam Long documentId) {
        var response = careerItemService.updateCareerItem(SecurityUtils.currentAccountId(), careerId, id, documentId, request);
        return ResponseEntity.ok(ApiResponse.success("수정 완료", response));
    }

    /**
     * (명령형) 경력의 상세 항목을 삭제한다.
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "경력 상세 항목 삭제")
    public ResponseEntity<Void> deleteCareerItem(@PathVariable Long careerId, @PathVariable Long id, @RequestParam Long documentId) {
        careerItemService.deleteCareerItem(SecurityUtils.currentAccountId(), careerId, id, documentId);
        return ResponseEntity.noContent().build();
    }

    /**
     * (명령형) 상세 항목의 표시 순서를 재정렬한다.
     */
    @PutMapping("/reorder")
    @Operation(summary = "경력 상세 항목 표시 순서 재정렬")
    public ResponseEntity<Void> reorderCareerItems(
            @PathVariable Long careerId,
            @Valid @RequestBody ReorderRequest request,
            @RequestParam Long documentId) {
        careerItemService.reorderCareerItems(SecurityUtils.currentAccountId(), careerId, documentId, request.ids());
        return ResponseEntity.noContent().build();
    }
}