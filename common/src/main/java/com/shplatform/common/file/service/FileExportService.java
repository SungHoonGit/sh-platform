package com.shplatform.common.file.service;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
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
            Document document = new Document(PageSize.A4, 30, 30, 30, 30);
            PdfWriter.getInstance(document, baos);
            document.open();

            BaseFont bf;
            com.lowagie.text.Font titleFont;
            com.lowagie.text.Font headerFont;
            com.lowagie.text.Font bodyFont;

            try {
                bf = BaseFont.createFont(
                        "/usr/share/fonts/truetype/noto/NotoSansCJK-Regular.ttc,0",
                        BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
                titleFont = new com.lowagie.text.Font(bf, 18, com.lowagie.text.Font.BOLD);
                headerFont = new com.lowagie.text.Font(bf, 10, com.lowagie.text.Font.BOLD);
                bodyFont = new com.lowagie.text.Font(bf, 9);
            } catch (Exception e) {
                bf = BaseFont.createFont();
                titleFont = new com.lowagie.text.Font(bf, 18, com.lowagie.text.Font.BOLD);
                headerFont = new com.lowagie.text.Font(bf, 10, com.lowagie.text.Font.BOLD);
                bodyFont = new com.lowagie.text.Font(bf, 9);
            }

            String[] lines = mdContent.split("\n");
            boolean inTable = false;
            List<String[]> tableRows = new ArrayList<>();

            for (String line : lines) {
                if (line.startsWith("# ")) {
                    flushTable(document, tableRows, headerFont, bodyFont);
                    tableRows.clear();
                    inTable = false;
                    document.add(new Paragraph(line.substring(2).trim(), titleFont));
                    document.add(new Paragraph(" "));
                } else if (line.startsWith("## ")) {
                    flushTable(document, tableRows, headerFont, bodyFont);
                    tableRows.clear();
                    inTable = false;
                    document.add(new Paragraph(line.substring(3).trim(), headerFont));
                    document.add(new Paragraph(" "));
                } else if (line.startsWith("### ")) {
                    flushTable(document, tableRows, headerFont, bodyFont);
                    tableRows.clear();
                    inTable = false;
                    document.add(new Paragraph(line.substring(4).trim(), bodyFont));
                    document.add(new Paragraph(" "));
                } else if (line.contains("|") && line.contains("---")) {
                    inTable = true;
                } else if (inTable && line.startsWith("|")) {
                    String[] cells = line.split("\\|");
                    List<String> row = new ArrayList<>();
                    for (int i = 1; i < cells.length - 1; i++) {
                        String cellValue = cells[i].trim();
                        Matcher linkMatcher = Pattern.compile("\\[([^\\]]+)\\]\\([^)]+\\)").matcher(cellValue);
                        if (linkMatcher.find()) {
                            cellValue = linkMatcher.group(1);
                        }
                        row.add(cellValue);
                    }
                    tableRows.add(row.toArray(new String[0]));
                } else if (inTable && line.trim().isEmpty()) {
                    flushTable(document, tableRows, headerFont, bodyFont);
                    tableRows.clear();
                    inTable = false;
                } else if (!line.trim().isEmpty()) {
                    String cleanLine = line.replaceAll("\\|", " ").trim();
                    if (!cleanLine.matches("^[-\\s]+$")) {
                        document.add(new Paragraph(cleanLine, bodyFont));
                    }
                }
            }

            flushTable(document, tableRows, headerFont, bodyFont);
            document.close();
        } catch (Exception e) {
            log.error("Error generating PDF", e);
            throw new RuntimeException("Failed to generate PDF", e);
        }

        return baos.toByteArray();
    }

    private void flushTable(Document document, List<String[]> rows,
                            com.lowagie.text.Font headerFont,
                            com.lowagie.text.Font bodyFont) throws DocumentException {
        if (rows.isEmpty()) return;

        int cols = rows.get(0).length;
        PdfPTable table = new PdfPTable(cols);
        table.setWidthPercentage(100);
        table.setSpacingBefore(5);
        table.setSpacingAfter(5);

        for (int i = 0; i < rows.size(); i++) {
            String[] row = rows.get(i);
            com.lowagie.text.Font font = (i == 0) ? headerFont : bodyFont;

            for (int j = 0; j < cols; j++) {
                String cellText = (j < row.length) ? row[j] : "";
                PdfPCell cell = new PdfPCell(new Phrase(cellText, font));
                cell.setPadding(5);

                if (i == 0) {
                    cell.setBackgroundColor(new java.awt.Color(240, 240, 240));
                    cell.setBorderWidth(1);
                } else {
                    cell.setBorderWidth(0.5f);
                }

                table.addCell(cell);
            }
        }

        document.add(table);
    }

    public byte[] exportToExcel(String rootPath, String relativePath) {
        String mdContent = fileReadService.readRaw(rootPath, relativePath);

        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Job Listings");

            CellStyle headerStyle = workbook.createCellStyle();
            org.apache.poi.ss.usermodel.Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setFontHeightInPoints((short) 11);
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setBorderBottom(BorderStyle.THIN);
            headerStyle.setBorderTop(BorderStyle.THIN);
            headerStyle.setBorderLeft(BorderStyle.THIN);
            headerStyle.setBorderRight(BorderStyle.THIN);

            CellStyle bodyStyle = workbook.createCellStyle();
            bodyStyle.setBorderBottom(BorderStyle.THIN);
            bodyStyle.setBorderTop(BorderStyle.THIN);
            bodyStyle.setBorderLeft(BorderStyle.THIN);
            bodyStyle.setBorderRight(BorderStyle.THIN);
            bodyStyle.setWrapText(true);

            String[] lines = mdContent.split("\n");
            int rowIndex = 0;
            boolean inTable = false;
            boolean isHeader = false;

            for (String line : lines) {
                if (line.contains("|") && line.contains("---")) {
                    inTable = true;
                    isHeader = true;
                    continue;
                }

                if (inTable && line.startsWith("|")) {
                    String[] cells = line.split("\\|");
                    Row row = sheet.createRow(rowIndex++);

                    for (int i = 1; i < cells.length - 1; i++) {
                        org.apache.poi.ss.usermodel.Cell cell = row.createCell(i - 1);
                        String cellValue = cells[i].trim();

                        Matcher linkMatcher = Pattern.compile("\\[([^\\]]+)\\]\\([^)]+\\)").matcher(cellValue);
                        if (linkMatcher.find()) {
                            cellValue = linkMatcher.group(1);
                        }

                        cell.setCellValue(cellValue);

                        if (isHeader) {
                            cell.setCellStyle(headerStyle);
                        } else {
                            cell.setCellStyle(bodyStyle);
                        }
                    }
                    isHeader = false;
                }

                if (inTable && line.trim().isEmpty()) {
                    inTable = false;
                }
            }

            if (sheet.getRow(0) != null) {
                for (int i = 0; i < sheet.getRow(0).getLastCellNum(); i++) {
                    sheet.autoSizeColumn(i);
                    if (sheet.getColumnWidth(i) < 3000) {
                        sheet.setColumnWidth(i, 3000);
                    }
                }
                sheet.setAutoFilter(new org.apache.poi.ss.util.CellRangeAddress(
                        0, 0, 0, sheet.getRow(0).getLastCellNum() - 1));
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
