package com.shplatform.auth.domain.tenant;

import com.shplatform.auth.infrastructure.tenant.*;
import com.shplatform.shared.exception.BusinessException;
import com.shplatform.shared.exception.ErrorCode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class TenantServiceImpl implements TenantService {

    private static final Pattern SLUG_PATTERN = Pattern.compile("^[a-z0-9][a-z0-9\\-]{1,48}[a-z0-9]$");

    private final TenantRepository tenantRepository;
    private final TenantMemberRepository tenantMemberRepository;
    private final TenantInvitationRepository tenantInvitationRepository;

    public TenantServiceImpl(
            TenantRepository tenantRepository,
            TenantMemberRepository tenantMemberRepository,
            TenantInvitationRepository tenantInvitationRepository
    ) {
        this.tenantRepository = tenantRepository;
        this.tenantMemberRepository = tenantMemberRepository;
        this.tenantInvitationRepository = tenantInvitationRepository;
    }

    @Override
    public Tenant createTenant(Long ownerId, String name, String slug) {
        validateSlug(slug);

        if (tenantRepository.existsBySlug(slug)) {
            throw new BusinessException(ErrorCode.DUPLICATE_EMAIL);
        }

        var tenantEntity = TenantEntity.create(name, slug);
        var savedTenant = tenantRepository.save(tenantEntity);

        var ownerMember = TenantMemberEntity.create(
                savedTenant.getId(), ownerId, TenantMemberRole.OWNER
        );
        ownerMember.accept();
        tenantMemberRepository.save(ownerMember);

        return toDomain(savedTenant);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Tenant> getTenantsByUserId(Long userId) {
        return tenantMemberRepository.findByUserId(userId).stream()
                .map(m -> toDomain(m.getTenant()))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Tenant getTenant(Long tenantId) {
        var entity = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        return toDomain(entity);
    }

    @Override
    public Tenant updateTenant(Long tenantId, String name, String domain, String logoUrl) {
        var entity = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));

        if (name != null) entity.setName(name);
        if (domain != null) entity.setDomain(domain);
        if (logoUrl != null) entity.setLogoUrl(logoUrl);

        return toDomain(tenantRepository.save(entity));
    }

    @Override
    public void deleteTenant(Long tenantId) {
        var entity = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        entity.setStatus(TenantStatus.DELETED);
        tenantRepository.save(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TenantMember> getMembers(Long tenantId) {
        return tenantMemberRepository.findByTenantId(tenantId).stream()
                .map(this::toDomainMember)
                .toList();
    }

    @Override
    public void inviteMember(Long tenantId, String email, TenantMemberRole role, Long invitedBy) {
        var tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));

        long currentMemberCount = tenantMemberRepository.countByTenantId(tenantId);
        if (currentMemberCount >= tenant.getMaxUsers()) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }

        var existingInvitation = tenantInvitationRepository.findByTenantIdAndEmail(tenantId, email);
        if (existingInvitation.isPresent()) {
            throw new BusinessException(ErrorCode.DUPLICATE_EMAIL);
        }

        String token = UUID.randomUUID().toString().replace("-", "");
        var invitation = TenantInvitationEntity.create(
                tenantId, email, role, token
        );
        tenantInvitationRepository.save(invitation);
    }

    @Override
    public void acceptInvitation(String token, Long userId) {
        var invitation = tenantInvitationRepository.findByToken(token)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));

        if (invitation.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new BusinessException(ErrorCode.TOKEN_EXPIRED);
        }

        if (invitation.getAcceptedAt() != null) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }

        invitation.setAcceptedAt(LocalDateTime.now());
        tenantInvitationRepository.save(invitation);

        var member = TenantMemberEntity.create(
                invitation.getTenantId(), userId, invitation.getRole()
        );
        member.accept();
        tenantMemberRepository.save(member);
    }

    @Override
    public void changeMemberRole(Long tenantId, Long userId, TenantMemberRole newRole) {
        var member = tenantMemberRepository.findByTenantIdAndUserId(tenantId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));

        if (member.getRole() == TenantMemberRole.OWNER) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }

        member.setRole(newRole);
        tenantMemberRepository.save(member);
    }

    @Override
    public void removeMember(Long tenantId, Long userId) {
        var member = tenantMemberRepository.findByTenantIdAndUserId(tenantId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));

        if (member.getRole() == TenantMemberRole.OWNER) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }

        tenantMemberRepository.delete(member);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isMember(Long tenantId, Long userId) {
        return tenantMemberRepository.findByTenantIdAndUserId(tenantId, userId).isPresent();
    }

    @Override
    @Transactional(readOnly = true)
    public TenantMember getMembership(Long tenantId, Long userId) {
        return tenantMemberRepository.findByTenantIdAndUserId(tenantId, userId)
                .map(this::toDomainMember)
                .orElse(null);
    }

    private void validateSlug(String slug) {
        if (slug == null || slug.isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }
        if (!SLUG_PATTERN.matcher(slug).matches()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }
    }

    private Tenant toDomain(TenantEntity entity) {
        if (entity == null) return null;
        return new Tenant(
                entity.getId(),
                entity.getName(),
                entity.getSlug(),
                entity.getDomain(),
                entity.getLogoUrl(),
                entity.getStatus(),
                entity.getPlanType(),
                entity.getMaxUsers(),
                null,
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    private TenantMember toDomainMember(TenantMemberEntity entity) {
        if (entity == null) return null;
        return new TenantMember(
                entity.getId(),
                entity.getTenantId(),
                entity.getUserId(),
                entity.getRole(),
                entity.getStatus(),
                entity.getInvitedAt(),
                entity.getJoinedAt(),
                entity.getCreatedAt()
        );
    }
}
