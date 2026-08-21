package com.shplatform.resume.domain;

import com.shplatform.common.exception.BusinessException;
import com.shplatform.common.exception.ErrorCode;
import com.shplatform.resume.api.dto.CareerRequest;
import com.shplatform.resume.api.dto.CareerResponse;
import com.shplatform.resume.infrastructure.entity.ResumeCareerEntity;
import com.shplatform.resume.infrastructure.repository.ResumeCareerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CareerServiceImpl implements CareerService {

    private final ResumeCareerRepository careerRepository;

    @Override
    public List<CareerResponse> getCareers(Long userId) {
        return careerRepository.findByUserIdOrderByDisplayOrderAscIdAsc(userId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public CareerResponse createCareer(Long userId, CareerRequest request) {
        var entity = ResumeCareerEntity.create(userId);
        entity.setUserId(userId);
        applyRequest(entity, request);
        return toResponse(careerRepository.save(entity));
    }

    @Override
    @Transactional
    public CareerResponse updateCareer(Long userId, Long careerId, CareerRequest request) {
        var entity = getOwnedCareer(userId, careerId);
        applyRequest(entity, request);
        return toResponse(careerRepository.save(entity));
    }

    @Override
    @Transactional
    public void deleteCareer(Long userId, Long careerId) {
        var entity = getOwnedCareer(userId, careerId);
        careerRepository.delete(entity);
    }

    private ResumeCareerEntity getOwnedCareer(Long userId, Long careerId) {
        var entity = careerRepository.findById(careerId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        if (!entity.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        return entity;
    }

    private void applyRequest(ResumeCareerEntity entity, CareerRequest request) {
        entity.setCompany(request.company());
        entity.setTitle(request.title());
        entity.setStartDate(request.startDate());
        entity.setEndDate(request.endDate());
        entity.setDescription(request.description());
        entity.setDisplayOrder(request.displayOrder() != null ? request.displayOrder() : 0);
    }

    private CareerResponse toResponse(ResumeCareerEntity entity) {
        return new CareerResponse(
                entity.getId(),
                entity.getCompany(),
                entity.getTitle(),
                entity.getStartDate(),
                entity.getEndDate(),
                entity.getDescription(),
                entity.getDisplayOrder(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
