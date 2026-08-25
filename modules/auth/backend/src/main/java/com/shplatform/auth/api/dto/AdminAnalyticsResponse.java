package com.shplatform.auth.api.dto;

public record AdminAnalyticsResponse(
        long todaySuccessLogins,
        long todayFailedLogins
) {}
