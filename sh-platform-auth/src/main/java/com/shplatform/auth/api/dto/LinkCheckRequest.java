package com.shplatform.auth.api.dto;

public record LinkCheckRequest(
        String provider,
        String providerId
) {}
