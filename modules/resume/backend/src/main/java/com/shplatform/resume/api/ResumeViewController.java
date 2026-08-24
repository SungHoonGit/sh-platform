package com.shplatform.resume.api;

import com.shplatform.common.dto.ApiResponse;
import com.shplatform.common.security.SecurityUtils;
import com.shplatform.resume.api.dto.ResumeViewResponse;
import com.shplatform.resume.domain.ResumeViewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/view")
@RequiredArgsConstructor
@Tag(name = "ResumeView", description = "이력서 뷰 조립 API")
public class ResumeViewController {

    private final ResumeViewService resumeViewService;

    /**
     * (질의형) 내 전체 이력을 조립하여 반환한다.
     */
    @GetMapping
    @Operation(summary = "전체 이력 조립 조회")
    public ResponseEntity<ApiResponse<ResumeViewResponse>> getMyResumeView() {
        var response = resumeViewService.getMyResumeView(SecurityUtils.currentAccountId());
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
