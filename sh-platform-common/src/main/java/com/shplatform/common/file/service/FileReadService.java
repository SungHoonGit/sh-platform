package com.shplatform.common.file.service;

import com.vladsch.flexmark.ext.tables.TablesExtension;
import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.ast.Node;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.Arrays;

@Slf4j
@Service
public class FileReadService {

    private final Parser markdownParser;
    private final HtmlRenderer htmlRenderer;

    public FileReadService() {
        this.markdownParser = Parser.builder()
                .extensions(Arrays.asList(TablesExtension.create()))
                .build();
        this.htmlRenderer = HtmlRenderer.builder()
                .extensions(Arrays.asList(TablesExtension.create()))
                .build();
    }

    public String readRaw(String rootPath, String relativePath) {
        Path filePath = resolveAndValidate(rootPath, relativePath);
        try {
            return Files.readString(filePath, StandardCharsets.UTF_8);
        } catch (IOException e) {
            log.error("Error reading file: {}", filePath, e);
            throw new RuntimeException("Failed to read file: " + relativePath);
        }
    }

    public String readAsHtml(String rootPath, String relativePath) {
        String mdContent = readRaw(rootPath, relativePath);
        Node document = markdownParser.parse(mdContent);
        return htmlRenderer.render(document);
    }

    public String readFileTitle(String rootPath, String relativePath) {
        String content = readRaw(rootPath, relativePath);
        String[] lines = content.split("\n");
        for (String line : lines) {
            if (line.startsWith("# ")) {
                return line.substring(2).trim();
            }
        }
        return relativePath;
    }

    private Path resolveAndValidate(String rootPath, String relativePath) {
        Path basePath = Paths.get(rootPath).toAbsolutePath().normalize();
        Path filePath = basePath.resolve(relativePath).normalize();

        if (!filePath.startsWith(basePath)) {
            throw new SecurityException("Path traversal detected: " + relativePath);
        }

        if (!Files.exists(filePath) || !Files.isRegularFile(filePath)) {
            throw new IllegalArgumentException("File not found: " + relativePath);
        }

        if (!filePath.toString().endsWith(".md")) {
            throw new IllegalArgumentException("Only .md files are allowed: " + relativePath);
        }

        return filePath;
    }
}
