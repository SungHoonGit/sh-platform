package com.shplatform.auth.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "OAuth2 연결 확인 응답")
public record LinkCheckResponse(
        @Schema(description = "이미 연결된 provider인지 여부") boolean alreadyLinked,
        @Schema(description = "연결된 provider 목록") List<String> linkedProviders
) {}
