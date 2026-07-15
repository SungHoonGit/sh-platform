package com.shplatform.common.file.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobItem {
    private String site;
    private String company;
    private String position;
    private String career;
    private String tech;
    private String location;
    private String deadline;
    private String url;
}
