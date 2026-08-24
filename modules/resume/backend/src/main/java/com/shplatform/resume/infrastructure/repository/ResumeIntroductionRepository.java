package com.shplatform.resume.infrastructure.repository;

import com.shplatform.resume.infrastructure.entity.ResumeIntroductionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ResumeIntroductionRepository extends JpaRepository<ResumeIntroductionEntity, Long> {

    List<ResumeIntroductionEntity> findByUserIdOrderByDisplayOrderAscIdAsc(Long userId);
}
