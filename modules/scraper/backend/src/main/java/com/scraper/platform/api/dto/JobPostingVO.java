package com.scraper.platform.api.dto;

import cn.idev.excel.annotation.ExcelProperty;
import cn.idev.excel.annotation.write.style.ColumnWidth;
import cn.idev.excel.annotation.write.style.HeadFontStyle;
import cn.idev.excel.annotation.write.style.HeadStyle;
import cn.idev.excel.enums.BooleanEnum;
import cn.idev.excel.enums.poi.BorderStyleEnum;
import cn.idev.excel.enums.poi.FillPatternTypeEnum;
import cn.idev.excel.enums.poi.HorizontalAlignmentEnum;
import lombok.*;

/**
 * 채용공고 엑셀 내보내기용 VO.
 * FastExcel 어노테이션으로 컬럼 매핑.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@HeadFontStyle(fontName = "Arial", fontHeightInPoints = 10, bold = BooleanEnum.TRUE)
@HeadStyle(
    horizontalAlignment = HorizontalAlignmentEnum.CENTER,
    borderLeft = BorderStyleEnum.THIN,
    borderRight = BorderStyleEnum.THIN,
    borderTop = BorderStyleEnum.THIN,
    borderBottom = BorderStyleEnum.THIN,
    leftBorderColor = 0,
    rightBorderColor = 0,
    topBorderColor = 0,
    bottomBorderColor = 0,
    fillPatternType = FillPatternTypeEnum.SOLID_FOREGROUND,
    fillForegroundColor = 56
)
public class JobPostingVO {

    @ExcelProperty(value = "사이트", index = 0)
    @ColumnWidth(10)
    private String siteName;

    @ExcelProperty(value = "회사명", index = 1)
    @ColumnWidth(20)
    private String company;

    @ExcelProperty(value = "포지션", index = 2)
    @ColumnWidth(30)
    private String position;

    @ExcelProperty(value = "경력", index = 3)
    @ColumnWidth(12)
    private String career;

    @ExcelProperty(value = "기술스택", index = 4)
    @ColumnWidth(25)
    private String tech;

    @ExcelProperty(value = "지역", index = 5)
    @ColumnWidth(15)
    private String location;

    @ExcelProperty(value = "마감일", index = 6)
    @ColumnWidth(12)
    private String deadline;

    @ExcelProperty(value = "URL", index = 7)
    @ColumnWidth(30)
    private String url;

    @ExcelProperty(value = "수집일", index = 8)
    @ColumnWidth(15)
    private String crawledAt;
}
