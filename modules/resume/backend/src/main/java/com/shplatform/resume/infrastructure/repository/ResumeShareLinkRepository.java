package com.shplatform.resume.infrastructure.repository;

import com.shplatform.resume.infrastructure.entity.ResumeShareLinkEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ResumeShareLinkRepository extends JpaRepository<ResumeShareLinkEntity, Long> {

    Optional<ResumeShareLinkEntity> findByToken(String token);

    Optional<ResumeShareLinkEntity> findByDocumentId(Long documentId);

    void deleteByDocumentId(Long documentId);
}