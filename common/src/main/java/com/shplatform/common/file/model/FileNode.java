package com.shplatform.common.file.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FileNode {
    private String name;
    private String path;
    private String type; // "file" or "directory"
    private long size;
    private LocalDateTime modifiedAt;
    private int childCount;
    private List<FileNode> children; // 디렉토리인 경우 하위 항목
    private int depth; // 트리 깊이 (시각화용)
    private boolean isLast; // 마지막 형제인지 (시각화용)
}
