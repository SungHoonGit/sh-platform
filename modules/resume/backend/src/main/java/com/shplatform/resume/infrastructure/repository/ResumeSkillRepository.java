package com.shplatform.resume.infrastructure.repository;

import com.shplatform.resume.infrastructure.entity.ResumeSkillEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ResumeSkillRepository extends JpaRepository<ResumeSkillEntity, Long> {

    List<ResumeSkillEntity> findByUserIdOrderByDisplayOrderAscIdAsc(Long userId);
}
