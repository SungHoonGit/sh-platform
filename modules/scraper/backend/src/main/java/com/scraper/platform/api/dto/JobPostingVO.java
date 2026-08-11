package com.scraper.platform.api.dto;

import cn.idev.excel.annotation.ExcelProperty;
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
public class JobPostingVO {

    @ExcelProperty(value = "사이트", index = 0)
    private String siteName;

    @ExcelProperty(value = "회사명", index = 1)
    private String company;

    @ExcelProperty(value = "포지션", index = 2)
    private String position;

    @ExcelProperty(value = "경력", index = 3)
    private String career;

    @ExcelProperty(value = "기술스택", index = 4)
    private String tech;

    @ExcelProperty(value = "지역", index = 5)
    private String location;

    @ExcelProperty(value = "마감일", index = 6)
    private String deadline;

    @ExcelProperty(value = "URL", index = 7)
    private String url;

    @ExcelProperty(value = "수집일", index = 8)
    private String crawledAt;
}
