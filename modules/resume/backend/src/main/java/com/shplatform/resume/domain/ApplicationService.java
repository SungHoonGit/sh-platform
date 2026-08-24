package com.shplatform.resume.domain;

import com.shplatform.resume.api.dto.ApplicationRequest;
import com.shplatform.resume.api.dto.ApplicationResponse;

import java.util.List;

/**
 * 지원 관리 도메인 서비스.
 */
public interface ApplicationService {

    /**
     * (질의형) 내 전체 지원 목록을 조회한다.
     *
     * @param userId 로그인 사용자 ID
     * @param status 진행 상태 필터 (null이면 전체)
     * @return 지원 목록
     */
    List<ApplicationResponse> getApplications(Long userId, String status);

    /**
     * (명령형) 새 지원을 등록한다.
     *
     * @param userId  로그인 사용자 ID
     * @param request 회사명, 공고 제목 등
     * @return 등록된 지원 정보
     * @throws BusinessException INVALID_INPUT 상태 코드가 유효하지 않을 때
     */
    ApplicationResponse create(Long userId, ApplicationRequest request);

    /**
     * (명령형) 기존 지원을 수정한다.
     *
     * @param userId 로그인 사용자 ID
     * @param id     지원 ID
     * @param request 수정할 내용
     * @return 수정된 지원 정보
     * @throws BusinessException NOT_FOUND 지원이 없거나 다른 사용자의 것일 때
     */
    ApplicationResponse update(Long userId, Long id, ApplicationRequest request);

    /**
     * (명령형) 지원을 삭제한다.
     *
     * @param userId 로그인 사용자 ID
     * @param id     지원 ID
     * @throws BusinessException NOT_FOUND 지원이 없거나 다른 사용자의 것일 때
     */
    void delete(Long userId, Long id);
}
