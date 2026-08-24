package com.shplatform.resume.infrastructure.repository;

import com.shplatform.resume.infrastructure.entity.ResumeProjectEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ResumeProjectRepository extends JpaRepository<ResumeProjectEntity, Long> {

    List<ResumeProjectEntity> findByUserIdOrderByDisplayOrderAscIdAsc(Long userId);
}
