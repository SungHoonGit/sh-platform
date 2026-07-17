package com.shplatform.auth.api.admin;

import com.shplatform.auth.api.admin.dto.*;
import com.shplatform.auth.domain.UserRole;
import com.shplatform.auth.domain.tenant.*;
import com.shplatform.auth.infrastructure.UserEntity;
import com.shplatform.auth.infrastructure.UserRepository;
import com.shplatform.auth.infrastructure.tenant.*;
import com.shplatform.shared.exception.BusinessException;
import com.shplatform.shared.exception.ErrorCode;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class AdminService {

    private final UserRepository userRepository;
    private final TenantRepository tenantRepository;
    private final TenantMemberRepository memberRepository;

    public AdminService(UserRepository userRepository,
                        TenantRepository tenantRepository,
                        TenantMemberRepository memberRepository) {
        this.userRepository = userRepository;
        this.tenantRepository = tenantRepository;
        this.memberRepository = memberRepository;
    }

    // ── 대시보드 ──

    public AdminStatsResponse getStats() {
        long totalUsers = userRepository.count();
        long totalTenants = tenantRepository.count();
        long activeTenants = tenantRepository.countByStatus(TenantStatus.ACTIVE);
        long totalMembers = memberRepository.count();
        return new AdminStatsResponse(totalUsers, totalTenants, activeTenants, totalMembers);
    }

    // ── 사용자 관리 ──

    public Page<UserListResponse> getUsers(String search, String role, Pageable pageable) {
        return userRepository.findAll((root, query, cb) -> {
            var predicates = new java.util.ArrayList<jakarta.persistence.criteria.Predicate>();
            if (search != null && !search.isBlank()) {
                var pattern = "%" + search.toLowerCase() + "%";
                predicates.add(cb.or(
                    cb.like(cb.lower(root.get("email")), pattern),
                    cb.like(cb.lower(root.get("name")), pattern)
                ));
            }
            if (role != null && !role.isBlank()) {
                predicates.add(cb.equal(root.get("role"), UserRole.valueOf(role)));
            }
            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        }, pageable).map(u -> new UserListResponse(
            u.getId(), u.getName(), u.getEmail(), u.getRole(),
            u.getProvider(), u.isEmailVerified(), u.getCreatedAt()
        ));
    }

    public UserListResponse getUser(Long id) {
        var user = userRepository.findById(id)
            .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        return new UserListResponse(
            user.getId(), user.getName(), user.getEmail(), user.getRole(),
            user.getProvider(), user.isEmailVerified(), user.getCreatedAt()
        );
    }

    @Transactional
    public void updateUserRole(Long id, UserRole role) {
        var user = userRepository.findById(id)
            .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        user.setRole(role);
        userRepository.save(user);
    }

    @Transactional
    public void deleteUser(Long id) {
        var user = userRepository.findById(id)
            .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        userRepository.delete(user);
    }

    // ── 테넌트 관리 ──

    public Page<TenantListResponse> getTenants(String search, String planType, Pageable pageable) {
        return tenantRepository.findAll((root, query, cb) -> {
            var predicates = new java.util.ArrayList<jakarta.persistence.criteria.Predicate>();
            if (search != null && !search.isBlank()) {
                var pattern = "%" + search.toLowerCase() + "%";
                predicates.add(cb.or(
                    cb.like(cb.lower(root.get("name")), pattern),
                    cb.like(cb.lower(root.get("slug")), pattern)
                ));
            }
            if (planType != null && !planType.isBlank()) {
                predicates.add(cb.equal(root.get("planType"), TenantPlanType.valueOf(planType)));
            }
            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        }, pageable).map(t -> {
            long memberCount = memberRepository.countByTenantId(t.getId());
            return new TenantListResponse(
                t.getId(), t.getName(), t.getSlug(), t.getPlanType(),
                t.getStatus(), t.getMaxUsers(), memberCount, t.getCreatedAt()
            );
        });
    }

    public TenantDetailResponse getTenant(Long id) {
        var tenant = tenantRepository.findById(id)
            .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        var members = memberRepository.findByTenantId(id).stream()
            .map(m -> {
                var user = userRepository.findById(m.getUserId()).orElse(null);
                return new TenantDetailResponse.MemberInfo(
                    m.getUserId(),
                    user != null ? user.getName() : "Unknown",
                    user != null ? user.getEmail() : "Unknown",
                    m.getRole(), m.getStatus(), m.getJoinedAt()
                );
            }).toList();
        return new TenantDetailResponse(
            tenant.getId(), tenant.getName(), tenant.getSlug(),
            tenant.getPlanType(), tenant.getStatus(), tenant.getMaxUsers(),
            members, tenant.getCreatedAt()
        );
    }

    @Transactional
    public void updateTenant(Long id, UpdateTenantRequest request) {
        var tenant = tenantRepository.findById(id)
            .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        tenant.setPlanType(request.planType());
        tenant.setStatus(request.status());
        tenantRepository.save(tenant);
    }

    @Transactional
    public void deleteTenant(Long id) {
        var tenant = tenantRepository.findById(id)
            .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        tenantRepository.delete(tenant);
    }

    @Transactional
    public void updateMemberRole(Long tenantId, Long userId, TenantMemberRole role) {
        var member = memberRepository.findByTenantIdAndUserId(tenantId, userId)
            .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        member.setRole(role);
        memberRepository.save(member);
    }

    @Transactional
    public void removeMember(Long tenantId, Long userId) {
        var member = memberRepository.findByTenantIdAndUserId(tenantId, userId)
            .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        memberRepository.delete(member);
    }

    @Transactional
    public void inviteMember(Long tenantId, String email) {
        var user = userRepository.findByEmail(email)
            .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        if (memberRepository.existsByTenantIdAndUserId(tenantId, user.getId())) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        var member = TenantMemberEntity.create(tenantId, user.getId(), TenantMemberRole.MEMBER);
        memberRepository.save(member);
    }
}
