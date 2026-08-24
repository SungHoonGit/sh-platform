package com.shplatform.resume.domain;

import com.shplatform.resume.api.dto.PortfolioItemRequest;
import com.shplatform.resume.api.dto.PortfolioItemResponse;

import java.util.List;

/**
 * 포트폴리오 작업물 도메인 서비스.
 */
public interface PortfolioItemService {

    /**
     * (질의형) 내 포트폴리오 작업물 목록을 표시 순서대로 조회한다.
     *
     * @param userId 로그인 사용자 ID
     * @return 작업물 목록 (display_order ASC, id ASC)
     */
    List<PortfolioItemResponse> getPortfolioItems(Long userId);

    /**
     * (명령형) 포트폴리오 작업물을 추가한다. 파일 업로드는 Phase 5에서 지원 예정이며
     * 현재는 LINK 타입만 허용한다.
     *
     * @param userId  로그인 사용자 ID
     * @param request 작업물 정보
     * @return 생성된 작업물
     * @throws BusinessException INVALID_INPUT FILE 타입으로 요청했을 때
     */
    PortfolioItemResponse createPortfolioItem(Long userId, PortfolioItemRequest request);

    /**
     * (명령형) 포트폴리오 작업물을 수정한다.
     *
     * @param userId  로그인 사용자 ID
     * @param itemId  작업물 ID
     * @param request 수정할 작업물 정보
     * @return 수정된 작업물
     * @throws BusinessException NOT_FOUND 작업물이 없을 때, FORBIDDEN 다른 사용자의 작업물일 때,
     *                          INVALID_INPUT FILE 타입으로 요청했을 때
     */
    PortfolioItemResponse updatePortfolioItem(Long userId, Long itemId, PortfolioItemRequest request);

    /**
     * (명령형) 포트폴리오 작업물을 삭제한다.
     *
     * @param userId 로그인 사용자 ID
     * @param itemId 작업물 ID
     * @throws BusinessException NOT_FOUND 작업물이 없을 때, FORBIDDEN 다른 사용자의 작업물일 때
     */
    void deletePortfolioItem(Long userId, Long itemId);
}
