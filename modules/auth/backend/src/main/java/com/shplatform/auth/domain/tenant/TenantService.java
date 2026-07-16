package com.shplatform.auth.domain.tenant;

import java.util.List;

public interface TenantService {

    /**
     * 테넌트를 생성한다.
     *
     * @param ownerId 테넌트 소유자 ID
     * @param name 테넌트 이름
     * @param slug URL용 식별자
     * @return 생성된 테넌트
     */
    Tenant createTenant(Long ownerId, String name, String slug);

    /**
     * 사용자가 소유한 테넌트 목록을 조회한다.
     *
     * @param userId 사용자 ID
     * @return 테넌트 목록
     */
    List<Tenant> getTenantsByUserId(Long userId);

    /**
     * 테넌트 상세 정보를 조회한다.
     *
     * @param tenantId 테넌트 ID
     * @return 테넌트 상세
     */
    Tenant getTenant(Long tenantId);

    /**
     * 테넌트 정보를 수정한다.
     *
     * @param tenantId 테넌트 ID
     * @param name 테넌트 이름
     * @param domain 커스텀 도메인
     * @param logoUrl 로고 URL
     * @return 수정된 테넌트
     */
    Tenant updateTenant(Long tenantId, String name, String domain, String logoUrl);

    /**
     * 테넌트를 삭제한다 (soft delete).
     *
     * @param tenantId 테넌트 ID
     */
    void deleteTenant(Long tenantId);

    /**
     * 테넌트 멤버 목록을 조회한다.
     *
     * @param tenantId 테넌트 ID
     * @return 멤버 목록
     */
    List<TenantMember> getMembers(Long tenantId);

    /**
     * 테넌트에 멤버를 초대한다.
     *
     * @param tenantId 테넌트 ID
     * @param email 초대할 이메일
     * @param role 역할
     * @param invitedBy 초대한 사용자 ID
     */
    void inviteMember(Long tenantId, String email, TenantMemberRole role, Long invitedBy);

    /**
     * 초대를 수락한다.
     *
     * @param token 초대 토큰
     * @param userId 수락하는 사용자 ID
     */
    void acceptInvitation(String token, Long userId);

    /**
     * 멤버 역할을 변경한다.
     *
     * @param tenantId 테넌트 ID
     * @param userId 대상 사용자 ID
     * @param newRole 새로운 역할
     */
    void changeMemberRole(Long tenantId, Long userId, TenantMemberRole newRole);

    /**
     * 멤버를 제거한다.
     *
     * @param tenantId 테넌트 ID
     * @param userId 대상 사용자 ID
     */
    void removeMember(Long tenantId, Long userId);

    /**
     * 사용자가 특정 테넌트의 멤버인지 확인한다.
     *
     * @param tenantId 테넌트 ID
     * @param userId 사용자 ID
     * @return 멤버 여부
     */
    boolean isMember(Long tenantId, Long userId);

    /**
     * 사용자의 테넌트 멤버십을 조회한다.
     *
     * @param tenantId 테넌트 ID
     * @param userId 사용자 ID
     * @return 멤버 정보 (없으면 null)
     */
    TenantMember getMembership(Long tenantId, Long userId);
}
