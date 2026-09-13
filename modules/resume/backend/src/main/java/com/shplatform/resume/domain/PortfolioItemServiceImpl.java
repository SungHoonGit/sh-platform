package com.shplatform.resume.domain;

import com.shplatform.common.exception.BusinessException;
import com.shplatform.common.exception.ErrorCode;
import com.shplatform.resume.api.dto.PortfolioItemRequest;
import com.shplatform.resume.api.dto.PortfolioItemResponse;
import com.shplatform.resume.infrastructure.entity.ResumePortfolioItemEntity;
import com.shplatform.resume.infrastructure.repository.ResumePortfolioItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PortfolioItemServiceImpl implements PortfolioItemService {

    private final ResumePortfolioItemRepository portfolioItemRepository;

    @Override
    public List<PortfolioItemResponse> getPortfolioItems(Long userId, Long documentId) {
        return portfolioItemRepository.findByUserIdAndDocumentIdOrderByDisplayOrderAscIdAsc(userId, documentId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public PortfolioItemResponse createPortfolioItem(Long userId, Long documentId, PortfolioItemRequest request) {
        validateTypePayload(request);
        var entity = ResumePortfolioItemEntity.create(userId, documentId);
        applyRequest(entity, request);
        return toResponse(portfolioItemRepository.save(entity));
    }

    @Override
    @Transactional
    public PortfolioItemResponse updatePortfolioItem(Long userId, Long documentId, Long itemId, PortfolioItemRequest request) {
        validateTypePayload(request);
        var entity = getOwnedPortfolioItem(userId, documentId, itemId);
        applyRequest(entity, request);
        return toResponse(portfolioItemRepository.save(entity));
    }

    @Override
    @Transactional
    public void deletePortfolioItem(Long userId, Long documentId, Long itemId) {
        var entity = getOwnedPortfolioItem(userId, documentId, itemId);
        portfolioItemRepository.delete(entity);
    }

    @Override
    @Transactional
    public void reorderPortfolioItems(Long userId, Long documentId, List<Long> orderedIds) {
        if (orderedIds == null || orderedIds.isEmpty()) {
            return;
        }
        var owned = portfolioItemRepository.findByUserIdAndDocumentIdOrderByDisplayOrderAscIdAsc(userId, documentId);
        Map<Long, ResumePortfolioItemEntity> byId = owned.stream()
                .collect(Collectors.toMap(ResumePortfolioItemEntity::getId, e -> e));
        int order = 1;
        for (Long id : orderedIds) {
            var entity = byId.get(id);
            if (entity == null) {
                throw new BusinessException(ErrorCode.FORBIDDEN);
            }
            entity.setDisplayOrder(order++);
        }
        portfolioItemRepository.saveAll(owned);
    }

    private void validateTypePayload(PortfolioItemRequest request) {
        if ("FILE".equals(request.itemType())
                && (request.filePath() == null || request.filePath().isBlank())) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }
    }

    /** itemType이 비어 있으면 첨부 여부로 유형을 결정한다 (프론트가 더 이상 itemType을 보내지 않음). */
    private String resolveItemType(PortfolioItemRequest request) {
        String type = request.itemType();
        if (type != null && !type.isBlank()) {
            return type;
        }
        return request.filePath() != null && !request.filePath().isBlank() ? "FILE" : "LINK";
    }

    private ResumePortfolioItemEntity getOwnedPortfolioItem(Long userId, Long documentId, Long itemId) {
        var entity = portfolioItemRepository.findById(itemId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        if (!entity.getUserId().equals(userId) || !entity.getDocumentId().equals(documentId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        return entity;
    }

    private void applyRequest(ResumePortfolioItemEntity entity, PortfolioItemRequest request) {
        entity.setTitle(request.title());
        entity.setItemType(resolveItemType(request));
        entity.setRole(request.role());
        entity.setStartDate(request.startDate());
        entity.setEndDate(request.endDate());
        entity.setTechStack(request.techStack());
        entity.setThumbnailPath(request.thumbnailPath());
        entity.setGithubUrl(request.githubUrl());
        entity.setDemoUrl(request.demoUrl());
        entity.setVideoUrl(request.videoUrl());
        entity.setFilePath(request.filePath());
        entity.setLinkUrl(request.linkUrl());
        entity.setDescription(request.description());
        entity.setDisplayOrder(request.displayOrder() != null ? request.displayOrder() : 0);
    }

    private PortfolioItemResponse toResponse(ResumePortfolioItemEntity entity) {
        return new PortfolioItemResponse(
                entity.getId(),
                entity.getTitle(),
                entity.getItemType(),
                entity.getRole(),
                entity.getStartDate(),
                entity.getEndDate(),
                entity.getTechStack(),
                entity.getThumbnailPath(),
                entity.getGithubUrl(),
                entity.getDemoUrl(),
                entity.getVideoUrl(),
                entity.getFilePath(),
                entity.getLinkUrl(),
                entity.getDescription(),
                entity.getDisplayOrder(),
                entity.getCreatedAt()
        );
    }
}
