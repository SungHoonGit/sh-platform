package com.shplatform.resume.api;

import com.shplatform.common.dto.ApiResponse;
import com.shplatform.resume.api.dto.ShareViewResponse;
import com.shplatform.resume.domain.ResumePdfService;
import com.shplatform.resume.domain.ResumeShareService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 공유 이력서 공개 조회 API.
 * 인증 없이 접근 가능하므로 SecurityConfig에 /share/** 가 whitelist로 등록되어야 한다.
 */
@RestController
@RequestMapping("/share")
@RequiredArgsConstructor
@Tag(name = "Share", description = "공유 이력서 공개 조회 API (인증 불필요)")
public class ShareController {

    private final ResumeShareService shareService;
    private final ResumePdfService resumePdfService;

    /**
     * (질의형) 공유 토큰으로 이력서 뷰를 반환한다. 검색 엔진 색인을 막는 noindex 헤더를 포함한다.
     */
    @GetMapping("/{token}")
    @Operation(summary = "공유 이력서 뷰 조회", description = "인증 없이 공유된 이력서를 조회한다.")
    public ResponseEntity<ApiResponse<ShareViewResponse>> getSharedView(@PathVariable String token) {
        ShareViewResponse view = shareService.getPublicView(token);
        return ResponseEntity.ok()
                .header("X-Robots-Tag", "noindex")
                .body(ApiResponse.success(view));
    }

    /**
     * (질의형) 공유 토큰으로 이력서 PDF를 다운로드한다.
     */
    @GetMapping("/{token}/pdf")
    @Operation(summary = "공유 이력서 PDF 다운로드")
    public ResponseEntity<byte[]> getSharedPdf(@PathVariable String token) {
        var resolved = shareService.resolve(token)
                .orElseThrow(() -> new com.shplatform.common.exception.BusinessException(
                        com.shplatform.common.exception.ErrorCode.NOT_FOUND));
        byte[] pdf = resumePdfService.generatePdf(resolved.userId(), resolved.documentId());
        String filename = resumePdfService.pdfFilename(resolved.userId(), resolved.documentId());
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDispositionSupport.attachment(filename))
                .header("X-Robots-Tag", "noindex")
                .body(pdf);
    }
}