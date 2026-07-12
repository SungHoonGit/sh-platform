package com.shplatform.auth.api.dto;

public record ProviderLinkRequest(
        String provider,
        String providerId,
        String providerEmail
) {}
