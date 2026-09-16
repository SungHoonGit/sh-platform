package com.scraper.platform.repository;

import com.scraper.platform.model.CompanyNote;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 계정별 회사 메모 Repository.
 */
@Repository
public interface CompanyNoteRepository extends JpaRepository<CompanyNote, Long> {

    Page<CompanyNote> findByAccountId(Long accountId, Pageable pageable);

    Page<CompanyNote> findByAccountIdAndIsBookmarkedTrue(Long accountId, Pageable pageable);

    Page<CompanyNote> findByAccountIdAndCompanyNameDisplayContainingIgnoreCase(
            Long accountId, String query, Pageable pageable);

    Page<CompanyNote> findByAccountIdAndIsBookmarkedTrueAndCompanyNameDisplayContainingIgnoreCase(
            Long accountId, String query, Pageable pageable);

    Optional<CompanyNote> findByAccountIdAndCompanyNameNormalized(Long accountId, String normalized);

    List<CompanyNote> findByAccountIdAndCompanyNameNormalizedIn(Long accountId, List<String> normalizedNames);
}
