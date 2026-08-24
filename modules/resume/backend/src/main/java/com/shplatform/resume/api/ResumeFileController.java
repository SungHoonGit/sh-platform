package com.shplatform.resume.api;

import com.shplatform.common.dto.ApiResponse;
import com.shplatform.common.security.SecurityUtils;
import com.shplatform.resume.api.dto.FileUploadResponse;
import com.shplatform.resume.domain.FileStorageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.ContentDisposition;

@RestController
@RequestMapping("/api/v1/files")
@RequiredArgsConstructor
@Tag(name = "File", description = "파일 업로드/다운로드 API (Phase 5)")
public class ResumeFileController {

    private final FileStorageService fileStorageService;

    /**
     * (명령형) 파일을 업로드한다.
     * 허용 확장자: pdf, pptx, ppt, docx, png, jpg, jpeg / 최대 10MB
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "파일 업로드")
    public ResponseEntity<ApiResponse<FileUploadResponse>> upload(
            @RequestParam("file") MultipartFile file) throws IOException {
        var response = fileStorageService.upload(SecurityUtils.currentAccountId(),
                file.getOriginalFilename(), file.getContentType(), file.getBytes());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * (질의형) 본인이 업로드한 파일을 다운로드한다.
     */
    @GetMapping("/{id}/download")
    @Operation(summary = "파일 다운로드")
    public ResponseEntity<byte[]> download(@PathVariable Long id) {
        var downloaded = fileStorageService.download(SecurityUtils.currentAccountId(), id);
        String contentType = downloaded.contentType() != null ? downloaded.contentType() : "application/octet-stream";
        ContentDisposition disposition = ContentDisposition.attachment()
                .filename(downloaded.originalName(), StandardCharsets.UTF_8)
                .build();
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .body(downloaded.data());
    }
}
