package com.shplatform.resume.domain;

import com.shplatform.common.exception.BusinessException;
import com.shplatform.common.exception.ErrorCode;
import com.shplatform.resume.api.dto.SkillRequest;
import com.shplatform.resume.api.dto.SkillResponse;
import com.shplatform.resume.infrastructure.entity.ResumeSkillEntity;
import com.shplatform.resume.infrastructure.repository.ResumeSkillRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SkillServiceImpl implements SkillService {

    private final ResumeSkillRepository skillRepository;

    @Override
    public List<SkillResponse> getSkills(Long userId) {
        return skillRepository.findByUserIdOrderByDisplayOrderAscIdAsc(userId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public SkillResponse createSkill(Long userId, SkillRequest request) {
        var entity = ResumeSkillEntity.create(userId);
        applyRequest(entity, request);
        return toResponse(skillRepository.save(entity));
    }

    @Override
    @Transactional
    public SkillResponse updateSkill(Long userId, Long skillId, SkillRequest request) {
        var entity = getOwnedSkill(userId, skillId);
        applyRequest(entity, request);
        return toResponse(skillRepository.save(entity));
    }

    @Override
    @Transactional
    public void deleteSkill(Long userId, Long skillId) {
        var entity = getOwnedSkill(userId, skillId);
        skillRepository.delete(entity);
    }

    private ResumeSkillEntity getOwnedSkill(Long userId, Long skillId) {
        var entity = skillRepository.findById(skillId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        if (!entity.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        return entity;
    }

    private void applyRequest(ResumeSkillEntity entity, SkillRequest request) {
        entity.setName(request.name());
        entity.setLevel(request.level());
        entity.setCategory(request.category());
        entity.setDisplayOrder(request.displayOrder() != null ? request.displayOrder() : 0);
    }

    private SkillResponse toResponse(ResumeSkillEntity entity) {
        return new SkillResponse(
                entity.getId(),
                entity.getName(),
                entity.getLevel(),
                entity.getCategory(),
                entity.getDisplayOrder(),
                entity.getCreatedAt()
        );
    }
}
