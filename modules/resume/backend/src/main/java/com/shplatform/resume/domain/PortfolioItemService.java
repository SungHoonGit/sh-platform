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
    List<PortfolioItemResponse> getPortfolioItems(Long userId, Long documentId);

    /**
     * (명령형) 포트폴리오 작업물을 추가한다. FILE/LINK 타입을 지원하며
     * FILE 타입은 사전에 파일 업로드 API로 저장된 경로가 필요하다.
     *
     * @param userId  로그인 사용자 ID
     * @param request 작업물 정보
     * @return 생성된 작업물
     * @throws BusinessException INVALID_INPUT FILE 타입인데 filePath가 없을 때
     */
    PortfolioItemResponse createPortfolioItem(Long userId, Long documentId, PortfolioItemRequest request);

    /**
     * (명령형) 포트폴리오 작업물을 수정한다.
     *
     * @param userId  로그인 사용자 ID
     * @param itemId  작업물 ID
     * @param request 수정할 작업물 정보
     * @return 수정된 작업물
     * @throws BusinessException NOT_FOUND 작업물이 없을 때, FORBIDDEN 다른 사용자의 작업물일 때,
     *                          INVALID_INPUT FILE 타입인데 filePath가 없을 때
     */
    PortfolioItemResponse updatePortfolioItem(Long userId, Long itemId, Long documentId, PortfolioItemRequest request);

    /**
     * (명령형) 포트폴리오 작업물을 삭제한다.
     *
     * @param userId 로그인 사용자 ID
     * @param itemId 작업물 ID
     * @throws BusinessException NOT_FOUND 작업물이 없을 때, FORBIDDEN 다른 사용자의 작업물일 때
     */
    void deletePortfolioItem(Long userId, Long itemId, Long documentId);

    /**
     * (명령형) 포트폴리오 작업물의 표시 순서를 재정렬한다.
     *
     * @param userId     로그인 사용자 ID
     * @param orderedIds 새 표시 순서대로 나열한 작업물 ID 목록
     * @throws BusinessException FORBIDDEN 본인 소유가 아닌 작업물 ID가 포함된 경우
     */
    void reorderPortfolioItems(Long userId, Long documentId, List<Long> orderedIds);
}
