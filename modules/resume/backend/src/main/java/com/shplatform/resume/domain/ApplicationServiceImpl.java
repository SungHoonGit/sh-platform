package com.shplatform.resume.domain;

import com.shplatform.common.exception.BusinessException;
import com.shplatform.common.exception.ErrorCode;
import com.shplatform.resume.api.dto.ApplicationRequest;
import com.shplatform.resume.api.dto.ApplicationResponse;
import com.shplatform.resume.infrastructure.ApplicationEntity;
import com.shplatform.resume.infrastructure.ApplicationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ApplicationServiceImpl implements ApplicationService {

    private final ApplicationRepository applicationRepository;

    @Override
    @Transactional(readOnly = true)
    public List<ApplicationResponse> getApplications(Long userId, String status) {
        List<ApplicationEntity> entities = (status == null || status.isBlank())
                ? applicationRepository.findByUserIdOrderByAppliedAtDescIdDesc(userId)
                : applicationRepository.findByUserIdAndStatusOrderByAppliedAtDescIdDesc(userId, status);
        return entities.stream().map(this::toResponse).toList();
    }

    @Override
    @Transactional
    public ApplicationResponse create(Long userId, ApplicationRequest request) {
        ApplicationEntity saved = applicationRepository.save(toEntity(userId, request));
        return toResponse(saved);
    }

    @Override
    @Transactional
    public ApplicationResponse update(Long userId, Long id, ApplicationRequest request) {
        ApplicationEntity entity = getOwnedEntity(userId, id);
        applyRequest(entity, request);
        return toResponse(entity);
    }

    @Override
    @Transactional
    public void delete(Long userId, Long id) {
        ApplicationEntity entity = getOwnedEntity(userId, id);
        applicationRepository.delete(entity);
    }

    private ApplicationEntity getOwnedEntity(Long userId, Long id) {
        ApplicationEntity entity = applicationRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        if (!entity.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        return entity;
    }

    private ApplicationEntity toEntity(Long userId, ApplicationRequest request) {
        ApplicationEntity entity = ApplicationEntity.builder()
                .userId(userId)
                .build();
        applyRequest(entity, request);
        return entity;
    }

    private void applyRequest(ApplicationEntity entity, ApplicationRequest request) {
        entity.setCompanyName(request.companyName());
        entity.setPostingTitle(request.postingTitle());
        entity.setPostingUrl(request.postingUrl());
        entity.setApplyChannel(ApplyChannel.fromCode(request.applyChannel()).name());
        entity.setAppliedAt(request.appliedAt());
        entity.setStatus(ApplicationStatus.fromCode(request.status()).name());
        entity.setDocumentId(request.documentId());
        entity.setPostingId(request.postingId());
        entity.setMemo(request.memo());
    }

    private ApplicationResponse toResponse(ApplicationEntity entity) {
        return new ApplicationResponse(
                entity.getId(),
                entity.getPostingId(),
                entity.getCompanyName(),
                entity.getPostingTitle(),
                entity.getPostingUrl(),
                entity.getApplyChannel(),
                entity.getAppliedAt(),
                entity.getStatus(),
                entity.getDocumentId(),
                entity.getMemo(),
                entity.getCreatedAt()
        );
    }
}
