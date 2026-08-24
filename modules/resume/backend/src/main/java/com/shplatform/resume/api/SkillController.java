package com.shplatform.resume.api;

import com.shplatform.common.dto.ApiResponse;
import com.shplatform.common.security.SecurityUtils;
import com.shplatform.resume.api.dto.SkillRequest;
import com.shplatform.resume.api.dto.SkillResponse;
import com.shplatform.resume.domain.SkillService;
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
@RequestMapping("/api/v1/skills")
@RequiredArgsConstructor
@Tag(name = "Skill", description = "스킬 API")
public class SkillController {

    private final SkillService skillService;

    /**
     * (질의형) 내 스킬 목록을 조회한다.
     */
    @GetMapping
    @Operation(summary = "스킬 목록 조회")
    public ResponseEntity<ApiResponse<List<SkillResponse>>> getSkills() {
        var response = skillService.getSkills(SecurityUtils.currentAccountId());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * (명령형) 스킬을 추가한다.
     */
    @PostMapping
    @Operation(summary = "스킬 추가")
    public ResponseEntity<ApiResponse<SkillResponse>> createSkill(
            @Valid @RequestBody SkillRequest request) {
        var response = skillService.createSkill(SecurityUtils.currentAccountId(), request);
        return ResponseEntity.ok(ApiResponse.created(response));
    }

    /**
     * (명령형) 스킬을 수정한다.
     */
    @PutMapping("/{id}")
    @Operation(summary = "스킬 수정")
    public ResponseEntity<ApiResponse<SkillResponse>> updateSkill(
            @PathVariable Long id,
            @Valid @RequestBody SkillRequest request) {
        var response = skillService.updateSkill(SecurityUtils.currentAccountId(), id, request);
        return ResponseEntity.ok(ApiResponse.success("수정 완료", response));
    }

    /**
     * (명령형) 스킬을 삭제한다.
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "스킬 삭제")
    public ResponseEntity<Void> deleteSkill(@PathVariable Long id) {
        skillService.deleteSkill(SecurityUtils.currentAccountId(), id);
        return ResponseEntity.noContent().build();
    }
}
