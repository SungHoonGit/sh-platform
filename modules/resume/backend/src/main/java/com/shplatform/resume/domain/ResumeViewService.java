package com.shplatform.resume.domain;

import com.shplatform.resume.api.dto.ResumeViewResponse;

/**
 * 이력서 뷰 조립 도메인 서비스.
 */
public interface ResumeViewService {

    /**
     * (질의형) 내 전체 이력을 항목별로 조립하여 반환한다.
     * 인적사항이 미등록이면 profile은 null, 나머지 목록은 비어 있는 채로 반환된다.
     *
     * @param userId 로그인 사용자 ID
     * @return 조립된 이력서 뷰
     */
    ResumeViewResponse getMyResumeView(Long userId);
}
