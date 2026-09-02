package com.shplatform.resume.domain;

import com.shplatform.common.exception.BusinessException;
import com.shplatform.common.exception.ErrorCode;
import com.shplatform.resume.api.dto.ShareLinkResponse;
import com.shplatform.resume.api.dto.ShareViewResponse;
import com.shplatform.resume.infrastructure.entity.ResumeShareLinkEntity;
import com.shplatform.resume.infrastructure.repository.ResumeDocumentRepository;
import com.shplatform.resume.infrastructure.repository.ResumeShareLinkRepository;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ResumeShareServiceImpl implements ResumeShareService {

    private final ResumeDocumentRepository documentRepository;
    private final ResumeShareLinkRepository shareLinkRepository;
    private final ResumeViewService resumeViewService;
    private final ResumeDocumentService resumeDocumentService;

    @Override
    @Transactional
    public ShareLinkResponse createShareLink(Long userId, Long documentId, LocalDateTime expiresAt) {
        assertOwnedDocument(userId, documentId);
        shareLinkRepository.deleteByDocumentId(documentId);
        ResumeShareLinkEntity entity = ResumeShareLinkEntity.create(
                documentId, newToken(), expiresAt);
        ResumeShareLinkEntity saved = shareLinkRepository.save(entity);
        return toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ShareLinkResponse> getShareLink(Long userId, Long documentId) {
        assertOwnedDocument(userId, documentId);
        return shareLinkRepository.findByDocumentId(documentId).map(this::toResponse);
    }

    @Override
    @Transactional
    public void revokeShareLink(Long userId, Long documentId) {
        assertOwnedDocument(userId, documentId);
        shareLinkRepository.deleteByDocumentId(documentId);
    }

    @Override
    public Optional<ResolvedShare> resolve(String token) {
        if (token == null || token.isBlank()) {
            return Optional.empty();
        }
        return shareLinkRepository.findByToken(token)
                .filter(link -> link.getExpiresAt() == null || link.getExpiresAt().isAfter(LocalDateTime.now()))
                .map(link -> new ResolvedShare(ownerUserId(link), link.getDocumentId()));
    }

    @Override
    public ShareViewResponse getPublicView(String token) {
        ResolvedShare resolved = resolve(token)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        var doc = resumeDocumentService.getDocuments(resolved.userId()).stream()
                .filter(d -> d.id().equals(resolved.documentId()))
                .findFirst()
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        return new ShareViewResponse(
                doc.id(),
                doc.title(),
                doc.templateCode(),
                doc.sectionConfig(),
                resumeViewService.getMyResumeView(resolved.userId())
        );
    }

    private Long ownerUserId(ResumeShareLinkEntity link) {
        return documentRepository.findById(link.getDocumentId())
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND))
                .getUserId();
    }

    private void assertOwnedDocument(Long userId, Long documentId) {
        documentRepository.findByIdAndUserId(documentId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
    }

    private String newToken() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    private ShareLinkResponse toResponse(ResumeShareLinkEntity entity) {
        return new ShareLinkResponse(
                entity.getDocumentId(),
                entity.getToken(),
                entity.getExpiresAt(),
                entity.getCreatedAt()
        );
    }
}