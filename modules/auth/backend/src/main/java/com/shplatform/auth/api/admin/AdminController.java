package com.shplatform.auth.api.admin;

import com.shplatform.auth.api.admin.dto.*;
import com.shplatform.auth.domain.UserRole;
import com.shplatform.auth.domain.tenant.TenantMemberRole;
import com.shplatform.shared.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin")
@PreAuthorize("hasRole(\"ADMIN\")")
@Tag(name = "Admin", description = "관리자 API - 사용자/테넌트 관리")
public class AdminController {

    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    // ── 대시보드 ──

    @GetMapping("/stats")
    @Operation(summary = "대시보드 통계")
    public ResponseEntity<ApiResponse<AdminStatsResponse>> getStats() {
        return ResponseEntity.ok(ApiResponse.success(adminService.getStats()));
    }

    // ── 사용자 관리 ──

    @GetMapping("/users")
    @Operation(summary = "사용자 목록")
    public ResponseEntity<ApiResponse<Page<UserListResponse>>> getUsers(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String role,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.success(
            adminService.getUsers(search, role, PageRequest.of(page, size))));
    }

    @GetMapping("/users/{id}")
    @Operation(summary = "사용자 상세")
    public ResponseEntity<ApiResponse<UserListResponse>> getUser(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(adminService.getUser(id)));
    }

    @PutMapping("/users/{id}/role")
    @Operation(summary = "사용자 역할 변경")
    public ResponseEntity<ApiResponse<Void>> updateUserRole(
            @PathVariable Long id, @RequestBody @Valid UpdateUserRequest request) {
        adminService.updateUserRole(id, request.role());
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @DeleteMapping("/users/{id}")
    @Operation(summary = "사용자 삭제")
    public ResponseEntity<ApiResponse<Void>> deleteUser(@PathVariable Long id) {
        adminService.deleteUser(id);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    // ── 테넌트 관리 ──

    @GetMapping("/tenants")
    @Operation(summary = "테넌트 목록")
    public ResponseEntity<ApiResponse<Page<TenantListResponse>>> getTenants(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String planType,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.success(
            adminService.getTenants(search, planType, PageRequest.of(page, size))));
    }

    @GetMapping("/tenants/{id}")
    @Operation(summary = "테넌트 상세")
    public ResponseEntity<ApiResponse<TenantDetailResponse>> getTenant(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(adminService.getTenant(id)));
    }

    @PutMapping("/tenants/{id}")
    @Operation(summary = "테넌트 수정")
    public ResponseEntity<ApiResponse<Void>> updateTenant(
            @PathVariable Long id, @RequestBody @Valid UpdateTenantRequest request) {
        adminService.updateTenant(id, request);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @DeleteMapping("/tenants/{id}")
    @Operation(summary = "테넌트 삭제")
    public ResponseEntity<ApiResponse<Void>> deleteTenant(@PathVariable Long id) {
        adminService.deleteTenant(id);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @PutMapping("/tenants/{tenantId}/members/{userId}/role")
    @Operation(summary = "멤버 역할 변경")
    public ResponseEntity<ApiResponse<Void>> updateMemberRole(
            @PathVariable Long tenantId, @PathVariable Long userId,
            @RequestBody @Valid UpdateMemberRoleRequest request) {
        adminService.updateMemberRole(tenantId, userId, request.role());
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @PostMapping("/tenants/{tenantId}/members")
    @Operation(summary = "멤버 초대")
    public ResponseEntity<ApiResponse<Void>> inviteMember(
            @PathVariable Long tenantId,
            @RequestBody @Valid InviteMemberRequest request) {
        adminService.inviteMember(tenantId, request.email());
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success(null));
    }

    @DeleteMapping("/tenants/{tenantId}/members/{userId}")
    @Operation(summary = "멤버 제거")
    public ResponseEntity<ApiResponse<Void>> removeMember(
            @PathVariable Long tenantId, @PathVariable Long userId) {
        adminService.removeMember(tenantId, userId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
