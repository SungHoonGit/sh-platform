package com.shplatform.resume.domain;

import com.shplatform.common.exception.BusinessException;
import com.shplatform.common.exception.ErrorCode;
import com.shplatform.resume.api.dto.IntroductionRequest;
import com.shplatform.resume.api.dto.IntroductionResponse;
import com.shplatform.resume.infrastructure.entity.ResumeIntroductionEntity;
import com.shplatform.resume.infrastructure.repository.ResumeIntroductionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class IntroductionServiceImpl implements IntroductionService {

    private final ResumeIntroductionRepository introductionRepository;

    @Override
    public List<IntroductionResponse> getIntroductions(Long userId, Long documentId) {
        return introductionRepository.findByUserIdAndDocumentIdOrderByDisplayOrderAscIdAsc(userId, documentId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public IntroductionResponse createIntroduction(Long userId, Long documentId, IntroductionRequest request) {
        var entity = ResumeIntroductionEntity.create(userId, documentId);
        applyRequest(entity, request);
        return toResponse(introductionRepository.save(entity));
    }

    @Override
    @Transactional
    public IntroductionResponse updateIntroduction(Long userId, Long documentId, Long introductionId, IntroductionRequest request) {
        var entity = getOwnedIntroduction(userId, documentId, introductionId);
        applyRequest(entity, request);
        return toResponse(introductionRepository.save(entity));
    }

    @Override
    @Transactional
    public void deleteIntroduction(Long userId, Long documentId, Long introductionId) {
        var entity = getOwnedIntroduction(userId, documentId, introductionId);
        introductionRepository.delete(entity);
    }

    @Override
    @Transactional
    public void reorderIntroductions(Long userId, Long documentId, List<Long> orderedIds) {
        if (orderedIds == null || orderedIds.isEmpty()) {
            return;
        }
        var owned = introductionRepository.findByUserIdAndDocumentIdOrderByDisplayOrderAscIdAsc(userId, documentId);
        Map<Long, ResumeIntroductionEntity> byId = owned.stream()
                .collect(Collectors.toMap(ResumeIntroductionEntity::getId, e -> e));
        int order = 1;
        for (Long id : orderedIds) {
            var entity = byId.get(id);
            if (entity == null) {
                throw new BusinessException(ErrorCode.FORBIDDEN);
            }
            entity.setDisplayOrder(order++);
        }
        introductionRepository.saveAll(owned);
    }

    private ResumeIntroductionEntity getOwnedIntroduction(Long userId, Long documentId, Long introductionId) {
        var entity = introductionRepository.findById(introductionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        if (!entity.getUserId().equals(userId) || !entity.getDocumentId().equals(documentId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        return entity;
    }

    private void applyRequest(ResumeIntroductionEntity entity, IntroductionRequest request) {
        entity.setTitle(request.title());
        entity.setContent(request.content());
        if (request.displayOrder() != null) entity.setDisplayOrder(request.displayOrder());
    }

    private IntroductionResponse toResponse(ResumeIntroductionEntity entity) {
        return new IntroductionResponse(
                entity.getId(),
                entity.getTitle(),
                entity.getContent(),
                entity.getDisplayOrder(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
