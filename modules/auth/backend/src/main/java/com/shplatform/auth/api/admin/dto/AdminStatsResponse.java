package com.shplatform.auth.api.admin.dto;

public record AdminStatsResponse(
    long totalUsers,
    long totalTenants,
    long activeTenants,
    long totalMembers
) {}
