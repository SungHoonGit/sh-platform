package com.shplatform.auth.api.dto;

import java.util.List;

public record LinkCheckResponse(
        boolean alreadyLinked,
        List<String> linkedProviders
) {}
