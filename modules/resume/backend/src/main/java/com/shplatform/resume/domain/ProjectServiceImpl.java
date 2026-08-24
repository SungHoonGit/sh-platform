package com.shplatform.resume.domain;

import com.shplatform.common.exception.BusinessException;
import com.shplatform.common.exception.ErrorCode;
import com.shplatform.resume.api.dto.ProjectRequest;
import com.shplatform.resume.api.dto.ProjectResponse;
import com.shplatform.resume.infrastructure.entity.ResumeProjectEntity;
import com.shplatform.resume.infrastructure.repository.ResumeProjectRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProjectServiceImpl implements ProjectService {

    private final ResumeProjectRepository projectRepository;

    @Override
    public List<ProjectResponse> getProjects(Long userId) {
        return projectRepository.findByUserIdOrderByDisplayOrderAscIdAsc(userId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public ProjectResponse createProject(Long userId, ProjectRequest request) {
        var entity = ResumeProjectEntity.create(userId);
        applyRequest(entity, request);
        return toResponse(projectRepository.save(entity));
    }

    @Override
    @Transactional
    public ProjectResponse updateProject(Long userId, Long projectId, ProjectRequest request) {
        var entity = getOwnedProject(userId, projectId);
        applyRequest(entity, request);
        return toResponse(projectRepository.save(entity));
    }

    @Override
    @Transactional
    public void deleteProject(Long userId, Long projectId) {
        var entity = getOwnedProject(userId, projectId);
        projectRepository.delete(entity);
    }

    private ResumeProjectEntity getOwnedProject(Long userId, Long projectId) {
        var entity = projectRepository.findById(projectId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        if (!entity.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        return entity;
    }

    private void applyRequest(ResumeProjectEntity entity, ProjectRequest request) {
        entity.setName(request.name());
        entity.setRole(request.role());
        entity.setStartDate(request.startDate());
        entity.setEndDate(request.endDate());
        entity.setDescription(request.description());
        entity.setTechStack(request.techStack());
        entity.setLinkUrl(request.linkUrl());
        entity.setDisplayOrder(request.displayOrder() != null ? request.displayOrder() : 0);
    }

    private ProjectResponse toResponse(ResumeProjectEntity entity) {
        return new ProjectResponse(
                entity.getId(),
                entity.getName(),
                entity.getRole(),
                entity.getStartDate(),
                entity.getEndDate(),
                entity.getDescription(),
                entity.getTechStack(),
                entity.getLinkUrl(),
                entity.getDisplayOrder(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
