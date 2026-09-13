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
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SkillServiceImpl implements SkillService {

    private final ResumeSkillRepository skillRepository;

    @Override
    public List<SkillResponse> getSkills(Long userId, Long documentId) {
        return skillRepository.findByUserIdAndDocumentIdOrderByDisplayOrderAscIdAsc(userId, documentId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public SkillResponse createSkill(Long userId, Long documentId, SkillRequest request) {
        var entity = ResumeSkillEntity.create(userId, documentId);
        applyRequest(entity, request);
        return toResponse(skillRepository.save(entity));
    }

    @Override
    @Transactional
    public SkillResponse updateSkill(Long userId, Long documentId, Long skillId, SkillRequest request) {
        var entity = getOwnedSkill(userId, documentId, skillId);
        applyRequest(entity, request);
        return toResponse(skillRepository.save(entity));
    }

    @Override
    @Transactional
    public void deleteSkill(Long userId, Long documentId, Long skillId) {
        var entity = getOwnedSkill(userId, documentId, skillId);
        skillRepository.delete(entity);
    }

    @Override
    @Transactional
    public void reorderSkills(Long userId, Long documentId, List<Long> orderedIds) {
        if (orderedIds == null || orderedIds.isEmpty()) {
            return;
        }
        var owned = skillRepository.findByUserIdAndDocumentIdOrderByDisplayOrderAscIdAsc(userId, documentId);
        Map<Long, ResumeSkillEntity> byId = owned.stream()
                .collect(Collectors.toMap(ResumeSkillEntity::getId, e -> e));
        int order = 1;
        for (Long id : orderedIds) {
            var entity = byId.get(id);
            if (entity == null) {
                throw new BusinessException(ErrorCode.FORBIDDEN);
            }
            entity.setDisplayOrder(order++);
        }
        skillRepository.saveAll(owned);
    }

    private ResumeSkillEntity getOwnedSkill(Long userId, Long documentId, Long skillId) {
        var entity = skillRepository.findById(skillId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        if (!entity.getUserId().equals(userId) || !entity.getDocumentId().equals(documentId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        return entity;
    }

    private void applyRequest(ResumeSkillEntity entity, SkillRequest request) {
        entity.setName(request.name());
        entity.setLevel(request.level());
        entity.setCategory(request.category());
        if (request.displayOrder() != null) entity.setDisplayOrder(request.displayOrder());
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
