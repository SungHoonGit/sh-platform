package com.shplatform.resume.domain;

import com.shplatform.common.exception.BusinessException;
import com.shplatform.common.exception.ErrorCode;
import com.shplatform.resume.api.dto.EducationRequest;
import com.shplatform.resume.api.dto.EducationResponse;
import com.shplatform.resume.infrastructure.entity.ResumeEducationEntity;
import com.shplatform.resume.infrastructure.repository.ResumeEducationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EducationServiceImpl implements EducationService {

    private final ResumeEducationRepository educationRepository;

    @Override
    public List<EducationResponse> getEducations(Long userId) {
        return educationRepository.findByUserIdOrderByDisplayOrderAscIdAsc(userId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public EducationResponse createEducation(Long userId, EducationRequest request) {
        var entity = ResumeEducationEntity.create(userId);
        applyRequest(entity, request);
        return toResponse(educationRepository.save(entity));
    }

    @Override
    @Transactional
    public EducationResponse updateEducation(Long userId, Long educationId, EducationRequest request) {
        var entity = getOwnedEducation(userId, educationId);
        applyRequest(entity, request);
        return toResponse(educationRepository.save(entity));
    }

    @Override
    @Transactional
    public void deleteEducation(Long userId, Long educationId) {
        var entity = getOwnedEducation(userId, educationId);
        educationRepository.delete(entity);
    }

    private ResumeEducationEntity getOwnedEducation(Long userId, Long educationId) {
        var entity = educationRepository.findById(educationId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        if (!entity.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        return entity;
    }

    private void applyRequest(ResumeEducationEntity entity, EducationRequest request) {
        entity.setSchool(request.school());
        entity.setSchoolType(request.schoolType());
        entity.setMajor(request.major());
        entity.setDegree(request.degree());
        entity.setStartDate(request.startDate());
        entity.setEndDate(request.endDate());
        entity.setStatus(request.status());
        entity.setDisplayOrder(request.displayOrder() != null ? request.displayOrder() : 0);
    }

    private EducationResponse toResponse(ResumeEducationEntity entity) {
        return new EducationResponse(
                entity.getId(),
                entity.getSchool(),
                entity.getSchoolType(),
                entity.getMajor(),
                entity.getDegree(),
                entity.getStartDate(),
                entity.getEndDate(),
                entity.getStatus(),
                entity.getDisplayOrder(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
