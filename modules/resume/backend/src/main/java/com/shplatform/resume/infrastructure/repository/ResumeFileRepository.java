package com.shplatform.resume.infrastructure.repository;

import com.shplatform.resume.infrastructure.entity.ResumeFileEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ResumeFileRepository extends JpaRepository<ResumeFileEntity, Long> {
}
