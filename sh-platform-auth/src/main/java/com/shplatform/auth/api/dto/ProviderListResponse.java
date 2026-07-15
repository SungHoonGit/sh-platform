package com.shplatform.auth.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "연결된 OAuth2 provider 목록 응답")
public record ProviderListResponse(
        @Schema(description = "provider 목록") List<ProviderInfo> providers
) {
    @Schema(description = "연결된 provider 정보")
    public record ProviderInfo(
            @Schema(description = "OAuth2 provider", example = "kakao") String provider,
            @Schema(description = "연결 일시") LocalDateTime connectedAt
    ) {}
}
