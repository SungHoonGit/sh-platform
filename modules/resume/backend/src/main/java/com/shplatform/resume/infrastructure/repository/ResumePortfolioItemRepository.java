package com.shplatform.resume.infrastructure.repository;

import com.shplatform.resume.infrastructure.entity.ResumePortfolioItemEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ResumePortfolioItemRepository extends JpaRepository<ResumePortfolioItemEntity, Long> {

    List<ResumePortfolioItemEntity> findByUserIdOrderByDisplayOrderAscIdAsc(Long userId);
}
