package com.shplatform.resume.domain;

import com.shplatform.resume.api.dto.EducationRequest;
import com.shplatform.resume.api.dto.EducationResponse;

import java.util.List;

/**
 * 학력 도메인 서비스.
 */
public interface EducationService {

    /**
     * (질의형) 내 학력 목록을 표시 순서대로 조회한다.
     *
     * @param userId 로그인 사용자 ID
     * @return 학력 목록 (display_order ASC, id ASC)
     */
    List<EducationResponse> getEducations(Long userId);

    /**
     * (명령형) 학력을 추가한다.
     *
     * @param userId  로그인 사용자 ID
     * @param request 학력 정보
     * @return 생성된 학력
     */
    EducationResponse createEducation(Long userId, EducationRequest request);

    /**
     * (명령형) 학력을 수정한다.
     *
     * @param userId      로그인 사용자 ID
     * @param educationId 학력 ID
     * @param request     수정할 학력 정보
     * @return 수정된 학력
     * @throws BusinessException NOT_FOUND 학력이 없을 때, FORBIDDEN 다른 사용자의 학력일 때
     */
    EducationResponse updateEducation(Long userId, Long educationId, EducationRequest request);

    /**
     * (명령형) 학력을 삭제한다.
     *
     * @param userId      로그인 사용자 ID
     * @param educationId 학력 ID
     * @throws BusinessException NOT_FOUND 학력이 없을 때, FORBIDDEN 다른 사용자의 학력일 때
     */
    void deleteEducation(Long userId, Long educationId);
}
