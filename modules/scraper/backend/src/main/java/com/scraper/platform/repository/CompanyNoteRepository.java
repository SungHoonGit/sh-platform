package com.scraper.platform.repository;

import com.scraper.platform.model.CompanyNote;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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

    /**
     * 지정 메모들의 태그를 한 번에 조회한다 (목록 메모 태그 표시용, N+1 회피).
     *
     * @param ids 메모 ID 목록 (빈 목록 호출 금지)
     * @return [메모ID, 태그ID, 태그명] 행 목록 (정렬순)
     */
    @Query("SELECT n.id, r.id, r.name FROM CompanyNote n JOIN n.noteReasons r "
            + "WHERE n.id IN :ids ORDER BY r.sortOrder ASC")
    List<Object[]> findTagRowsByNoteIds(@Param("ids") List<Long> ids);
}
