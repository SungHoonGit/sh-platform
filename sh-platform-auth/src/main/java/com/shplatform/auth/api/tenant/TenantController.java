package com.shplatform.auth.api.tenant;

import com.shplatform.auth.api.tenant.dto.*;
import com.shplatform.auth.domain.tenant.TenantMemberRole;
import com.shplatform.auth.domain.tenant.TenantService;
import com.shplatform.auth.infrastructure.TokenProvider;
import com.shplatform.auth.infrastructure.oauth2.CustomOAuth2User;
import com.shplatform.shared.dto.ApiResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/tenants")
public class TenantController {

    private final TenantService tenantService;

    public TenantController(TenantService tenantService) {
        this.tenantService = tenantService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<TenantResponse>> createTenant(
            @Valid @RequestBody CreateTenantRequest request,
            @AuthenticationPrincipal Object principal
    ) {
        Long userId = getCurrentUserId(principal);
        var tenant = tenantService.createTenant(userId, request.name(), request.slug());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(TenantResponse.from(tenant)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<TenantResponse>>> getMyTenants(
            @AuthenticationPrincipal Object principal
    ) {
        Long userId = getCurrentUserId(principal);
        var tenants = tenantService.getTenantsByUserId(userId);
        var responses = tenants.stream()
                .map(TenantResponse::from)
                .toList();
        return ResponseEntity.ok(ApiResponse.success(responses));
    }

    @GetMapping("/{tenantId}")
    public ResponseEntity<ApiResponse<TenantResponse>> getTenant(
            @PathVariable Long tenantId
    ) {
        var tenant = tenantService.getTenant(tenantId);
        return ResponseEntity.ok(ApiResponse.success(TenantResponse.from(tenant)));
    }

    @PutMapping("/{tenantId}")
    public ResponseEntity<ApiResponse<TenantResponse>> updateTenant(
            @PathVariable Long tenantId,
            @Valid @RequestBody UpdateTenantRequest request
    ) {
        var tenant = tenantService.updateTenant(
                tenantId, request.name(), request.domain(), request.logoUrl()
        );
        return ResponseEntity.ok(ApiResponse.success("테넌트가 수정되었습니다.", TenantResponse.from(tenant)));
    }

    @DeleteMapping("/{tenantId}")
    public ResponseEntity<ApiResponse<Void>> deleteTenant(
            @PathVariable Long tenantId
    ) {
        tenantService.deleteTenant(tenantId);
        return ResponseEntity.ok(ApiResponse.success("테넌트가 삭제되었습니다.", null));
    }

    @GetMapping("/{tenantId}/members")
    public ResponseEntity<ApiResponse<List<MemberResponse>>> getMembers(
            @PathVariable Long tenantId
    ) {
        var members = tenantService.getMembers(tenantId);
        var responses = members.stream()
                .map(MemberResponse::from)
                .toList();
        return ResponseEntity.ok(ApiResponse.success(responses));
    }

    @PostMapping("/{tenantId}/members")
    public ResponseEntity<ApiResponse<Void>> inviteMember(
            @PathVariable Long tenantId,
            @Valid @RequestBody InviteMemberRequest request,
            @AuthenticationPrincipal Object principal
    ) {
        Long userId = getCurrentUserId(principal);
        TenantMemberRole role = request.role() != null
                ? TenantMemberRole.valueOf(request.role())
                : TenantMemberRole.MEMBER;
        tenantService.inviteMember(tenantId, request.email(), role, userId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("초대가 발송되었습니다.", null));
    }

    @PutMapping("/{tenantId}/members/{userId}")
    public ResponseEntity<ApiResponse<Void>> changeMemberRole(
            @PathVariable Long tenantId,
            @PathVariable Long userId,
            @Valid @RequestBody ChangeMemberRoleRequest request
    ) {
        TenantMemberRole role = TenantMemberRole.valueOf(request.role());
        tenantService.changeMemberRole(tenantId, userId, role);
        return ResponseEntity.ok(ApiResponse.success("역할이 변경되었습니다.", null));
    }

    @DeleteMapping("/{tenantId}/members/{userId}")
    public ResponseEntity<ApiResponse<Void>> removeMember(
            @PathVariable Long tenantId,
            @PathVariable Long userId
    ) {
        tenantService.removeMember(tenantId, userId);
        return ResponseEntity.ok(ApiResponse.success("멤버가 제거되었습니다.", null));
    }

    @PostMapping("/invitations/{token}/accept")
    public ResponseEntity<ApiResponse<Void>> acceptInvitation(
            @PathVariable String token,
            @AuthenticationPrincipal Object principal
    ) {
        Long userId = getCurrentUserId(principal);
        tenantService.acceptInvitation(token, userId);
        return ResponseEntity.ok(ApiResponse.success("초대가 수락되었습니다.", null));
    }

    private Long getCurrentUserId(Object principal) {
        if (principal instanceof TokenProvider.Claims claims) {
            return claims.userId();
        }
        if (principal instanceof CustomOAuth2User oauth2User) {
            return oauth2User.getUserId();
        }
        throw new RuntimeException("인증된 사용자를 찾을 수 없습니다.");
    }
}
