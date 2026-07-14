package com.shplatform.auth.domain.tenant;

import com.shplatform.auth.infrastructure.tenant.*;
import com.shplatform.shared.exception.BusinessException;
import com.shplatform.shared.exception.ErrorCode;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TenantServiceImpl 테스트")
class TenantServiceImplTest {

    @Mock
    private TenantRepository tenantRepository;

    @Mock
    private TenantMemberRepository tenantMemberRepository;

    @Mock
    private TenantInvitationRepository tenantInvitationRepository;

    private TenantServiceImpl tenantService;

    @BeforeEach
    void setUp() {
        tenantService = new TenantServiceImpl(
                tenantRepository,
                tenantMemberRepository,
                tenantInvitationRepository
        );
    }

    @Nested
    @DisplayName("createTenant 메서드")
    class CreateTenant {

        @Test
        @DisplayName("정상적으로 테넌트를 생성한다")
        void createTenant_shouldCreateTenant_whenValidInput() {
            // given
            Long ownerId = 1L;
            String name = "테스트 회사";
            String slug = "test-company";

            given(tenantRepository.existsBySlug(slug)).willReturn(false);
            given(tenantRepository.save(any(TenantEntity.class)))
                    .willReturn(createTenantEntity(name, slug));
            given(tenantMemberRepository.save(any(TenantMemberEntity.class)))
                    .willReturn(createMemberEntity(1L, ownerId));

            // when
            var result = tenantService.createTenant(ownerId, name, slug);

            // then
            assertNotNull(result);
            assertEquals(name, result.name());
            assertEquals(slug, result.slug());
            verify(tenantRepository).save(any(TenantEntity.class));
            verify(tenantMemberRepository).save(any(TenantMemberEntity.class));
        }

        @Test
        @DisplayName("중복된 슬러그로 생성 시 예외가 발생한다")
        void createTenant_shouldThrow_whenDuplicateSlug() {
            // given
            Long ownerId = 1L;
            String name = "테스트 회사";
            String slug = "duplicate-slug";

            given(tenantRepository.existsBySlug(slug)).willReturn(true);

            // when & then
            var ex = assertThrows(BusinessException.class,
                    () -> tenantService.createTenant(ownerId, name, slug));
            assertEquals(ErrorCode.DUPLICATE_EMAIL, ex.getErrorCode());
        }

        @Test
        @DisplayName("잘못된 슬러그 형식으로 생성 시 예외가 발생한다")
        void createTenant_shouldThrow_whenInvalidSlug() {
            // given
            Long ownerId = 1L;
            String name = "테스트 회사";
            String slug = "-invalid-slug-";

            // when & then
            var ex = assertThrows(BusinessException.class,
                    () -> tenantService.createTenant(ownerId, name, slug));
            assertEquals(ErrorCode.INVALID_INPUT, ex.getErrorCode());
        }
    }

    @Nested
    @DisplayName("getTenant 메서드")
    class GetTenant {

        @Test
        @DisplayName("존재하는 테넌트를 조회한다")
        void getTenant_shouldReturnTenant_whenExists() {
            // given
            Long tenantId = 1L;
            given(tenantRepository.findById(tenantId))
                    .willReturn(Optional.of(createTenantEntity("테스트", "test")));

            // when
            var result = tenantService.getTenant(tenantId);

            // then
            assertNotNull(result);
            assertEquals("테스트", result.name());
        }

        @Test
        @DisplayName("존재하지 않는 테넌트 조회 시 예외가 발생한다")
        void getTenant_shouldThrow_whenNotExists() {
            // given
            Long tenantId = 999L;
            given(tenantRepository.findById(tenantId)).willReturn(Optional.empty());

            // when & then
            var ex = assertThrows(BusinessException.class,
                    () -> tenantService.getTenant(tenantId));
            assertEquals(ErrorCode.NOT_FOUND, ex.getErrorCode());
        }
    }

    @Nested
    @DisplayName("inviteMember 메서드")
    class InviteMember {

        @Test
        @DisplayName("정상적으로 멤버를 초대한다")
        void inviteMember_shouldCreateInvitation_whenValidInput() {
            // given
            Long tenantId = 1L;
            String email = "new@example.com";
            TenantMemberRole role = TenantMemberRole.MEMBER;

            given(tenantRepository.findById(tenantId))
                    .willReturn(Optional.of(createTenantEntity("테스트", "test")));
            given(tenantMemberRepository.countByTenantId(tenantId)).willReturn(2L);
            given(tenantInvitationRepository.findByTenantIdAndEmail(tenantId, email))
                    .willReturn(Optional.empty());
            when(tenantInvitationRepository.save(any(TenantInvitationEntity.class)))
                    .thenReturn(createInvitationEntity());

            // when
            tenantService.inviteMember(tenantId, email, role, 1L);

            // then
            verify(tenantInvitationRepository).save(any(TenantInvitationEntity.class));
        }

        @Test
        @DisplayName("최대 인원 초과 시 예외가 발생한다")
        void inviteMember_shouldThrow_whenMemberLimitExceeded() {
            // given
            Long tenantId = 1L;
            String email = "new@example.com";
            TenantMemberRole role = TenantMemberRole.MEMBER;

            var tenant = createTenantEntity("테스트", "test");
            tenant.setMaxUsers(3);
            given(tenantRepository.findById(tenantId)).willReturn(Optional.of(tenant));
            given(tenantMemberRepository.countByTenantId(tenantId)).willReturn(3L);

            // when & then
            var ex = assertThrows(BusinessException.class,
                    () -> tenantService.inviteMember(tenantId, email, role, 1L));
            assertEquals(ErrorCode.FORBIDDEN, ex.getErrorCode());
        }
    }

    @Nested
    @DisplayName("changeMemberRole 메서드")
    class ChangeMemberRole {

        @Test
        @DisplayName("정상적으로 역할을 변경한다")
        void changeMemberRole_shouldChangeRole_whenValidInput() {
            // given
            Long tenantId = 1L;
            Long userId = 2L;
            TenantMemberRole newRole = TenantMemberRole.ADMIN;

            var member = createMemberEntity(tenantId, userId);
            given(tenantMemberRepository.findByTenantIdAndUserId(tenantId, userId))
                    .willReturn(Optional.of(member));
            when(tenantMemberRepository.save(any(TenantMemberEntity.class)))
                    .thenReturn(member);

            // when
            tenantService.changeMemberRole(tenantId, userId, newRole);

            // then
            verify(tenantMemberRepository).save(any(TenantMemberEntity.class));
        }

        @Test
        @DisplayName("OWNER 역할 변경 시 예외가 발생한다")
        void changeMemberRole_shouldThrow_whenOwnerRole() {
            // given
            Long tenantId = 1L;
            Long userId = 1L;

            var member = createMemberEntity(tenantId, userId);
            member.setRole(TenantMemberRole.OWNER);
            given(tenantMemberRepository.findByTenantIdAndUserId(tenantId, userId))
                    .willReturn(Optional.of(member));

            // when & then
            var ex = assertThrows(BusinessException.class,
                    () -> tenantService.changeMemberRole(tenantId, userId, TenantMemberRole.MEMBER));
            assertEquals(ErrorCode.FORBIDDEN, ex.getErrorCode());
        }
    }

    @Nested
    @DisplayName("isMember 메서드")
    class IsMember {

        @Test
        @DisplayName("멤버가 존재하면 true를 반환한다")
        void isMember_shouldReturnTrue_whenMemberExists() {
            // given
            Long tenantId = 1L;
            Long userId = 1L;
            given(tenantMemberRepository.findByTenantIdAndUserId(tenantId, userId))
                    .willReturn(Optional.of(createMemberEntity(tenantId, userId)));

            // when
            var result = tenantService.isMember(tenantId, userId);

            // then
            assertTrue(result);
        }

        @Test
        @DisplayName("멤버가 존재하지 않으면 false를 반환한다")
        void isMember_shouldReturnFalse_whenMemberNotExists() {
            // given
            Long tenantId = 1L;
            Long userId = 999L;
            given(tenantMemberRepository.findByTenantIdAndUserId(tenantId, userId))
                    .willReturn(Optional.empty());

            // when
            var result = tenantService.isMember(tenantId, userId);

            // then
            assertFalse(result);
        }
    }

    // 테스트 헬퍼 메서드
    private TenantEntity createTenantEntity(String name, String slug) {
        return TenantEntity.create(name, slug);
    }

    private TenantMemberEntity createMemberEntity(Long tenantId, Long userId) {
        return TenantMemberEntity.create(tenantId, userId, TenantMemberRole.MEMBER);
    }

    private TenantInvitationEntity createInvitationEntity() {
        return TenantInvitationEntity.create(1L, "test@example.com", TenantMemberRole.MEMBER, "token123");
    }
}
