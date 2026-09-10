package com.shplatform.resume.domain;

import com.shplatform.resume.api.dto.SkillMatchResponse;

/**
 * 공고-이력서 기술 매칭 서비스.
 */
public interface SkillMatchService {

    /**
     * (질의형) 공고의 기술 스택과 내 이력서(프로젝트·작업물) 기술 스택을
     * 기술 마스터(정식명/별칭, 대소문자 무시) 기준으로 비교해 일치 기술을 계산한다.
     *
     * @param userId    사용자 ID
     * @param techStack 공고의 기술 스택 문자열 (콤마 구분)
     * @return 일치 기술 목록과 개수
     */
    SkillMatchResponse match(Long userId, String techStack);
}