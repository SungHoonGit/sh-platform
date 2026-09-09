package com.shplatform.resume.infrastructure.repository;

import com.shplatform.resume.infrastructure.entity.ResumeCareerItemEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface ResumeCareerItemRepository extends JpaRepository<ResumeCareerItemEntity, Long> {

    List<ResumeCareerItemEntity> findByCareerIdOrderByDisplayOrderAscIdAsc(Long careerId);

    List<ResumeCareerItemEntity> findByCareerIdInOrderByDisplayOrderAscIdAsc(Collection<Long> careerIds);
}