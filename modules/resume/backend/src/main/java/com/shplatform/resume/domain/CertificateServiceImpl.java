package com.shplatform.resume.domain;

import com.shplatform.common.exception.BusinessException;
import com.shplatform.common.exception.ErrorCode;
import com.shplatform.resume.api.dto.CertificateRequest;
import com.shplatform.resume.api.dto.CertificateResponse;
import com.shplatform.resume.infrastructure.entity.ResumeCertificateEntity;
import com.shplatform.resume.infrastructure.repository.ResumeCertificateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CertificateServiceImpl implements CertificateService {

    private final ResumeCertificateRepository certificateRepository;

    @Override
    public List<CertificateResponse> getCertificates(Long userId, Long documentId) {
        return certificateRepository.findByUserIdAndDocumentIdOrderByDisplayOrderAscIdAsc(userId, documentId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public CertificateResponse createCertificate(Long userId, Long documentId, CertificateRequest request) {
        var entity = ResumeCertificateEntity.create(userId, documentId);
        applyRequest(entity, request);
        return toResponse(certificateRepository.save(entity));
    }

    @Override
    @Transactional
    public CertificateResponse updateCertificate(Long userId, Long documentId, Long certificateId, CertificateRequest request) {
        var entity = getOwnedCertificate(userId, documentId, certificateId);
        applyRequest(entity, request);
        return toResponse(certificateRepository.save(entity));
    }

    @Override
    @Transactional
    public void deleteCertificate(Long userId, Long documentId, Long certificateId) {
        var entity = getOwnedCertificate(userId, documentId, certificateId);
        certificateRepository.delete(entity);
    }

    @Override
    @Transactional
    public void reorderCertificates(Long userId, Long documentId, List<Long> orderedIds) {
        if (orderedIds == null || orderedIds.isEmpty()) {
            return;
        }
        var owned = certificateRepository.findByUserIdAndDocumentIdOrderByDisplayOrderAscIdAsc(userId, documentId);
        Map<Long, ResumeCertificateEntity> byId = owned.stream()
                .collect(Collectors.toMap(ResumeCertificateEntity::getId, e -> e));
        int order = 1;
        for (Long id : orderedIds) {
            var entity = byId.get(id);
            if (entity == null) {
                throw new BusinessException(ErrorCode.FORBIDDEN);
            }
            entity.setDisplayOrder(order++);
        }
        certificateRepository.saveAll(owned);
    }

    private ResumeCertificateEntity getOwnedCertificate(Long userId, Long documentId, Long certificateId) {
        var entity = certificateRepository.findById(certificateId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        if (!entity.getUserId().equals(userId) || !entity.getDocumentId().equals(documentId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        return entity;
    }

    private void applyRequest(ResumeCertificateEntity entity, CertificateRequest request) {
        entity.setName(request.name());
        entity.setIssuer(request.issuer());
        entity.setAcquiredAt(request.acquiredAt());
        entity.setDisplayOrder(request.displayOrder() != null ? request.displayOrder() : 0);
    }

    private CertificateResponse toResponse(ResumeCertificateEntity entity) {
        return new CertificateResponse(
                entity.getId(),
                entity.getName(),
                entity.getIssuer(),
                entity.getAcquiredAt(),
                entity.getDisplayOrder(),
                entity.getCreatedAt()
        );
    }
}
