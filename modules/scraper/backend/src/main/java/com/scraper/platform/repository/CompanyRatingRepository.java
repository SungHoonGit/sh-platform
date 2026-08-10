package com.scraper.platform.repository;

import com.scraper.platform.model.CompanyRating;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 기업 평점 Repository.
 */
@Repository
public interface CompanyRatingRepository extends JpaRepository<CompanyRating, Long> {

    Optional<CompanyRating> findByCompanyName(String companyName);

    List<CompanyRating> findByCompanyNameIn(List<String> companyNames);

    @Query("SELECT c FROM CompanyRating c WHERE c.companyName IN :companyNames AND c.lastUpdatedAt > :since")
    List<CompanyRating> findByCompanyNameInAndLastUpdatedAtAfter(
        @Param("companyNames") List<String> companyNames,
        @Param("since") LocalDateTime since
    );

    @Query("SELECT c.companyName FROM CompanyRating c WHERE c.companyName IN :companyNames AND c.lastUpdatedAt > :since")
    List<String> findCachedCompanyNames(
        @Param("companyNames") List<String> companyNames,
        @Param("since") LocalDateTime since
    );

    @Query("SELECT c FROM CompanyRating c WHERE c.averageScore IS NOT NULL ORDER BY c.averageScore DESC")
    List<CompanyRating> findTopRatedCompanies();
}
