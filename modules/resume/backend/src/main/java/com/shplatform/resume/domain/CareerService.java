package com.shplatform.resume.domain;

import com.shplatform.resume.api.dto.CareerRequest;
import com.shplatform.resume.api.dto.CareerResponse;

import java.util.List;

/**
 * 경력 도메인 서비스.
 */
public interface CareerService {

    /**
     * (질의형) 내 경력 목록을 표시 순서대로 조회한다.
     *
     * @param userId 로그인 사용자 ID
     * @return 경력 목록 (display_order ASC, id ASC)
     */
    List<CareerResponse> getCareers(Long userId);

    /**
     * (명령형) 경력을 추가한다.
     *
     * @param userId  로그인 사용자 ID
     * @param request 경력 정보
     * @return 생성된 경력
     */
    CareerResponse createCareer(Long userId, CareerRequest request);

    /**
     * (명령형) 경력을 수정한다.
     *
     * @param userId   로그인 사용자 ID
     * @param careerId 경력 ID
     * @param request  수정할 경력 정보
     * @return 수정된 경력
     * @throws BusinessException NOT_FOUND 경력이 없을 때, FORBIDDEN 다른 사용자의 경력일 때
     */
    CareerResponse updateCareer(Long userId, Long careerId, CareerRequest request);

    /**
     * (명령형) 경력을 삭제한다.
     *
     * @param userId   로그인 사용자 ID
     * @param careerId 경력 ID
     * @throws BusinessException NOT_FOUND 경력이 없을 때, FORBIDDEN 다른 사용자의 경력일 때
     */
    void deleteCareer(Long userId, Long careerId);
}
