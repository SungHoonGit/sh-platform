package com.scraper.platform.repository;

import com.scraper.platform.model.CompanyBlacklist;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface CompanyBlacklistRepository extends JpaRepository<CompanyBlacklist, Long> {
    @Query("SELECT DISTINCT b FROM CompanyBlacklist b LEFT JOIN FETCH b.blockReasons " +
            "WHERE b.accountId = :accountId ORDER BY b.createdAt DESC")
    List<CompanyBlacklist> findByAccountIdOrderByCreatedAtDesc(@Param("accountId") Long accountId);
    boolean existsByAccountIdAndCompanyNameNormalized(Long accountId, String name);
}
