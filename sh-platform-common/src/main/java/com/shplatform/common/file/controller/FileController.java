package com.shplatform.common.file.controller;

import com.shplatform.common.file.model.FileNode;
import com.shplatform.common.file.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/docs")
@RequiredArgsConstructor
public class FileController {

    private final FileTreeService fileTreeService;
    private final FileReadService fileReadService;
    private final FileSearchService fileSearchService;
    private final FileExportService fileExportService;

    @GetMapping("/tree")
    public ResponseEntity<List<FileNode>> getTree(
            @RequestParam String rootPath,
            @RequestParam(required = false) String path) {
        List<FileNode> nodes = fileTreeService.scan(rootPath, path);
        return ResponseEntity.ok(nodes);
    }

    @GetMapping("/file")
    public ResponseEntity<Map<String, Object>> getFile(
            @RequestParam String rootPath,
            @RequestParam String path,
            @RequestParam(defaultValue = "html") String format) {
        Map<String, Object> result = new HashMap<>();
        result.put("path", path);
        result.put("title", fileReadService.readFileTitle(rootPath, path));

        if ("raw".equals(format)) {
            result.put("content", fileReadService.readRaw(rootPath, path));
            result.put("format", "raw");
        } else {
            result.put("content", fileReadService.readAsHtml(rootPath, path));
            result.put("format", "html");
        }

        return ResponseEntity.ok(result);
    }

    @GetMapping("/search")
    public ResponseEntity<Map<String, Object>> search(
            @RequestParam String rootPath,
            @RequestParam String q) {
        List<FileSearchService.SearchResult> results = fileSearchService.search(rootPath, q);

        Map<String, Object> response = new HashMap<>();
        response.put("query", q);
        response.put("totalMatches", results.size());
        response.put("results", results);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/export/pdf")
    public ResponseEntity<byte[]> exportPdf(
            @RequestParam String rootPath,
            @RequestParam String path) {
        byte[] pdfBytes = fileExportService.exportToPdf(rootPath, path);
        String fileName = path.substring(path.lastIndexOf('/') + 1).replace(".md", ".pdf");

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdfBytes);
    }

    @GetMapping("/export/excel")
    public ResponseEntity<byte[]> exportExcel(
            @RequestParam String rootPath,
            @RequestParam String path) {
        byte[] excelBytes = fileExportService.exportToExcel(rootPath, path);
        String fileName = path.substring(path.lastIndexOf('/') + 1).replace(".md", ".xlsx");

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(excelBytes);
    }
}
