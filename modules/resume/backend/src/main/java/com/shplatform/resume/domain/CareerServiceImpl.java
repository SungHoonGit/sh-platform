package com.shplatform.resume.domain;

import com.shplatform.common.exception.BusinessException;
import com.shplatform.common.exception.ErrorCode;
import com.shplatform.resume.api.dto.CareerItemResponse;
import com.shplatform.resume.api.dto.CareerRequest;
import com.shplatform.resume.api.dto.CareerResponse;
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
public class CareerServiceImpl implements CareerService {

    private final ResumeCareerRepository careerRepository;
    private final ResumeCareerItemRepository careerItemRepository;

    @Override
    public List<CareerResponse> getCareers(Long userId, Long documentId) {
        var careers = careerRepository.findByUserIdAndDocumentIdOrderByDisplayOrderAscIdAsc(userId, documentId);
        Map<Long, List<ResumeCareerItemEntity>> itemsByCareer = groupItems(
                careers.stream().map(ResumeCareerEntity::getId).toList());
        return careers.stream()
                .map(e -> toResponse(e, itemsByCareer.getOrDefault(e.getId(), List.of())))
                .toList();
    }

    @Override
    @Transactional
    public CareerResponse createCareer(Long userId, Long documentId, CareerRequest request) {
        var entity = ResumeCareerEntity.create(userId, documentId);
        applyRequest(entity, request);
        return toResponse(careerRepository.save(entity), listItems(entity.getId(), documentId));
    }

    @Override
    @Transactional
    public CareerResponse updateCareer(Long userId, Long careerId, Long documentId, CareerRequest request) {
        var entity = getOwnedCareer(userId, careerId, documentId);
        applyRequest(entity, request);
        return toResponse(careerRepository.save(entity), listItems(entity.getId(), documentId));
    }

    @Override
    @Transactional
    public void deleteCareer(Long userId, Long careerId, Long documentId) {
        var entity = getOwnedCareer(userId, careerId, documentId);
        careerRepository.delete(entity);
    }

    @Override
    @Transactional
    public void reorderCareers(Long userId, Long documentId, List<Long> orderedIds) {
        if (orderedIds == null || orderedIds.isEmpty()) {
            return;
        }
        var owned = careerRepository.findByUserIdAndDocumentIdOrderByDisplayOrderAscIdAsc(userId, documentId);
        Map<Long, ResumeCareerEntity> byId = owned.stream()
                .collect(Collectors.toMap(ResumeCareerEntity::getId, e -> e));
        int order = 1;
        for (Long id : orderedIds) {
            var entity = byId.get(id);
            if (entity == null) {
                throw new BusinessException(ErrorCode.FORBIDDEN);
            }
            entity.setDisplayOrder(order++);
        }
        careerRepository.saveAll(owned);
    }

    private ResumeCareerEntity getOwnedCareer(Long userId, Long careerId, Long documentId) {
        var entity = careerRepository.findById(careerId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        if (!entity.getUserId().equals(userId) || !entity.getDocumentId().equals(documentId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        return entity;
    }

    private List<ResumeCareerItemEntity> listItems(Long careerId, Long documentId) {
        return careerItemRepository.findByCareerIdAndDocumentIdOrderByDisplayOrderAscIdAsc(careerId, documentId);
    }

    private Map<Long, List<ResumeCareerItemEntity>> groupItems(List<Long> careerIds) {
        if (careerIds.isEmpty()) {
            return Map.of();
        }
        return careerItemRepository.findByCareerIdInOrderByDisplayOrderAscIdAsc(careerIds).stream()
                .collect(Collectors.groupingBy(ResumeCareerItemEntity::getCareerId));
    }

    private void applyRequest(ResumeCareerEntity entity, CareerRequest request) {
        entity.setCompany(request.company());
        entity.setTitle(request.title());
        entity.setStartDate(request.startDate());
        entity.setEndDate(request.endDate());
        entity.setDescription(request.description());
        if (request.displayOrder() != null) entity.setDisplayOrder(request.displayOrder());
    }

    private CareerResponse toResponse(ResumeCareerEntity entity, List<ResumeCareerItemEntity> items) {
        List<CareerItemResponse> itemResponses = items.stream()
                .map(it -> new CareerItemResponse(
                        it.getId(),
                        it.getTitle(),
                        it.getStartDate(),
                        it.getEndDate(),
                        it.getDescription(),
                        it.getDisplayOrder(),
                        it.getCreatedAt()
                ))
                .toList();
        return new CareerResponse(
                entity.getId(),
                entity.getCompany(),
                entity.getTitle(),
                entity.getStartDate(),
                entity.getEndDate(),
                entity.getDescription(),
                itemResponses,
                entity.getDisplayOrder(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
