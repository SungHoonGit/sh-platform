package com.shplatform.resume.infrastructure.repository;

import com.shplatform.resume.infrastructure.entity.ResumeCareerEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ResumeCareerRepository extends JpaRepository<ResumeCareerEntity, Long> {

    List<ResumeCareerEntity> findByUserIdOrderByDisplayOrderAscIdAsc(Long userId);
}
