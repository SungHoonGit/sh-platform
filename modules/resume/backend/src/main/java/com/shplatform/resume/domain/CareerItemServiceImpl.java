package com.shplatform.resume.domain;

import com.shplatform.common.exception.BusinessException;
import com.shplatform.common.exception.ErrorCode;
import com.shplatform.resume.api.dto.CareerItemRequest;
import com.shplatform.resume.api.dto.CareerItemResponse;
import com.shplatform.resume.infrastructure.entity.ResumeCareerEntity;
import com.shplatform.resume.infrastructure.entity.ResumeCareerItemEntity;
import com.shplatform.resume.infrastructure.repository.ResumeCareerItemRepository;
import com.shplatform.resume.infrastructure.repository.ResumeCareerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CareerItemServiceImpl implements CareerItemService {

    private final ResumeCareerItemRepository careerItemRepository;
    private final ResumeCareerRepository careerRepository;

    @Override
    public List<CareerItemResponse> getCareerItems(Long userId, Long careerId, Long documentId) {
        requireOwnedCareer(userId, careerId, documentId);
        return careerItemRepository.findByCareerIdAndDocumentIdOrderByDisplayOrderAscIdAsc(careerId, documentId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public CareerItemResponse createCareerItem(Long userId, Long careerId, Long documentId, CareerItemRequest request) {
        requireOwnedCareer(userId, careerId, documentId);
        var entity = ResumeCareerItemEntity.create(careerId, documentId);
        applyRequest(entity, request);
        return toResponse(careerItemRepository.save(entity));
    }

    @Override
    @Transactional
    public CareerItemResponse updateCareerItem(Long userId, Long careerId, Long itemId, Long documentId, CareerItemRequest request) {
        requireOwnedCareer(userId, careerId, documentId);
        var entity = getOwnedItem(careerId, itemId, documentId);
        applyRequest(entity, request);
        return toResponse(careerItemRepository.save(entity));
    }

    @Override
    @Transactional
    public void deleteCareerItem(Long userId, Long careerId, Long itemId, Long documentId) {
        requireOwnedCareer(userId, careerId, documentId);
        careerItemRepository.delete(getOwnedItem(careerId, itemId, documentId));
    }

    @Override
    @Transactional
    public void reorderCareerItems(Long userId, Long careerId, Long documentId, List<Long> orderedIds) {
        if (orderedIds == null || orderedIds.isEmpty()) {
            return;
        }
        requireOwnedCareer(userId, careerId, documentId);
        var owned = careerItemRepository.findByCareerIdAndDocumentIdOrderByDisplayOrderAscIdAsc(careerId, documentId);
        Map<Long, ResumeCareerItemEntity> byId = owned.stream()
                .collect(Collectors.toMap(ResumeCareerItemEntity::getId, e -> e));
        int order = 1;
        for (Long id : orderedIds) {
            var entity = byId.get(id);
            if (entity == null) {
                throw new BusinessException(ErrorCode.FORBIDDEN);
            }
            entity.setDisplayOrder(order++);
        }
        careerItemRepository.saveAll(owned);
    }

    private void requireOwnedCareer(Long userId, Long careerId, Long documentId) {
        ResumeCareerEntity career = careerRepository.findById(careerId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        if (!career.getUserId().equals(userId) || !career.getDocumentId().equals(documentId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
    }

    private ResumeCareerItemEntity getOwnedItem(Long careerId, Long itemId, Long documentId) {
        var entity = careerItemRepository.findById(itemId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        if (!entity.getCareerId().equals(careerId) || !entity.getDocumentId().equals(documentId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        return entity;
    }

    private void applyRequest(ResumeCareerItemEntity entity, CareerItemRequest request) {
        entity.setTitle(request.title());
        entity.setStartDate(request.startDate());
        entity.setEndDate(request.endDate());
        entity.setDescription(request.description());
        entity.setDisplayOrder(request.displayOrder() != null ? request.displayOrder() : 0);
    }

    private CareerItemResponse toResponse(ResumeCareerItemEntity entity) {
        return new CareerItemResponse(
                entity.getId(),
                entity.getTitle(),
                entity.getStartDate(),
                entity.getEndDate(),
                entity.getDescription(),
                entity.getDisplayOrder(),
                entity.getCreatedAt()
        );
    }
}