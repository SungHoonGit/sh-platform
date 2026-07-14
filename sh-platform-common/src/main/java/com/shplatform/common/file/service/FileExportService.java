package com.shplatform.common.file.service;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfWriter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileExportService {

    private final FileReadService fileReadService;

    public byte[] exportToPdf(String rootPath, String relativePath) {
        String mdContent = fileReadService.readRaw(rootPath, relativePath);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            Document document = new Document();
            PdfWriter.getInstance(document, baos);
            document.open();

            Font font;
            try {
                BaseFont bf = BaseFont.createFont(
                        "/usr/share/fonts/truetype/noto/NotoSansCJK-Regular.ttc,0",
                        BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
                font = new Font(bf, 11);
            } catch (Exception e) {
                font = FontFactory.getFont(FontFactory.HELVETICA, 11);
            }

            String[] lines = mdContent.split("\n");
            for (String line : lines) {
                if (line.startsWith("# ")) {
                    document.add(new Paragraph(line.substring(2), new Font(font.getBaseFont(), 16)));
                } else if (line.startsWith("## ")) {
                    document.add(new Paragraph(line.substring(3), new Font(font.getBaseFont(), 14)));
                } else if (line.startsWith("### ")) {
                    document.add(new Paragraph(line.substring(4), new Font(font.getBaseFont(), 12)));
                } else if (!line.trim().isEmpty()) {
                    String cleanLine = line.replaceAll("\\|", " ").trim();
                    if (!cleanLine.matches("^[-\\s]+$")) {
                        document.add(new Paragraph(cleanLine, font));
                    }
                }
            }

            document.close();
        } catch (Exception e) {
            log.error("Error generating PDF", e);
            throw new RuntimeException("Failed to generate PDF", e);
        }

        return baos.toByteArray();
    }

    public byte[] exportToExcel(String rootPath, String relativePath) {
        String mdContent = fileReadService.readRaw(rootPath, relativePath);

        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Job Listings");

            String[] lines = mdContent.split("\n");
            int rowIndex = 0;
            boolean inTable = false;

            for (String line : lines) {
                if (line.contains("|") && line.contains("---")) {
                    inTable = true;
                    continue;
                }

                if (inTable && line.startsWith("|")) {
                    String[] cells = line.split("\\|");
                    Row row = sheet.createRow(rowIndex++);

                    for (int i = 1; i < cells.length - 1; i++) {
                        Cell cell = row.createCell(i - 1);
                        String cellValue = cells[i].trim();

                        Matcher linkMatcher = Pattern.compile("\\[([^\\]]+)\\]\\([^)]+\\)").matcher(cellValue);
                        if (linkMatcher.find()) {
                            cellValue = linkMatcher.group(1);
                        }

                        cell.setCellValue(cellValue);
                    }
                }

                if (inTable && line.trim().isEmpty()) {
                    inTable = false;
                }
            }

            if (sheet.getRow(0) != null) {
                for (int i = 0; i < sheet.getRow(0).getLastCellNum(); i++) {
                    sheet.autoSizeColumn(i);
                }
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            workbook.write(baos);
            return baos.toByteArray();

        } catch (IOException e) {
            log.error("Error generating Excel", e);
            throw new RuntimeException("Failed to generate Excel", e);
        }
    }
}
