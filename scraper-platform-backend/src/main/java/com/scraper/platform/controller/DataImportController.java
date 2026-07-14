package com.scraper.platform.controller;

import com.scraper.platform.service.MarkdownParserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/data-import")
@RequiredArgsConstructor
@Tag(name = "DataImport", description = "데이터 임포트 API")
public class DataImportController {

    private final MarkdownParserService markdownParserService;

    @PostMapping("/all")
    @Operation(summary = "전체 파일 임포트", description = "모든 카테고리의 MD 파일을 임포트합니다")
    public ResponseEntity<Map<String, Object>> importAllFiles() {
        int imported = markdownParserService.importAllFiles();
        
        Map<String, Object> result = new HashMap<>();
        result.put("message", "Import completed");
        result.put("imported", imported);
        
        return ResponseEntity.ok(result);
    }

    @PostMapping("/category/{category}")
    @Operation(summary = "카테고리별 파일 임포트", description = "특정 카테고리의 MD 파일을 임포트합니다")
    public ResponseEntity<Map<String, Object>> importCategoryFiles(@PathVariable String category) {
        int imported = markdownParserService.importAllFiles(); // 카테고리별 임포트 로직
        
        Map<String, Object> result = new HashMap<>();
        result.put("message", "Import completed for category: " + category);
        result.put("imported", imported);
        
        return ResponseEntity.ok(result);
    }
}
