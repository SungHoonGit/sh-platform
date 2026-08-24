package com.shplatform.resume.domain;

import com.shplatform.resume.api.dto.IntroductionRequest;
import com.shplatform.resume.api.dto.IntroductionResponse;

import java.util.List;

/**
 * 자기소개 도메인 서비스.
 */
public interface IntroductionService {

    /**
     * (질의형) 내 자기소개 항목 목록을 표시 순서대로 조회한다.
     *
     * @param userId 로그인 사용자 ID
     * @return 자기소개 항목 목록 (display_order ASC, id ASC)
     */
    List<IntroductionResponse> getIntroductions(Long userId);

    /**
     * (명령형) 자기소개 항목을 추가한다.
     *
     * @param userId  로그인 사용자 ID
     * @param request 자기소개 항목 정보
     * @return 생성된 항목
     */
    IntroductionResponse createIntroduction(Long userId, IntroductionRequest request);

    /**
     * (명령형) 자기소개 항목을 수정한다.
     *
     * @param userId         로그인 사용자 ID
     * @param introductionId 항목 ID
     * @param request        수정할 항목 정보
     * @return 수정된 항목
     * @throws BusinessException NOT_FOUND 항목이 없을 때, FORBIDDEN 다른 사용자의 항목일 때
     */
    IntroductionResponse updateIntroduction(Long userId, Long introductionId, IntroductionRequest request);

    /**
     * (명령형) 자기소개 항목을 삭제한다.
     *
     * @param userId         로그인 사용자 ID
     * @param introductionId 항목 ID
     * @throws BusinessException NOT_FOUND 항목이 없을 때, FORBIDDEN 다른 사용자의 항목일 때
     */
    void deleteIntroduction(Long userId, Long introductionId);
}
