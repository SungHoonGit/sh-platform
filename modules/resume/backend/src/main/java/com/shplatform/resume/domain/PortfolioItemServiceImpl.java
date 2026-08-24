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

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PortfolioItemServiceImpl implements PortfolioItemService {

    private final ResumePortfolioItemRepository portfolioItemRepository;

    @Override
    public List<PortfolioItemResponse> getPortfolioItems(Long userId) {
        return portfolioItemRepository.findByUserIdOrderByDisplayOrderAscIdAsc(userId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public PortfolioItemResponse createPortfolioItem(Long userId, PortfolioItemRequest request) {
        validateLinkOnly(request);
        var entity = ResumePortfolioItemEntity.create(userId);
        applyRequest(entity, request);
        return toResponse(portfolioItemRepository.save(entity));
    }

    @Override
    @Transactional
    public PortfolioItemResponse updatePortfolioItem(Long userId, Long itemId, PortfolioItemRequest request) {
        validateLinkOnly(request);
        var entity = getOwnedPortfolioItem(userId, itemId);
        applyRequest(entity, request);
        return toResponse(portfolioItemRepository.save(entity));
    }

    @Override
    @Transactional
    public void deletePortfolioItem(Long userId, Long itemId) {
        var entity = getOwnedPortfolioItem(userId, itemId);
        portfolioItemRepository.delete(entity);
    }

    private void validateLinkOnly(PortfolioItemRequest request) {
        if ("FILE".equals(request.itemType())) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }
    }

    private ResumePortfolioItemEntity getOwnedPortfolioItem(Long userId, Long itemId) {
        var entity = portfolioItemRepository.findById(itemId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        if (!entity.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        return entity;
    }

    private void applyRequest(ResumePortfolioItemEntity entity, PortfolioItemRequest request) {
        entity.setTitle(request.title());
        entity.setItemType(request.itemType());
        entity.setFilePath(null);
        entity.setLinkUrl(request.linkUrl());
        entity.setDescription(request.description());
        entity.setDisplayOrder(request.displayOrder() != null ? request.displayOrder() : 0);
    }

    private PortfolioItemResponse toResponse(ResumePortfolioItemEntity entity) {
        return new PortfolioItemResponse(
                entity.getId(),
                entity.getTitle(),
                entity.getItemType(),
                entity.getFilePath(),
                entity.getLinkUrl(),
                entity.getDescription(),
                entity.getDisplayOrder(),
                entity.getCreatedAt()
        );
    }
}
