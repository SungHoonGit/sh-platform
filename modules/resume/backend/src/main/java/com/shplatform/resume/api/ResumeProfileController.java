package com.shplatform.resume.api;

import com.shplatform.common.dto.ApiResponse;
import com.shplatform.common.exception.BusinessException;
import com.shplatform.common.exception.ErrorCode;
import com.shplatform.common.security.SecurityUtils;
import com.shplatform.resume.api.dto.ProfileRequest;
import com.shplatform.resume.api.dto.ProfileResponse;
import com.shplatform.resume.domain.ResumeProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.io.IOException;
import java.util.Locale;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/profile")
@RequiredArgsConstructor
@Tag(name = "Profile", description = "인적사항 API")
public class ResumeProfileController {

    private static final Set<String> IMAGE_EXTENSIONS = Set.of("jpg", "jpeg", "png");

    private final ResumeProfileService resumeProfileService;
    private final com.shplatform.resume.domain.FileStorageService fileStorageService;

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

    /**
     * (명령형) 프로필 사진을 업로드하고 photo_url을 갱신한다.
     * 허용 확장자: jpg, jpeg, png / 최대 10MB
     */
    @PostMapping(value = "/photo", consumes = "multipart/form-data")
    @Operation(summary = "프로필 사진 업로드")
    public ResponseEntity<ApiResponse<ProfileResponse>> uploadPhoto(
            @RequestParam("file") MultipartFile file) throws IOException {
        String name = file.getOriginalFilename();
        String ext = "";
        if (name != null && name.lastIndexOf('.') >= 0) {
            ext = name.substring(name.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
        }
        if (!IMAGE_EXTENSIONS.contains(ext)) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }
        if (file.getSize() > 1_048_576L) { // 프로필 사진은 1MB 이하
            throw new BusinessException(ErrorCode.PAYLOAD_TOO_LARGE);
        }
        var uploaded = fileStorageService.upload(SecurityUtils.currentAccountId(),
                name, file.getContentType(), file.getBytes());
        resumeProfileService.updatePhotoUrl(SecurityUtils.currentAccountId(),
                "/api/v1/files/" + uploaded.id() + "/download");
        var response = resumeProfileService.getMyProfile(SecurityUtils.currentAccountId());
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
