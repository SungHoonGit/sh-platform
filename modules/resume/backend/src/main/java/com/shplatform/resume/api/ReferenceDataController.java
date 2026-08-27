package com.shplatform.resume.api;

import com.shplatform.common.dto.ApiResponse;
import com.shplatform.resume.api.dto.MajorResponse;
import com.shplatform.resume.api.dto.SchoolResponse;
import com.shplatform.resume.domain.ReferenceDataService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/reference")
@RequiredArgsConstructor
@Tag(name = "Reference", description = "기준/마스터 데이터 API")
public class ReferenceDataController {

    private final ReferenceDataService referenceDataService;

    /**
     * (질의형) 학교를 검색한다. 기준 데이터는 DB 테이블에서 조회 (프론트 하드코딩 금지).
     *
     * @param q          검색어
     * @param schoolType 학교 유형 필터 (선택)
     * @return 학교 목록
     */
    @GetMapping("/schools/search")
    @Operation(summary = "학교 검색")
    public ResponseEntity<ApiResponse<List<SchoolResponse>>> searchSchools(
            @RequestParam("q") String q,
            @RequestParam(value = "schoolType", required = false) String schoolType) {
        var response = referenceDataService.searchSchools(q, schoolType);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * (질의형) 전공을 검색한다. 기준 데이터는 DB 테이블에서 조회 (프론트 하드코딩 금지).
     *
     * @param q 검색어
     * @return 전공 목록
     */
    @GetMapping("/majors/search")
    @Operation(summary = "전공 검색")
    public ResponseEntity<ApiResponse<List<MajorResponse>>> searchMajors(
            @RequestParam("q") String q) {
        var response = referenceDataService.searchMajors(q);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
