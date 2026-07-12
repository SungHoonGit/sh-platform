package com.shplatform.auth.api.dto;

import java.time.LocalDateTime;
import java.util.List;

public record ProviderListResponse(
        List<ProviderInfo> providers
) {
    public record ProviderInfo(
            String provider,
            LocalDateTime connectedAt
    ) {}
}
