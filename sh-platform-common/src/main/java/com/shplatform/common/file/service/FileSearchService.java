package com.shplatform.common.file.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileSearchService {

    private final FileTreeService fileTreeService;

    public List<SearchResult> search(String rootPath, String query) {
        if (query == null || query.trim().isEmpty()) {
            return Collections.emptyList();
        }

        String lowerQuery = query.toLowerCase();
        Pattern pattern = Pattern.compile(Pattern.quote(query), Pattern.CASE_INSENSITIVE);

        List<SearchResult> results = new ArrayList<>();

        try {
            Path basePath = Paths.get(rootPath).toAbsolutePath().normalize();
            if (!Files.exists(basePath)) {
                return results;
            }

            Files.walk(basePath)
                    .filter(Files::isRegularFile)
                    .filter(p -> p.toString().endsWith(".md"))
                    .forEach(mdFile -> {
                        try {
                            List<String> lines = Files.readAllLines(mdFile, StandardCharsets.UTF_8);
                            String relativePath = basePath.relativize(mdFile).toString();

                            for (int i = 0; i < lines.size(); i++) {
                                String line = lines.get(i);
                                if (line.toLowerCase().contains(lowerQuery)) {
                                    String highlight = highlightMatch(line, pattern);
                                    results.add(SearchResult.builder()
                                            .file(relativePath)
                                            .line(i + 1)
                                            .content(line.trim())
                                            .highlight(highlight)
                                            .build());
                                }
                            }
                        } catch (IOException e) {
                            log.warn("Error searching file: {}", mdFile, e);
                        }
                    });
        } catch (IOException e) {
            log.error("Error walking directory for search: {}", rootPath, e);
        }

        return results;
    }

    private String highlightMatch(String line, Pattern pattern) {
        Matcher matcher = pattern.matcher(line);
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            matcher.appendReplacement(sb, "<mark>" + matcher.group() + "</mark>");
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    @lombok.Data
    @lombok.Builder
    public static class SearchResult {
        private String file;
        private int line;
        private String content;
        private String highlight;
    }
}
