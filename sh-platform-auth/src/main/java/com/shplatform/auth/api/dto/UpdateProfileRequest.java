package com.shplatform.auth.api.dto;

import jakarta.validation.constraints.Size;

public record UpdateProfileRequest(
        @Size(min = 2, max = 20) String name,
        @Size(max = 10) String locale
) {}
