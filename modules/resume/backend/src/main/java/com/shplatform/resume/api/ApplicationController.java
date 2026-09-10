package com.shplatform.resume.api;

import com.shplatform.common.dto.ApiResponse;
import com.shplatform.common.security.SecurityUtils;
import com.shplatform.resume.api.dto.ApplicationRequest;
import com.shplatform.resume.api.dto.ApplicationResponse;
import com.shplatform.resume.api.dto.SkillMatchRequest;
import com.shplatform.resume.api.dto.SkillMatchResponse;
import com.shplatform.resume.domain.ApplicationService;
import com.shplatform.resume.domain.SkillMatchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 지원 관리 API.
 */
@RestController
@RequestMapping("/api/v1/applications")
@RequiredArgsConstructor
@Tag(name = "Application", description = "지원 관리 API")
public class ApplicationController {

    private final ApplicationService applicationService;
    private final SkillMatchService skillMatchService;

    /**
     * (질의형) 공고의 기술 스택과 내 이력서의 기술 스택을 비교해 일치 기술을 계산한다.
     *
     * @param request 공고의 기술 스택 문자열 (콤마 구분)
     * @return 일치 기술 목록과 개수
     */
    @PostMapping("/match-skills")
    @Operation(summary = "공고-이력서 기술 매칭", description = "공고 기술 스택과 내 이력서(프로젝트·작업물) 기술 스택을 마스터 기준으로 정규화해 일치 기술을 반환합니다.")
    public ResponseEntity<ApiResponse<SkillMatchResponse>> matchSkills(
            @Valid @RequestBody SkillMatchRequest request) {
        SkillMatchResponse response =
                skillMatchService.match(SecurityUtils.currentAccountId(), request.techStack());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 내 지원 목록을 조회한다.
     *
     * @param status 진행 상태 필터 (선택)
     * @return 지원 목록
     */
    @GetMapping
    @Operation(summary = "지원 목록 조회", description = "내 전체 지원 목록을 최신순으로 반환합니다.")
    public ResponseEntity<ApiResponse<List<ApplicationResponse>>> getApplications(
            @RequestParam(required = false) String status) {
        List<ApplicationResponse> applications =
                applicationService.getApplications(SecurityUtils.currentAccountId(), status);
        return ResponseEntity.ok(ApiResponse.success(applications));
    }

    /**
     * 새 지원을 등록한다.
     *
     * @param request 회사명, 공고 제목 등
     * @return 등록된 지원 정보
     */
    @PostMapping
    @Operation(summary = "지원 등록")
    public ResponseEntity<ApiResponse<ApplicationResponse>> create(
            @Valid @RequestBody ApplicationRequest request) {
        ApplicationResponse response =
                applicationService.create(SecurityUtils.currentAccountId(), request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 기존 지원을 수정한다.
     *
     * @param id      지원 ID
     * @param request 수정할 내용
     * @return 수정된 지원 정보
     */
    @PutMapping("/{id}")
    @Operation(summary = "지원 수정")
    public ResponseEntity<ApiResponse<ApplicationResponse>> update(
            @PathVariable Long id,
            @Valid @RequestBody ApplicationRequest request) {
        ApplicationResponse response =
                applicationService.update(SecurityUtils.currentAccountId(), id, request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 지원을 삭제한다.
     *
     * @param id 지원 ID
     * @return 성공 여부
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "지원 삭제")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        applicationService.delete(SecurityUtils.currentAccountId(), id);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
