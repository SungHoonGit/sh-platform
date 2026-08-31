package com.shplatform.resume.api;

import com.shplatform.common.dto.ApiResponse;
import com.shplatform.common.security.SecurityUtils;
import com.shplatform.resume.api.dto.ResumeViewResponse;
import com.shplatform.resume.domain.ResumePdfService;
import com.shplatform.resume.domain.ResumeViewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.nio.charset.StandardCharsets;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
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
    private final ResumePdfService resumePdfService;

    /**
     * (질의형) 내 전체 이력을 조립하여 반환한다.
     */
    @GetMapping
    @Operation(summary = "전체 이력 조립 조회")
    public ResponseEntity<ApiResponse<ResumeViewResponse>> getMyResumeView() {
        var response = resumeViewService.getMyResumeView(SecurityUtils.currentAccountId());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * (명령형) 내 이력서를 서버사이드에서 A4 PDF로 생성하여 다운로드한다.
     */
    @GetMapping("/pdf")
    @Operation(summary = "이력서 PDF 다운로드", description = "기본 이력서를 OpenPDF로 렌더링한 application/pdf를 반환한다.")
    public ResponseEntity<byte[]> getPdf() {
        byte[] pdf = resumePdfService.generatePdf(SecurityUtils.currentAccountId());
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment()
                                .filename("이력서.pdf", StandardCharsets.UTF_8)
                                .build()
                                .toString())
                .body(pdf);
    }
}
