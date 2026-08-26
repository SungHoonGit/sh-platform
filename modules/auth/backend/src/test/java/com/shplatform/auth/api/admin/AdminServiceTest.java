package com.shplatform.auth.api.admin;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.shplatform.auth.api.admin.dto.UserListResponse;
import com.shplatform.auth.domain.AdminAuditService;
import com.shplatform.auth.domain.UserRole;
import com.shplatform.auth.infrastructure.UserEntity;
import com.shplatform.auth.infrastructure.UserRepository;
import com.shplatform.shared.exception.BusinessException;
import com.shplatform.shared.exception.ErrorCode;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AdminServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private com.shplatform.auth.infrastructure.tenant.TenantRepository tenantRepository;
    @Mock
    private com.shplatform.auth.infrastructure.tenant.TenantMemberRepository memberRepository;
    @Mock
    private AdminAuditService adminAuditService;

    private AdminService adminService;

    @BeforeEach
    void setUp() {
        adminService = new AdminService(userRepository, tenantRepository, memberRepository, adminAuditService);
    }

    private UserEntity user(long id, UserRole role) {
        UserEntity e = new UserEntity();
        e.setId(id);
        e.setEmail("u" + id + "@test.com");
        e.setRole(role);
        return e;
    }

    @Test
    void updateUserRole_shouldSucceed_andAudit() {
        var target = user(2L, UserRole.USER);
        when(userRepository.findById(2L)).thenReturn(Optional.of(target));
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        adminService.updateUserRole(1L, 2L, UserRole.ADMIN);

        assertEquals(UserRole.ADMIN, target.getRole());
        verify(adminAuditService).record(1L, AdminAuditService.ACTION_ROLE_CHANGE, 2L, "USER", "ADMIN", null);
    }

    @Test
    void updateUserRole_shouldBlockSelfChange() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> adminService.updateUserRole(1L, 1L, UserRole.USER));
        assertEquals(ErrorCode.FORBIDDEN, ex.getErrorCode());
        verify(userRepository, never()).save(any());
    }

    @Test
    void updateUserRole_shouldBlockDemotingLastAdmin() {
        when(userRepository.findById(2L)).thenReturn(Optional.of(user(2L, UserRole.ADMIN)));
        when(userRepository.countByRole(UserRole.ADMIN)).thenReturn(1L);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> adminService.updateUserRole(1L, 2L, UserRole.USER));

        assertEquals(ErrorCode.FORBIDDEN, ex.getErrorCode());
        verify(adminAuditService, never()).record(any(), any(), any(), any(), any(), any());
    }

    @Test
    void updateUserRole_shouldAllowDemotingAdmin_whenOtherAdminsExist() {
        when(userRepository.findById(2L)).thenReturn(Optional.of(user(2L, UserRole.ADMIN)));
        when(userRepository.countByRole(UserRole.ADMIN)).thenReturn(2L);
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        adminService.updateUserRole(1L, 2L, UserRole.USER);

        verify(adminAuditService).record(1L, AdminAuditService.ACTION_ROLE_CHANGE, 2L, "ADMIN", "USER", null);
    }

    @Test
    void deleteUser_shouldBlockSelfDelete() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> adminService.deleteUser(1L, 1L));
        assertEquals(ErrorCode.FORBIDDEN, ex.getErrorCode());
        verify(userRepository, never()).delete(any(UserEntity.class));
    }

    @Test
    void deleteUser_shouldThrow_whenNotFound() {
        when(userRepository.findById(9L)).thenReturn(Optional.empty());

        BusinessException ex = assertThrows(BusinessException.class,
                () -> adminService.deleteUser(1L, 9L));
        assertEquals(ErrorCode.NOT_FOUND, ex.getErrorCode());
    }
}
