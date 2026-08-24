package com.shplatform.resume.domain;

import com.shplatform.common.exception.BusinessException;
import com.shplatform.common.exception.ErrorCode;
import com.shplatform.resume.api.dto.ProfileRequest;
import com.shplatform.resume.api.dto.ProfileResponse;
import com.shplatform.resume.infrastructure.entity.ResumeProfileEntity;
import com.shplatform.resume.infrastructure.repository.ResumeProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ResumeProfileServiceImpl implements ResumeProfileService {

    private final ResumeProfileRepository profileRepository;

    @Override
    public ProfileResponse getMyProfile(Long userId) {
        var entity = profileRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        return toResponse(entity);
    }

    @Override
    @Transactional
    public ProfileResponse upsertProfile(Long userId, ProfileRequest request) {
        var entity = profileRepository.findByUserId(userId)
                .orElseGet(() -> ResumeProfileEntity.create(userId));
        applyRequest(entity, request);
        return toResponse(profileRepository.save(entity));
    }

    @Override
    @Transactional
    public void updatePhotoUrl(Long userId, String photoUrl) {
        var entity = profileRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        entity.setPhotoUrl(photoUrl);
    }

    private void applyRequest(ResumeProfileEntity entity, ProfileRequest request) {
        entity.setName(request.name());
        entity.setEmail(request.email());
        entity.setPhone(request.phone());
        entity.setAddress(request.address());
        entity.setBirthDate(request.birthDate());
        entity.setPhotoUrl(request.photoUrl());
        entity.setHeadline(request.headline());
    }

    private ProfileResponse toResponse(ResumeProfileEntity entity) {
        return new ProfileResponse(
                entity.getId(),
                entity.getName(),
                entity.getEmail(),
                entity.getPhone(),
                entity.getAddress(),
                entity.getBirthDate(),
                entity.getPhotoUrl(),
                entity.getHeadline(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
