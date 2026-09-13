package com.shplatform.resume.api;

import com.shplatform.common.dto.ApiResponse;
import com.shplatform.common.security.SecurityUtils;
import com.shplatform.resume.api.dto.ProjectRequest;
import com.shplatform.resume.api.dto.ProjectResponse;
import com.shplatform.resume.api.dto.ReorderRequest;
import com.shplatform.resume.domain.ProjectService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/projects")
@RequiredArgsConstructor
@Tag(name = "Project", description = "프로젝트 API")
public class ProjectController {

    private final ProjectService projectService;

    /**
     * (질의형) 내 프로젝트 목록을 조회한다.
     */
    @GetMapping
    @Operation(summary = "프로젝트 목록 조회")
    public ResponseEntity<ApiResponse<List<ProjectResponse>>> getProjects(@RequestParam Long documentId) {
        var response = projectService.getProjects(SecurityUtils.currentAccountId(), documentId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * (명령형) 프로젝트를 추가한다.
     */
    @PostMapping
    @Operation(summary = "프로젝트 추가")
    public ResponseEntity<ApiResponse<ProjectResponse>> createProject(
            @Valid @RequestBody ProjectRequest request,
            @RequestParam Long documentId) {
        var response = projectService.createProject(SecurityUtils.currentAccountId(), documentId, request);
        return ResponseEntity.ok(ApiResponse.created(response));
    }

    /**
     * (명령형) 프로젝트를 수정한다.
     */
    @PutMapping("/{id}")
    @Operation(summary = "프로젝트 수정")
    public ResponseEntity<ApiResponse<ProjectResponse>> updateProject(
            @PathVariable Long id,
            @Valid @RequestBody ProjectRequest request,
            @RequestParam Long documentId) {
        var response = projectService.updateProject(SecurityUtils.currentAccountId(), id, documentId, request);
        return ResponseEntity.ok(ApiResponse.success("수정 완료", response));
    }

    /**
     * (명령형) 프로젝트를 삭제한다.
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "프로젝트 삭제")
    public ResponseEntity<Void> deleteProject(@PathVariable Long id, @RequestParam Long documentId) {
        projectService.deleteProject(SecurityUtils.currentAccountId(), id, documentId);
        return ResponseEntity.noContent().build();
    }

    /**
     * (명령형) 프로젝트의 표시 순서를 재정렬한다.
     */
    @PutMapping("/reorder")
    @Operation(summary = "프로젝트 표시 순서 재정렬")
    public ResponseEntity<Void> reorderProjects(@Valid @RequestBody ReorderRequest request, @RequestParam Long documentId) {
        projectService.reorderProjects(SecurityUtils.currentAccountId(), documentId, request.ids());
        return ResponseEntity.noContent().build();
    }
}
