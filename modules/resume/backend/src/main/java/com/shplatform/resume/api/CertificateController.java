package com.shplatform.resume.api;

import com.shplatform.common.dto.ApiResponse;
import com.shplatform.common.security.SecurityUtils;
import com.shplatform.resume.api.dto.CertificateRequest;
import com.shplatform.resume.api.dto.CertificateResponse;
import com.shplatform.resume.domain.CertificateService;
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
@RequestMapping("/api/v1/certificates")
@RequiredArgsConstructor
@Tag(name = "Certificate", description = "자격증 API")
public class CertificateController {

    private final CertificateService certificateService;

    /**
     * (질의형) 내 자격증 목록을 조회한다.
     */
    @GetMapping
    @Operation(summary = "자격증 목록 조회")
    public ResponseEntity<ApiResponse<List<CertificateResponse>>> getCertificates() {
        var response = certificateService.getCertificates(SecurityUtils.currentAccountId());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * (명령형) 자격증을 추가한다.
     */
    @PostMapping
    @Operation(summary = "자격증 추가")
    public ResponseEntity<ApiResponse<CertificateResponse>> createCertificate(
            @Valid @RequestBody CertificateRequest request) {
        var response = certificateService.createCertificate(SecurityUtils.currentAccountId(), request);
        return ResponseEntity.ok(ApiResponse.created(response));
    }

    /**
     * (명령형) 자격증을 수정한다.
     */
    @PutMapping("/{id}")
    @Operation(summary = "자격증 수정")
    public ResponseEntity<ApiResponse<CertificateResponse>> updateCertificate(
            @PathVariable Long id,
            @Valid @RequestBody CertificateRequest request) {
        var response = certificateService.updateCertificate(SecurityUtils.currentAccountId(), id, request);
        return ResponseEntity.ok(ApiResponse.success("수정 완료", response));
    }

    /**
     * (명령형) 자격증을 삭제한다.
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "자격증 삭제")
    public ResponseEntity<Void> deleteCertificate(@PathVariable Long id) {
        certificateService.deleteCertificate(SecurityUtils.currentAccountId(), id);
        return ResponseEntity.noContent().build();
    }
}
