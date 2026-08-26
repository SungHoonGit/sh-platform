package com.scraper.platform.service;

import com.scraper.platform.api.dto.JobScrapResponse;

import java.util.List;

/**
 * 공고 스크랩 도메인 서비스.
 */
public interface JobScrapService {

    /**
     * (명령형) 공고를 스크랩한다. 이미 스크랩된 공고면 멱등하게 무시한다.
     *
     * @param userId    로그인 사용자 ID
     * @param postingId 공고 ID
     * @throws com.shplatform.common.exception.BusinessException NOT_FOUND 공고가 없을 때
     */
    void scrap(Long userId, Long postingId);

    /**
     * (명령형) 실시간 검색결과를 저장하고 스크랩한다.
     *
     * <p>공고가 아직 DB에 없으면(오늘자 dedup 기준) 새로 저장한 뒤 스크랩한다.
     * 저장된 공고의 소유자(savedByAccountId)는 요청 사용자이다.
     *
     * @param userId  로그인 사용자 ID
     * @param request 검색 결과 항목
     * @return 스크랩된 공고 ID
     */
    Long scrapLive(Long userId, com.scraper.platform.api.dto.LiveScrapRequest request);

    /**
     * (명령형) 스크랩을 해제한다. 존재하지 않으면 무시한다.
     *
     * @param userId    로그인 사용자 ID
     * @param postingId 공고 ID
     */
    void unscrap(Long userId, Long postingId);

    /**
     * (질의형) 내 스크랩 목록을 최신순으로 조회한다.
     *
     * @param userId 로그인 사용자 ID
     * @return 스크랩 + 공고 정보 목록
     */
    List<JobScrapResponse> getMyScraps(Long userId);

    /**
     * (질의형) 특정 공고의 스크랩 여부를 확인한다.
     *
     * @param userId    로그인 사용자 ID
     * @param postingId 공고 ID
     * @return 스크랩되어 있으면 true
     */
    boolean isScrapped(Long userId, Long postingId);
}
