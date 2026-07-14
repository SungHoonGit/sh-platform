package com.shplatform.common.file.model;

import lombok.*;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FileNode {
    private String name;
    private String path;
    private String type; // "file" or "directory"
    private long size;
    private LocalDateTime modifiedAt;
    private int childCount; // 디렉토리인 경우 하위 항목 수
}
