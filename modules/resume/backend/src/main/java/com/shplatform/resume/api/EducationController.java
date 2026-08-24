package com.shplatform.resume.api;

import com.shplatform.common.dto.ApiResponse;
import com.shplatform.common.security.SecurityUtils;
import com.shplatform.resume.api.dto.EducationRequest;
import com.shplatform.resume.api.dto.EducationResponse;
import com.shplatform.resume.domain.EducationService;
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
@RequestMapping("/api/v1/educations")
@RequiredArgsConstructor
@Tag(name = "Education", description = "학력 API")
public class EducationController {

    private final EducationService educationService;

    /**
     * (질의형) 내 학력 목록을 조회한다.
     */
    @GetMapping
    @Operation(summary = "학력 목록 조회")
    public ResponseEntity<ApiResponse<List<EducationResponse>>> getEducations() {
        var response = educationService.getEducations(SecurityUtils.currentAccountId());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * (명령형) 학력을 추가한다.
     */
    @PostMapping
    @Operation(summary = "학력 추가")
    public ResponseEntity<ApiResponse<EducationResponse>> createEducation(
            @Valid @RequestBody EducationRequest request) {
        var response = educationService.createEducation(SecurityUtils.currentAccountId(), request);
        return ResponseEntity.ok(ApiResponse.created(response));
    }

    /**
     * (명령형) 학력을 수정한다.
     */
    @PutMapping("/{id}")
    @Operation(summary = "학력 수정")
    public ResponseEntity<ApiResponse<EducationResponse>> updateEducation(
            @PathVariable Long id,
            @Valid @RequestBody EducationRequest request) {
        var response = educationService.updateEducation(SecurityUtils.currentAccountId(), id, request);
        return ResponseEntity.ok(ApiResponse.success("수정 완료", response));
    }

    /**
     * (명령형) 학력을 삭제한다.
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "학력 삭제")
    public ResponseEntity<Void> deleteEducation(@PathVariable Long id) {
        educationService.deleteEducation(SecurityUtils.currentAccountId(), id);
        return ResponseEntity.noContent().build();
    }
}
