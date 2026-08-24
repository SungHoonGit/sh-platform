package com.shplatform.resume.api;

import com.shplatform.common.dto.ApiResponse;
import com.shplatform.common.security.SecurityUtils;
import com.shplatform.resume.api.dto.DocumentCreateRequest;
import com.shplatform.resume.api.dto.DocumentResponse;
import com.shplatform.resume.api.dto.DocumentUpdateRequest;
import com.shplatform.resume.domain.ResumeDocumentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
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

@RestController
@RequestMapping("/api/v1/documents")
@RequiredArgsConstructor
@Tag(name = "Documents", description = "이력서 문서(뷰 정의) API")
public class DocumentsController {

    private final ResumeDocumentService documentService;

    /**
     * (질의형) 내 문서 목록을 조회한다. 없으면 기본 문서를 자동 생성한다.
     */
    @GetMapping
    @Operation(summary = "문서 목록 (없으면 기본 문서 자동 생성)")
    public ResponseEntity<ApiResponse<List<DocumentResponse>>> getDocuments() {
        var response = documentService.getDocuments(SecurityUtils.currentAccountId());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * (명령형) 문서를 생성한다. fromDocumentId로 기존 문서 편성을 불러올 수 있다.
     */
    @PostMapping
    @Operation(summary = "문서 생성 (불러오기 지원)")
    public ResponseEntity<ApiResponse<DocumentResponse>> createDocument(
            @Valid @RequestBody DocumentCreateRequest request) {
        var response = documentService.createDocument(SecurityUtils.currentAccountId(), request);
        return ResponseEntity.ok(ApiResponse.created(response));
    }

    /**
     * (명령형) 문서를 수정한다.
     */
    @PutMapping("/{id}")
    @Operation(summary = "문서 수정")
    public ResponseEntity<ApiResponse<DocumentResponse>> updateDocument(
            @PathVariable Long id,
            @Valid @RequestBody DocumentUpdateRequest request) {
        var response = documentService.updateDocument(SecurityUtils.currentAccountId(), id, request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * (명령형) 대표 문서로 지정한다.
     */
    @PutMapping("/{id}/primary")
    @Operation(summary = "대표 문서 지정")
    public ResponseEntity<ApiResponse<Void>> markPrimary(@PathVariable Long id) {
        documentService.markPrimary(SecurityUtils.currentAccountId(), id);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    /**
     * (명령형) 문서를 삭제한다. 마지막 1개는 삭제할 수 없다.
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "문서 삭제")
    public ResponseEntity<ApiResponse<Void>> deleteDocument(@PathVariable Long id) {
        documentService.deleteDocument(SecurityUtils.currentAccountId(), id);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
