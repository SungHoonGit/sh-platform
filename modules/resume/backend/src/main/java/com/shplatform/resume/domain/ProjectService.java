package com.shplatform.resume.domain;

import com.shplatform.resume.api.dto.ProjectRequest;
import com.shplatform.resume.api.dto.ProjectResponse;

import java.util.List;

/**
 * 프로젝트 도메인 서비스.
 */
public interface ProjectService {

    /**
     * (질의형) 내 프로젝트 목록을 표시 순서대로 조회한다.
     *
     * @param userId 로그인 사용자 ID
     * @return 프로젝트 목록 (display_order ASC, id ASC)
     */
    List<ProjectResponse> getProjects(Long userId);

    /**
     * (명령형) 프로젝트를 추가한다.
     *
     * @param userId  로그인 사용자 ID
     * @param request 프로젝트 정보
     * @return 생성된 프로젝트
     */
    ProjectResponse createProject(Long userId, ProjectRequest request);

    /**
     * (명령형) 프로젝트를 수정한다.
     *
     * @param userId     로그인 사용자 ID
     * @param projectId  프로젝트 ID
     * @param request    수정할 프로젝트 정보
     * @return 수정된 프로젝트
     * @throws BusinessException NOT_FOUND 프로젝트가 없을 때, FORBIDDEN 다른 사용자의 프로젝트일 때
     */
    ProjectResponse updateProject(Long userId, Long projectId, ProjectRequest request);

    /**
     * (명령형) 프로젝트를 삭제한다.
     *
     * @param userId    로그인 사용자 ID
     * @param projectId 프로젝트 ID
     * @throws BusinessException NOT_FOUND 프로젝트가 없을 때, FORBIDDEN 다른 사용자의 프로젝트일 때
     */
    void deleteProject(Long userId, Long projectId);
}
