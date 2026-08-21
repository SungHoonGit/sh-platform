package com.shplatform.resume.api;

import com.shplatform.common.dto.ApiResponse;
import com.shplatform.common.security.SecurityUtils;
import com.shplatform.resume.api.dto.ProfileRequest;
import com.shplatform.resume.api.dto.ProfileResponse;
import com.shplatform.resume.domain.ResumeProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/profile")
@RequiredArgsConstructor
@Tag(name = "Profile", description = "인적사항 API")
public class ResumeProfileController {

    private final ResumeProfileService resumeProfileService;

    /**
     * (질의형) 내 인적사항을 조회한다.
     */
    @GetMapping
    @Operation(summary = "내 인적사항 조회")
    public ResponseEntity<ApiResponse<ProfileResponse>> getMyProfile() {
        var response = resumeProfileService.getMyProfile(SecurityUtils.currentAccountId());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * (명령형) 내 인적사항을 등록 또는 수정한다(upsert).
     */
    @PutMapping
    @Operation(summary = "인적사항 등록/수정")
    public ResponseEntity<ApiResponse<ProfileResponse>> upsertProfile(
            @Valid @RequestBody ProfileRequest request) {
        var response = resumeProfileService.upsertProfile(SecurityUtils.currentAccountId(), request);
        return ResponseEntity.ok(ApiResponse.success("저장 완료", response));
    }
}
