package com.shplatform.resume.infrastructure.repository;

import com.shplatform.resume.infrastructure.entity.ResumeEducationEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ResumeEducationRepository extends JpaRepository<ResumeEducationEntity, Long> {

    List<ResumeEducationEntity> findByUserIdOrderByDisplayOrderAscIdAsc(Long userId);
}
