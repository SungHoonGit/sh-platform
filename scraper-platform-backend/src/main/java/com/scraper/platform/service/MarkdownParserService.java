package com.scraper.platform.service;

import com.scraper.platform.model.Category;
import com.scraper.platform.model.CrawlData;
import com.scraper.platform.repository.CrawlDataRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class MarkdownParserService {

    private final CrawlDataRepository crawlDataRepository;
    private final CategoryService categoryService;

    private static final String DATA_DIR = "/home/ubuntu/data/scraper";

    @Transactional
    public int importAllFiles() {
        int totalImported = 0;
        try {
            Path dataPath = Paths.get(DATA_DIR);
            if (!Files.exists(dataPath)) {
                log.warn("Data directory not found: {}", DATA_DIR);
                return 0;
            }
            try (var paths = Files.list(dataPath)) {
                paths.filter(Files::isDirectory).forEach(categoryPath -> {
                    String categoryName = categoryPath.getFileName().toString();
                    try {
                        importCategoryFiles(categoryName, categoryPath);
                    } catch (Exception e) {
                        log.error("Error importing category: {}", categoryName, e);
                    }
                });
            }
        } catch (IOException e) {
            log.error("Error reading data directory", e);
        }
        return totalImported;
    }

    @Transactional
    public int importCategoryFiles(String categoryName, Path categoryPath) throws IOException {
        int imported = 0;
        Category category = findOrCreateCategory(categoryName);
        try (var paths = Files.list(categoryPath)) {
            paths.filter(p -> p.toString().endsWith(".md")).forEach(mdFile -> {
                try {
                    importSingleFile(category, mdFile);
                } catch (Exception e) {
                    log.error("Error importing file: {}", mdFile, e);
                }
            });
        }
        return imported;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public int importSingleFile(Category category, Path mdFile) {
        int imported = 0;
        String fileName = mdFile.getFileName().toString();
        LocalDate fileDate = extractDateFromFileName(fileName);
        if (fileDate == null) {
            log.warn("Cannot extract date from filename: {}", fileName);
            return 0;
        }
        try (BufferedReader reader = new BufferedReader(new FileReader(mdFile.toFile()))) {
            String line;
            boolean inTable = false;
            while ((line = reader.readLine()) != null) {
                if (line.contains("회사명") && line.contains("|")) {
                    inTable = true;
                    continue;
                }
                if (inTable && line.contains("---")) {
                    continue;
                }
                if (inTable && line.startsWith("|")) {
                    try {
                        CrawlData data = parseTableRow(category, line, fileDate, fileName);
                        if (data != null && !isDuplicate(data)) {
                            crawlDataRepository.save(data);
                            imported++;
                        }
                    } catch (Exception e) {
                        log.warn("Error saving row: {}", line, e);
                    }
                }
                if (inTable && line.trim().isEmpty()) {
                    inTable = false;
                }
            }
            log.info("Imported {} items from {}", imported, fileName);
        } catch (IOException e) {
            log.error("Error reading file: {}", mdFile, e);
        }
        return imported;
    }

    private CrawlData parseTableRow(Category category, String line, LocalDate fileDate, String fileName) {
        String[] columns = line.split("\\|");
        if (columns.length < 6) {
            return null;
        }
        String company = columns[1].trim();
        String site = columns[2].trim();
        String titleWithLink = columns[3].trim();
        String techStack = columns.length > 6 ? columns[6].trim() : "";
        if (company.isEmpty() || site.isEmpty() || company.equals("회사명")) {
            return null;
        }
        String title = extractTitle(titleWithLink);
        String url = extractUrl(titleWithLink);
        String tagsJson = convertToJsonArray(techStack);
        return CrawlData.builder()
                .category(category)
                .filePath(fileName)
                .fileName(fileName)
                .title(company + " - " + title)
                .sourceUrl(url)
                .sourceSite(site)
                .author(company)
                .tags(tagsJson)
                .crawledAt(fileDate.atTime(9, 0))
                .build();
    }

    private String convertToJsonArray(String techStack) {
        if (techStack == null || techStack.isEmpty() || techStack.equals("-")) {
            return "[]";
        }
        String[] items = techStack.split(",");
        StringBuilder json = new StringBuilder("[");
        for (int i = 0; i < items.length; i++) {
            String item = items[i].trim().replace("\"", "'");
            if (i > 0) json.append(",");
            json.append("\"").append(item).append("\"");
        }
        json.append("]");
        return json.toString();
    }

    private String extractTitle(String titleWithLink) {
        Pattern pattern = Pattern.compile("\\[([^\\]]+)\\]\\([^)]+\\)");
        Matcher matcher = pattern.matcher(titleWithLink);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return titleWithLink;
    }

    private String extractUrl(String titleWithLink) {
        Pattern pattern = Pattern.compile("\\[([^\\]]+)\\]\\(([^)]+)\\)");
        Matcher matcher = pattern.matcher(titleWithLink);
        if (matcher.find()) {
            return matcher.group(2);
        }
        return "";
    }

    private LocalDate extractDateFromFileName(String fileName) {
        Pattern pattern = Pattern.compile("(\\d{4}-\\d{2}-\\d{2})");
        Matcher matcher = pattern.matcher(fileName);
        if (matcher.find()) {
            return LocalDate.parse(matcher.group(1), DateTimeFormatter.ISO_LOCAL_DATE);
        }
        return null;
    }

    private Category findOrCreateCategory(String categoryName) {
        return categoryService.getAllCategories().stream()
                .filter(c -> c.getSlug().equals(categoryName))
                .findFirst()
                .orElseGet(() -> categoryService.createCategory(categoryName, categoryName, categoryName + " 관련 기술 문서"));
    }

    private boolean isDuplicate(CrawlData data) {
        if (data.getSourceUrl() == null || data.getSourceUrl().isEmpty()) {
            return false;
        }
        return crawlDataRepository.findBySourceUrl(data.getSourceUrl()).size() > 0;
    }
}
