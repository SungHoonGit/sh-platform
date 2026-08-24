package com.shplatform.resume.infrastructure.repository;

import com.shplatform.resume.infrastructure.entity.ResumeDocumentEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ResumeDocumentRepository extends JpaRepository<ResumeDocumentEntity, Long> {

    List<ResumeDocumentEntity> findByUserIdOrderByCreatedAtAsc(Long userId);

    Optional<ResumeDocumentEntity> findByIdAndUserId(Long id, Long userId);

    long countByUserId(Long userId);
}
