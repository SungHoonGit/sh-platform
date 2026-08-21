package com.shplatform.resume.api;

import com.shplatform.common.dto.ApiResponse;
import com.shplatform.common.security.SecurityUtils;
import com.shplatform.resume.api.dto.CareerRequest;
import com.shplatform.resume.api.dto.CareerResponse;
import com.shplatform.resume.domain.CareerService;
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
@RequestMapping("/api/v1/careers")
@RequiredArgsConstructor
@Tag(name = "Career", description = "경력 API")
public class CareerController {

    private final CareerService careerService;

    /**
     * (질의형) 내 경력 목록을 조회한다.
     */
    @GetMapping
    @Operation(summary = "경력 목록 조회")
    public ResponseEntity<ApiResponse<List<CareerResponse>>> getCareers() {
        var response = careerService.getCareers(SecurityUtils.currentAccountId());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * (명령형) 경력을 추가한다.
     */
    @PostMapping
    @Operation(summary = "경력 추가")
    public ResponseEntity<ApiResponse<CareerResponse>> createCareer(
            @Valid @RequestBody CareerRequest request) {
        var response = careerService.createCareer(SecurityUtils.currentAccountId(), request);
        return ResponseEntity.ok(ApiResponse.created(response));
    }

    /**
     * (명령형) 경력을 수정한다.
     */
    @PutMapping("/{id}")
    @Operation(summary = "경력 수정")
    public ResponseEntity<ApiResponse<CareerResponse>> updateCareer(
            @PathVariable Long id,
            @Valid @RequestBody CareerRequest request) {
        var response = careerService.updateCareer(SecurityUtils.currentAccountId(), id, request);
        return ResponseEntity.ok(ApiResponse.success("수정 완료", response));
    }

    /**
     * (명령형) 경력을 삭제한다.
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "경력 삭제")
    public ResponseEntity<Void> deleteCareer(@PathVariable Long id) {
        careerService.deleteCareer(SecurityUtils.currentAccountId(), id);
        return ResponseEntity.noContent().build();
    }
}
