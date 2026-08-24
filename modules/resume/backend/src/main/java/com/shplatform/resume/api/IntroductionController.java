package com.shplatform.resume.api;

import com.shplatform.common.dto.ApiResponse;
import com.shplatform.common.security.SecurityUtils;
import com.shplatform.resume.api.dto.IntroductionRequest;
import com.shplatform.resume.api.dto.IntroductionResponse;
import com.shplatform.resume.domain.IntroductionService;
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
@RequestMapping("/api/v1/introductions")
@RequiredArgsConstructor
@Tag(name = "Introduction", description = "자기소개 API")
public class IntroductionController {

    private final IntroductionService introductionService;

    /**
     * (질의형) 내 자기소개 항목 목록을 조회한다.
     */
    @GetMapping
    @Operation(summary = "자기소개 항목 목록 조회")
    public ResponseEntity<ApiResponse<List<IntroductionResponse>>> getIntroductions() {
        var response = introductionService.getIntroductions(SecurityUtils.currentAccountId());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * (명령형) 자기소개 항목을 추가한다.
     */
    @PostMapping
    @Operation(summary = "자기소개 항목 추가")
    public ResponseEntity<ApiResponse<IntroductionResponse>> createIntroduction(
            @Valid @RequestBody IntroductionRequest request) {
        var response = introductionService.createIntroduction(SecurityUtils.currentAccountId(), request);
        return ResponseEntity.ok(ApiResponse.created(response));
    }

    /**
     * (명령형) 자기소개 항목을 수정한다.
     */
    @PutMapping("/{id}")
    @Operation(summary = "자기소개 항목 수정")
    public ResponseEntity<ApiResponse<IntroductionResponse>> updateIntroduction(
            @PathVariable Long id,
            @Valid @RequestBody IntroductionRequest request) {
        var response = introductionService.updateIntroduction(SecurityUtils.currentAccountId(), id, request);
        return ResponseEntity.ok(ApiResponse.success("수정 완료", response));
    }

    /**
     * (명령형) 자기소개 항목을 삭제한다.
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "자기소개 항목 삭제")
    public ResponseEntity<Void> deleteIntroduction(@PathVariable Long id) {
        introductionService.deleteIntroduction(SecurityUtils.currentAccountId(), id);
        return ResponseEntity.noContent().build();
    }
}
