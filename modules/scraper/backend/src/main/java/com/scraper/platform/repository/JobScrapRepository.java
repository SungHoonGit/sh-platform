package com.scraper.platform.repository;

import com.scraper.platform.model.JobScrap;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface JobScrapRepository extends JpaRepository<JobScrap, Long> {

    List<JobScrap> findByUserIdOrderByCreatedAtDesc(Long userId);

    Optional<JobScrap> findByUserIdAndPostingId(Long userId, Long postingId);

    boolean existsByUserIdAndPostingId(Long userId, Long postingId);
}
