package com.shplatform.auth.api.dto;

import java.util.List;

public record AdminSessionResponse(
        Long userId,
        int activeSessionCount,
        List<String> sessionIds
) {}
