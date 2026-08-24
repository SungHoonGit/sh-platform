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

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class IntroductionServiceImpl implements IntroductionService {

    private final ResumeIntroductionRepository introductionRepository;

    @Override
    public List<IntroductionResponse> getIntroductions(Long userId) {
        return introductionRepository.findByUserIdOrderByDisplayOrderAscIdAsc(userId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public IntroductionResponse createIntroduction(Long userId, IntroductionRequest request) {
        var entity = ResumeIntroductionEntity.create(userId);
        applyRequest(entity, request);
        return toResponse(introductionRepository.save(entity));
    }

    @Override
    @Transactional
    public IntroductionResponse updateIntroduction(Long userId, Long introductionId, IntroductionRequest request) {
        var entity = getOwnedIntroduction(userId, introductionId);
        applyRequest(entity, request);
        return toResponse(introductionRepository.save(entity));
    }

    @Override
    @Transactional
    public void deleteIntroduction(Long userId, Long introductionId) {
        var entity = getOwnedIntroduction(userId, introductionId);
        introductionRepository.delete(entity);
    }

    private ResumeIntroductionEntity getOwnedIntroduction(Long userId, Long introductionId) {
        var entity = introductionRepository.findById(introductionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        if (!entity.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        return entity;
    }

    private void applyRequest(ResumeIntroductionEntity entity, IntroductionRequest request) {
        entity.setTitle(request.title());
        entity.setContent(request.content());
        entity.setDisplayOrder(request.displayOrder() != null ? request.displayOrder() : 0);
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
