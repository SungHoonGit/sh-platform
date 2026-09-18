package com.scraper.platform.controller;

import com.scraper.platform.api.dto.CompanyNoteDetailResponse;
import com.scraper.platform.api.dto.CompanyNoteRequest;
import com.scraper.platform.api.dto.CompanyNoteResponse;
import com.scraper.platform.api.dto.CompanyPostingItem;
import com.scraper.platform.api.dto.CompanySuggestItem;
import com.scraper.platform.service.CompanyNoteService;
import com.shplatform.common.dto.ApiResponse;
import com.shplatform.common.security.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * 계정별 회사 메모 API (내 별점·북마크·MD 분석). 크롤링 평점과 차단 여부를 조인해 반환한다.
 */
@RestController
@RequestMapping("/company-notes")
@RequiredArgsConstructor
@Tag(name = "CompanyNote", description = "회사 메모 관리 (별점·북마크·MD 분석)")
public class CompanyNoteController {

    private final CompanyNoteService noteService;

    @GetMapping
    @Operation(summary = "내 회사 목록", description = "tab=all|bookmarked|blocked, q=회사명 부분 검색, sort=display|stars|updated, 평점·차단여부 조인")
    public ResponseEntity<ApiResponse<Page<CompanyNoteResponse>>> list(
            @RequestParam(value = "tab", defaultValue = "all") String tab,
            @RequestParam(value = "q", required = false) String q,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "50") int size,
            @RequestParam(value = "sort", defaultValue = "updated") String sort,
            @RequestParam(value = "dir", defaultValue = "desc") String dir) {
        return ResponseEntity.ok(ApiResponse.success(
                noteService.list(SecurityUtils.currentAccountId(), tab, q, page, size, sort, dir)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "회사 메모 상세", description = "note_md 포함. 없거나 타인 소유면 404")
    public ResponseEntity<ApiResponse<CompanyNoteDetailResponse>> get(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(
                noteService.get(SecurityUtils.currentAccountId(), id)));
    }

    @PostMapping
    @Operation(summary = "회사 메모 생성", description = "같은 회사(정규화 기준)가 있으면 갱신한다(멱등)")
    public ResponseEntity<ApiResponse<CompanyNoteDetailResponse>> upsert(
            @RequestBody CompanyNoteRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                noteService.upsert(SecurityUtils.currentAccountId(), request)));
    }

    @PutMapping("/{id}")
    @Operation(summary = "회사 메모 수정", description = "별점·북마크·MD 수정. 회사명 변경은 무시된다")
    public ResponseEntity<ApiResponse<CompanyNoteDetailResponse>> update(
            @PathVariable Long id,
            @RequestBody CompanyNoteRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                noteService.update(SecurityUtils.currentAccountId(), id, request)));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "회사 메모 삭제")
    public ResponseEntity<ApiResponse<Void>> remove(@PathVariable Long id) {
        noteService.delete(SecurityUtils.currentAccountId(), id);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @GetMapping("/{id}/export")
    @Operation(summary = ".md 내보내기", description = "분석 내용을 마크다운 파일로 다운로드한다")
    public ResponseEntity<String> export(@PathVariable Long id) {
        Long accountId = SecurityUtils.currentAccountId();
        CompanyNoteDetailResponse detail = noteService.get(accountId, id);
        String filename = detail.companyNameDisplay().replaceAll("[\\\\/:*?\"<>|]", "_") + "-analysis.md";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition
                        .attachment()
                        .filename(filename, StandardCharsets.UTF_8)
                        .build().toString())
                .contentType(new MediaType("text", "markdown", StandardCharsets.UTF_8))
                .body(noteService.exportMarkdown(accountId, id));
    }

    @GetMapping("/{id}/postings")
    @Operation(summary = "관련 공고 조회", description = "이 회사의 최근 저장 공고를 수집일 내림차순으로 반환한다 (슬라이드 보기 탭용)")
    public ResponseEntity<ApiResponse<List<CompanyPostingItem>>> postings(
            @PathVariable Long id,
            @RequestParam(value = "size", defaultValue = "10") int size) {
        return ResponseEntity.ok(ApiResponse.success(
                noteService.recentPostings(SecurityUtils.currentAccountId(), id, size)));
    }

    @GetMapping("/company-suggest")
    @Operation(summary = "수집 회사 자동완성", description = "뷰어에 수집된 회사명을 부분 검색한다 (회사 추가 모달용, 최대 8건)")
    public ResponseEntity<ApiResponse<List<CompanySuggestItem>>> suggest(
            @RequestParam(value = "q", required = false) String q) {
        return ResponseEntity.ok(ApiResponse.success(
                noteService.suggestCompanies(SecurityUtils.currentAccountId(), q)));
    }
}
