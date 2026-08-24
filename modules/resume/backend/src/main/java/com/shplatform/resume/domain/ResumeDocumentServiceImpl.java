package com.shplatform.resume.domain;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shplatform.common.exception.BusinessException;
import com.shplatform.common.exception.ErrorCode;
import com.shplatform.resume.api.dto.DocumentCreateRequest;
import com.shplatform.resume.api.dto.DocumentResponse;
import com.shplatform.resume.api.dto.DocumentUpdateRequest;
import com.shplatform.resume.infrastructure.entity.ResumeDocumentEntity;
import com.shplatform.resume.infrastructure.repository.ResumeDocumentRepository;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ResumeDocumentServiceImpl implements ResumeDocumentService {

    /** 섹션 key는 뷰 응답(ResumeView) 필드명과 일치해야 한다 */
    private static final Set<String> ALLOWED_SECTION_KEYS = Set.of(
            "careers", "projects", "educations", "skills",
            "certificates", "introductions", "portfolioItems"
    );

    static final String DEFAULT_SECTION_CONFIG = """
            [
              {"key":"careers","included":true,"order":1},
              {"key":"projects","included":true,"order":2},
              {"key":"educations","included":true,"order":3},
              {"key":"skills","included":true,"order":4},
              {"key":"certificates","included":true,"order":5},
              {"key":"introductions","included":true,"order":6},
              {"key":"portfolioItems","included":true,"order":7}
            ]""";

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final ResumeDocumentRepository documentRepository;

    @Override
    @Transactional
    public List<DocumentResponse> getDocuments(Long userId) {
        List<ResumeDocumentEntity> documents = documentRepository.findByUserIdOrderByCreatedAtAsc(userId);
        if (documents.isEmpty()) {
            return List.of(toResponse(documentRepository.save(defaultDocument(userId))));
        }
        return documents.stream().map(this::toResponse).toList();
    }

    @Override
    @Transactional
    public DocumentResponse createDocument(Long userId, DocumentCreateRequest request) {
        String sectionConfig = DEFAULT_SECTION_CONFIG;
        if (request.fromDocumentId() != null) {
            sectionConfig = getOwnedDocument(userId, request.fromDocumentId()).getSectionConfig();
        }
        var entity = ResumeDocumentEntity.create(userId, request.title(), "CLASSIC", false, sectionConfig);
        return toResponse(documentRepository.save(entity));
    }

    @Override
    @Transactional
    public DocumentResponse updateDocument(Long userId, Long documentId, DocumentUpdateRequest request) {
        var entity = getOwnedDocument(userId, documentId);
        if (request.title() != null && !request.title().isBlank()) {
            entity.updateTitle(request.title());
        }
        if (request.templateCode() != null && !request.templateCode().isBlank()) {
            entity.updateTemplateCode(request.templateCode());
        }
        if (request.sectionConfig() != null && !request.sectionConfig().isBlank()) {
            validateSectionConfig(request.sectionConfig());
            entity.updateSectionConfig(request.sectionConfig());
        }
        if (request.primary() != null && request.primary()) {
            markPrimaryInternal(userId, entity);
        }
        return toResponse(entity);
    }

    @Override
    @Transactional
    public void markPrimary(Long userId, Long documentId) {
        var entity = getOwnedDocument(userId, documentId);
        markPrimaryInternal(userId, entity);
    }

    @Override
    @Transactional
    public void deleteDocument(Long userId, Long documentId) {
        var entity = getOwnedDocument(userId, documentId);
        if (documentRepository.countByUserId(userId) <= 1) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }
        documentRepository.delete(entity);
    }

    private void markPrimaryInternal(Long userId, ResumeDocumentEntity target) {
        documentRepository.findByUserIdOrderByCreatedAtAsc(userId)
                .forEach(doc -> doc.unmarkPrimary());
        target.markPrimary();
    }

    private ResumeDocumentEntity defaultDocument(Long userId) {
        return ResumeDocumentEntity.create(userId, "내 이력서", "CLASSIC", true,
                DEFAULT_SECTION_CONFIG);
    }

    private ResumeDocumentEntity getOwnedDocument(Long userId, Long documentId) {
        var entity = documentRepository.findByIdAndUserId(documentId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        return entity;
    }

    private void validateSectionConfig(String sectionConfig) {
        try {
            JsonNode root = OBJECT_MAPPER.readTree(sectionConfig);
            if (!root.isArray() || root.isEmpty()) {
                throw new BusinessException(ErrorCode.INVALID_INPUT);
            }
            for (JsonNode item : root) {
                JsonNode key = item.get("key");
                if (key == null || key.asText().isBlank()
                        || !ALLOWED_SECTION_KEYS.contains(key.asText())) {
                    throw new BusinessException(ErrorCode.INVALID_INPUT);
                }
            }
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }
    }

    private DocumentResponse toResponse(ResumeDocumentEntity entity) {
        return new DocumentResponse(
                entity.getId(),
                entity.getTitle(),
                entity.getTemplateCode(),
                entity.isPrimary(),
                entity.getSectionConfig(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
