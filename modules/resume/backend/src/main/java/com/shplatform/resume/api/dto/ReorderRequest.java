package com.shplatform.resume.api.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * 표시 순서 재정렬 요청.
 *
 * @param ids 새 표시 순서대로 나열한 항목 ID 목록
 */
public record ReorderRequest(
        @NotEmpty List<@NotNull Long> ids
) {
}