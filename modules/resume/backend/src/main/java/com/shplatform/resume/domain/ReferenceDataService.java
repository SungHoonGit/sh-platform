package com.shplatform.resume.domain;

import com.shplatform.resume.api.dto.MajorResponse;
import com.shplatform.resume.api.dto.SchoolResponse;

import java.util.List;

/**
 * 기준/마스터 데이터(학교·전공) 조회 서비스.
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
}
