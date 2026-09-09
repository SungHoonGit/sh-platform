package com.shplatform.resume.domain;

import com.shplatform.resume.api.dto.MajorResponse;
import com.shplatform.resume.api.dto.SchoolResponse;
import com.shplatform.resume.api.dto.SkillMasterResponse;

import java.util.List;

/**
 * 기준/마스터 데이터(학교·전공·기술스택) 조회 서비스.
 */
public interface ReferenceDataService {

    /**
     * (질의형) 학교를 이름으로 검색한다.
     *
     * @param keyword    검색어 (학교명 포함)
     * @param schoolType 학교 유형 필터 (고등학교/대학교/대학원), null이면 전체
     * @return 일치하는 학교 목록 (최대 20건)
     */
    List<SchoolResponse> searchSchools(String keyword, String schoolType);

    /**
     * (질의형) 전공을 이름으로 검색한다.
     *
     * @param keyword 검색어 (전공명 포함)
     * @return 일치하는 전공 목록 (최대 20건)
     */
    List<MajorResponse> searchMajors(String keyword);

    /**
     * (질의형) 기술 스택을 유사검색한다.
     * 정식 이름 부분일치 + 별칭(유사어) 포함을 함께 찾는다 (예: "React" → React (+ React.js·ReactJS 별칭), "스프링" → Spring 등).
     *
     * @param keyword 검색어 (기술명 또는 별칭 포함)
     * @return 일치하는 기술 스택 목록 (최대 20건)
     */
    List<SkillMasterResponse> searchSkills(String keyword);
}
