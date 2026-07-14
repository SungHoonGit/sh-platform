package com.shplatform.auth.api.tenant.dto;

import jakarta.validation.constraints.Size;

public record UpdateTenantRequest(
        @Size(max = 100, message = "테넌트 이름은 100자 이내여야 합니다.")
        String name,

        @Size(max = 100, message = "도메인은 100자 이내여야 합니다.")
        String domain,

        @Size(max = 500, message = "로고 URL은 500자 이내여야 합니다.")
        String logoUrl
) {}
