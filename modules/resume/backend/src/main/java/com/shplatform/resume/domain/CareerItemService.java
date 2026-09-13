package com.shplatform.resume.domain;

import com.shplatform.resume.api.dto.CareerItemRequest;
import com.shplatform.resume.api.dto.CareerItemResponse;

import java.util.List;

public interface CareerItemService {

    /**
     * (질의형) 특정 경력의 기간별 상세 항목 목록을 조회한다.
     *
     * @param userId   현재 사용자 ID
     * @param careerId 경력 ID
     * @return 상세 항목 목록 (표시 순서순)
     * @throws com.shplatform.common.exception.BusinessException NOT_FOUND (경력 없음), FORBIDDEN (타인 소유)
     */
    List<CareerItemResponse> getCareerItems(Long userId, Long careerId, Long documentId);

    /**
     * (명령형) 경력에 기간별 상세 항목을 추가한다.
     *
     * @param userId   현재 사용자 ID
     * @param careerId 경력 ID
     * @param request  제목, 기간, 내용
     * @return 생성된 상세 항목
     * @throws com.shplatform.common.exception.BusinessException NOT_FOUND, FORBIDDEN
     */
    CareerItemResponse createCareerItem(Long userId, Long careerId, Long documentId, CareerItemRequest request);

    /**
     * (명령형) 경력의 상세 항목을 수정한다.
     *
     * @param userId   현재 사용자 ID
     * @param careerId 경력 ID
     * @param itemId   상세 항목 ID
     * @param request  제목, 기간, 내용
     * @return 수정된 상세 항목
     * @throws com.shplatform.common.exception.BusinessException NOT_FOUND, FORBIDDEN
     */
    CareerItemResponse updateCareerItem(Long userId, Long careerId, Long itemId, Long documentId, CareerItemRequest request);

    /**
     * (명령형) 경력의 상세 항목을 삭제한다.
     *
     * @param userId   현재 사용자 ID
     * @param careerId 경력 ID
     * @param itemId   상세 항목 ID
     * @throws com.shplatform.common.exception.BusinessException NOT_FOUND, FORBIDDEN
     */
    void deleteCareerItem(Long userId, Long careerId, Long itemId, Long documentId);

    /**
     * (명령형) 상세 항목의 표시 순서를 재정렬한다.
     *
     * @param userId     현재 사용자 ID
     * @param careerId   경력 ID
     * @param orderedIds 재배치할 항목 ID 목록 (첫 번째가 위)
     * @throws com.shplatform.common.exception.BusinessException NOT_FOUND, FORBIDDEN
     */
    void reorderCareerItems(Long userId, Long careerId, Long documentId, List<Long> orderedIds);
}