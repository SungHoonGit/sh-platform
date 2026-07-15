package com.shplatform.common.file.controller;

import com.shplatform.common.file.model.FileNode;
import com.shplatform.common.file.model.JobItem;
import com.shplatform.common.file.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

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

    @GetMapping("/tree/view")
    public ResponseEntity<List<FileNode>> getTreeView(
            @RequestParam String rootPath) {
        List<FileNode> nodes = fileTreeService.scanTree(rootPath);
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

    @GetMapping("/jobs")
    public ResponseEntity<Map<String, Object>> getJobs(
            @RequestParam String rootPath,
            @RequestParam String path,
            @RequestParam(required = false) String site,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        List<JobItem> allJobs = fileReadService.parseJobs(rootPath, path);

        // 사이트 필터
        if (site != null && !site.isEmpty() && !"all".equals(site)) {
            allJobs = allJobs.stream()
                    .filter(j -> site.equals(j.getSite()))
                    .collect(Collectors.toList());
        }

        // 전체 사이트 목록
        List<String> sites = fileReadService.parseJobs(rootPath, path).stream()
                .map(JobItem::getSite)
                .distinct()
                .collect(Collectors.toList());

        // 페이징
        int total = allJobs.size();
        int totalPages = (int) Math.ceil((double) total / size);
        int from = Math.min(page * size, total);
        int to = Math.min(from + size, total);
        List<JobItem> paged = allJobs.subList(from, to);

        Map<String, Object> result = new HashMap<>();
        result.put("jobs", paged);
        result.put("total", total);
        result.put("page", page);
        result.put("size", size);
        result.put("totalPages", totalPages);
        result.put("sites", sites);
        result.put("currentSite", site != null ? site : "all");

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
