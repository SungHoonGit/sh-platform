package com.shplatform.resume.domain;

import com.shplatform.resume.api.dto.ProfileRequest;
import com.shplatform.resume.api.dto.ProfileResponse;

/**
 * 인적사항 도메인 서비스.
 */
public interface ResumeProfileService {

    /**
     * (질의형) 내 인적사항을 조회한다.
     *
     * @param userId 로그인 사용자 ID
     * @return 인적사항
     * @throws BusinessException NOT_FOUND 등록된 인적사항이 없을 때
     */
    ProfileResponse getMyProfile(Long userId);

    /**
     * (명령형) 내 인적사항을 등록 또는 수정한다(upsert).
     *
     * @param userId  로그인 사용자 ID
     * @param request 인적사항 정보
     * @return 저장된 인적사항
     */
    ProfileResponse upsertProfile(Long userId, ProfileRequest request);
}
