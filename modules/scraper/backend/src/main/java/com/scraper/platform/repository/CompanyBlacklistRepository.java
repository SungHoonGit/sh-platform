package com.scraper.platform.repository;

import com.scraper.platform.model.CompanyBlacklist;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface CompanyBlacklistRepository extends JpaRepository<CompanyBlacklist, Long> {
    List<CompanyBlacklist> findByAccountIdOrderByCreatedAtDesc(Long accountId);
    boolean existsByAccountIdAndCompanyNameNormalized(Long accountId, String name);
}
