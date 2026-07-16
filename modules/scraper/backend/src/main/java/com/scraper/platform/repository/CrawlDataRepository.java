package com.scraper.platform.repository;

import com.scraper.platform.model.CrawlData;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface CrawlDataRepository extends JpaRepository<CrawlData, Long> {
    Page<CrawlData> findByCategory(String category, Pageable pageable);
    Page<CrawlData> findByCategoryAndFileNameContaining(String category, String fileName, Pageable pageable);
    List<CrawlData> findBySourceUrl(String sourceUrl);
    
    @Query("SELECT c FROM CrawlData c WHERE c.title LIKE %:keyword% OR c.fileName LIKE %:keyword%")
    Page<CrawlData> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);
    
    long countByCategory(String category);
    
    @Query("SELECT c FROM CrawlData c WHERE " +
           "(:keyword IS NULL OR c.title LIKE %:keyword%) AND " +
           "(:site IS NULL OR c.sourceSite = :site) AND " +
           "(:category IS NULL OR c.category = :category) AND " +
           "(:startDate IS NULL OR c.createdAt >= :startDate) AND " +
           "(:endDate IS NULL OR c.createdAt <= :endDate)")
    Page<CrawlData> advancedSearch(
            @Param("keyword") String keyword,
            @Param("site") String site,
            @Param("category") String category,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            Pageable pageable);
}
