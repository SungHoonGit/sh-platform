package com.shplatform.resume.api.dto;

import java.util.List;

/**
 * 공고-이력서 기술 매칭 응답.
 *
 * @param matchedSkills 내 이력서와 일치한 기술명 목록 (마스터 정식명 기준)
 * @param matchCount    일치한 기술 수
 * @param requestedCount 공고에서 요청 조회된 기술 수
 * @param mySkillCount  내 이력서에 등록된 전체 기술 수
 */
public record SkillMatchResponse(
        List<String> matchedSkills,
        int matchCount,
        int requestedCount,
        int mySkillCount) {
}