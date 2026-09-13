package com.shplatform.resume.domain;

import com.shplatform.resume.api.dto.SkillRequest;
import com.shplatform.resume.api.dto.SkillResponse;

import java.util.List;

/**
 * 스킬 도메인 서비스.
 */
public interface SkillService {

    /**
     * (질의형) 내 스킬 목록을 표시 순서대로 조회한다.
     *
     * @param userId 로그인 사용자 ID
     * @return 스킬 목록 (display_order ASC, id ASC)
     */
    List<SkillResponse> getSkills(Long userId, Long documentId);

    /**
     * (명령형) 스킬을 추가한다.
     *
     * @param userId  로그인 사용자 ID
     * @param request 스킬 정보
     * @return 생성된 스킬
     */
    SkillResponse createSkill(Long userId, Long documentId, SkillRequest request);

    /**
     * (명령형) 스킬을 수정한다.
     *
     * @param userId  로그인 사용자 ID
     * @param skillId 스킬 ID
     * @param request 수정할 스킬 정보
     * @return 수정된 스킬
     * @throws BusinessException NOT_FOUND 스킬이 없을 때, FORBIDDEN 다른 사용자의 스킬일 때
     */
    SkillResponse updateSkill(Long userId, Long skillId, Long documentId, SkillRequest request);

    /**
     * (명령형) 스킬을 삭제한다.
     *
     * @param userId  로그인 사용자 ID
     * @param skillId 스킬 ID
     * @throws BusinessException NOT_FOUND 스킬이 없을 때, FORBIDDEN 다른 사용자의 스킬일 때
     */
    void deleteSkill(Long userId, Long skillId, Long documentId);

    /**
     * (명령형) 스킬의 표시 순서를 재정렬한다.
     *
     * @param userId      로그인 사용자 ID
     * @param orderedIds 새 표시 순서대로 나열한 스킬 ID 목록
     * @throws BusinessException FORBIDDEN 본인 소유가 아닌 스킬 ID가 포함된 경우
     */
    void reorderSkills(Long userId, Long documentId, List<Long> orderedIds);
}
