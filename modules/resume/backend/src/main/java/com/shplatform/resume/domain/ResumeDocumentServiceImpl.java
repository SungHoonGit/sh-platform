package com.shplatform.resume.domain;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shplatform.common.exception.BusinessException;
import com.shplatform.common.exception.ErrorCode;
import com.shplatform.resume.api.dto.DocumentCreateRequest;
import com.shplatform.resume.api.dto.DocumentResponse;
import com.shplatform.resume.api.dto.DocumentUpdateRequest;
import com.shplatform.resume.infrastructure.entity.ResumeCareerEntity;
import com.shplatform.resume.infrastructure.entity.ResumeCareerItemEntity;
import com.shplatform.resume.infrastructure.entity.ResumeCertificateEntity;
import com.shplatform.resume.infrastructure.entity.ResumeDocumentEntity;
import com.shplatform.resume.infrastructure.entity.ResumeEducationEntity;
import com.shplatform.resume.infrastructure.entity.ResumeIntroductionEntity;
import com.shplatform.resume.infrastructure.entity.ResumePortfolioItemEntity;
import com.shplatform.resume.infrastructure.entity.ResumeProjectEntity;
import com.shplatform.resume.infrastructure.entity.ResumeSkillEntity;
import com.shplatform.resume.infrastructure.repository.ResumeCareerItemRepository;
import com.shplatform.resume.infrastructure.repository.ResumeCareerRepository;
import com.shplatform.resume.infrastructure.repository.ResumeCertificateRepository;
import com.shplatform.resume.infrastructure.repository.ResumeDocumentRepository;
import com.shplatform.resume.infrastructure.repository.ResumeEducationRepository;
import com.shplatform.resume.infrastructure.repository.ResumeIntroductionRepository;
import com.shplatform.resume.infrastructure.repository.ResumePortfolioItemRepository;
import com.shplatform.resume.infrastructure.repository.ResumeProjectRepository;
import com.shplatform.resume.infrastructure.repository.ResumeSkillRepository;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
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
    private final ResumeCareerRepository careerRepository;
    private final ResumeCareerItemRepository careerItemRepository;
    private final ResumeEducationRepository educationRepository;
    private final ResumeProjectRepository projectRepository;
    private final ResumeSkillRepository skillRepository;
    private final ResumeCertificateRepository certificateRepository;
    private final ResumeIntroductionRepository introductionRepository;
    private final ResumePortfolioItemRepository portfolioItemRepository;

    @Override
    @Transactional
    public List<DocumentResponse> getDocuments(Long userId) {
        List<ResumeDocumentEntity> documents =
                documentRepository.findByUserIdOrderByDisplayOrderAscIdAsc(userId);
        if (documents.isEmpty()) {
            var defaultDoc = defaultDocument(userId);
            defaultDoc.updateDisplayOrder(1);
            return List.of(toResponse(documentRepository.save(defaultDoc)));
        }
        return documents.stream().map(this::toResponse).toList();
    }

    @Override
    @Transactional
    public DocumentResponse createDocument(Long userId, DocumentCreateRequest request) {
        String sectionConfig = DEFAULT_SECTION_CONFIG;
        Long fromDocumentId = null;
        if (request.fromDocumentId() != null) {
            var sourceDoc = getOwnedDocument(userId, request.fromDocumentId());
            sectionConfig = sourceDoc.getSectionConfig();
            fromDocumentId = sourceDoc.getId();
        }
        var entity = ResumeDocumentEntity.create(userId, request.title(), "CLASSIC", false, sectionConfig);
        List<ResumeDocumentEntity> owned =
                documentRepository.findByUserIdOrderByDisplayOrderAscIdAsc(userId);
        int nextOrder = owned.stream()
                .mapToInt(ResumeDocumentEntity::getDisplayOrder)
                .max()
                .orElse(0) + 1;
        entity.updateDisplayOrder(nextOrder);
        ResumeDocumentEntity saved = documentRepository.save(entity);
        if (fromDocumentId != null) {
            cloneItems(userId, fromDocumentId, saved.getId());
        }
        return toResponse(saved);
    }

    private void cloneItems(Long userId, Long sourceDocumentId, Long targetDocumentId) {
        var careers = careerRepository.findByUserIdAndDocumentIdOrderByDisplayOrderAscIdAsc(userId, sourceDocumentId);
        for (var career : careers) {
            var newCareer = ResumeCareerEntity.create(userId, targetDocumentId);
            newCareer.setCompany(career.getCompany());
            newCareer.setTitle(career.getTitle());
            newCareer.setStartDate(career.getStartDate());
            newCareer.setEndDate(career.getEndDate());
            newCareer.setDescription(career.getDescription());
            newCareer.setDisplayOrder(career.getDisplayOrder());
            var savedCareer = careerRepository.save(newCareer);
            var items = careerItemRepository.findByCareerIdAndDocumentIdOrderByDisplayOrderAscIdAsc(career.getId(), sourceDocumentId);
            for (var item : items) {
                var newItem = ResumeCareerItemEntity.create(savedCareer.getId(), targetDocumentId);
                newItem.setTitle(item.getTitle());
                newItem.setStartDate(item.getStartDate());
                newItem.setEndDate(item.getEndDate());
                newItem.setDescription(item.getDescription());
                newItem.setDisplayOrder(item.getDisplayOrder());
                careerItemRepository.save(newItem);
            }
        }
        var educations = educationRepository.findByUserIdAndDocumentIdOrderByDisplayOrderAscIdAsc(userId, sourceDocumentId);
        for (var edu : educations) {
            var newEdu = ResumeEducationEntity.create(userId, targetDocumentId);
            newEdu.setSchool(edu.getSchool());
            newEdu.setSchoolType(edu.getSchoolType());
            newEdu.setMajor(edu.getMajor());
            newEdu.setDegree(edu.getDegree());
            newEdu.setGpa(edu.getGpa());
            newEdu.setStartDate(edu.getStartDate());
            newEdu.setEndDate(edu.getEndDate());
            newEdu.setStatus(edu.getStatus());
            newEdu.setDisplayOrder(edu.getDisplayOrder());
            educationRepository.save(newEdu);
        }
        var skills = skillRepository.findByUserIdAndDocumentIdOrderByDisplayOrderAscIdAsc(userId, sourceDocumentId);
        for (var skill : skills) {
            var newSkill = ResumeSkillEntity.create(userId, targetDocumentId);
            newSkill.setName(skill.getName());
            newSkill.setLevel(skill.getLevel());
            newSkill.setCategory(skill.getCategory());
            newSkill.setDisplayOrder(skill.getDisplayOrder());
            skillRepository.save(newSkill);
        }
        var certificates = certificateRepository.findByUserIdAndDocumentIdOrderByDisplayOrderAscIdAsc(userId, sourceDocumentId);
        for (var cert : certificates) {
            var newCert = ResumeCertificateEntity.create(userId, targetDocumentId);
            newCert.setName(cert.getName());
            newCert.setIssuer(cert.getIssuer());
            newCert.setAcquiredAt(cert.getAcquiredAt());
            newCert.setDisplayOrder(cert.getDisplayOrder());
            certificateRepository.save(newCert);
        }
        var projects = projectRepository.findByUserIdAndDocumentIdOrderByDisplayOrderAscIdAsc(userId, sourceDocumentId);
        for (var proj : projects) {
            var newProj = ResumeProjectEntity.create(userId, targetDocumentId);
            newProj.setName(proj.getName());
            newProj.setRole(proj.getRole());
            newProj.setStartDate(proj.getStartDate());
            newProj.setEndDate(proj.getEndDate());
            newProj.setDescription(proj.getDescription());
            newProj.setTechStack(proj.getTechStack());
            newProj.setGithubUrl(proj.getGithubUrl());
            newProj.setDemoUrl(proj.getDemoUrl());
            newProj.setVideoUrl(proj.getVideoUrl());
            newProj.setLinkUrl(proj.getLinkUrl());
            newProj.setThumbnailPath(proj.getThumbnailPath());
            newProj.setDisplayOrder(proj.getDisplayOrder());
            projectRepository.save(newProj);
        }
        var introductions = introductionRepository.findByUserIdAndDocumentIdOrderByDisplayOrderAscIdAsc(userId, sourceDocumentId);
        for (var intro : introductions) {
            var newIntro = ResumeIntroductionEntity.create(userId, targetDocumentId);
            newIntro.setTitle(intro.getTitle());
            newIntro.setContent(intro.getContent());
            newIntro.setDisplayOrder(intro.getDisplayOrder());
            introductionRepository.save(newIntro);
        }
        var portfolioItems = portfolioItemRepository.findByUserIdAndDocumentIdOrderByDisplayOrderAscIdAsc(userId, sourceDocumentId);
        for (var pi : portfolioItems) {
            var newPi = ResumePortfolioItemEntity.create(userId, targetDocumentId);
            newPi.setTitle(pi.getTitle());
            newPi.setItemType(pi.getItemType());
            newPi.setFilePath(pi.getFilePath());
            newPi.setLinkUrl(pi.getLinkUrl());
            newPi.setThumbnailPath(pi.getThumbnailPath());
            newPi.setGithubUrl(pi.getGithubUrl());
            newPi.setDemoUrl(pi.getDemoUrl());
            newPi.setVideoUrl(pi.getVideoUrl());
            newPi.setRole(pi.getRole());
            newPi.setStartDate(pi.getStartDate());
            newPi.setEndDate(pi.getEndDate());
            newPi.setTechStack(pi.getTechStack());
            newPi.setDescription(pi.getDescription());
            newPi.setDisplayOrder(pi.getDisplayOrder());
            portfolioItemRepository.save(newPi);
        }
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

    @Override
    @Transactional
    public void reorderDocuments(Long userId, List<Long> orderedIds) {
        if (orderedIds == null || orderedIds.isEmpty()) {
            return;
        }
        var owned = documentRepository.findByUserIdOrderByDisplayOrderAscIdAsc(userId);
        Map<Long, ResumeDocumentEntity> byId = owned.stream()
                .collect(Collectors.toMap(ResumeDocumentEntity::getId, e -> e));
        int order = 1;
        for (Long id : orderedIds) {
            var entity = byId.get(id);
            if (entity == null) {
                throw new BusinessException(ErrorCode.FORBIDDEN);
            }
            entity.updateDisplayOrder(order++);
        }
        documentRepository.saveAll(owned);
    }

    private void markPrimaryInternal(Long userId, ResumeDocumentEntity target) {
        documentRepository.findByUserIdOrderByDisplayOrderAscIdAsc(userId)
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
                JsonNode hidden = item.get("hiddenItemIds");
                if (hidden != null && !hidden.isArray()) {
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
                entity.getDisplayOrder(),
                entity.getSectionConfig(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
