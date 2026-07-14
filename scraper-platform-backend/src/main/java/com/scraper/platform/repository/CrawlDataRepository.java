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
    Page<CrawlData> findByCategoryId(Long categoryId, Pageable pageable);
    Page<CrawlData> findByCategoryIdAndFileNameContaining(Long categoryId, String fileName, Pageable pageable);
    List<CrawlData> findBySourceUrl(String sourceUrl);
    
    @Query("SELECT c FROM CrawlData c WHERE c.title LIKE %:keyword% OR c.fileName LIKE %:keyword%")
    Page<CrawlData> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);
    
    long countByCategoryId(Long categoryId);
    
    // 고급 검색
    @Query("SELECT c FROM CrawlData c WHERE " +
           "(:keyword IS NULL OR c.title LIKE %:keyword%) AND " +
           "(:site IS NULL OR c.sourceSite = :site) AND " +
           "(:categoryId IS NULL OR c.category.id = :categoryId) AND " +
           "(:startDate IS NULL OR c.createdAt >= :startDate) AND " +
           "(:endDate IS NULL OR c.createdAt <= :endDate)")
    Page<CrawlData> advancedSearch(
            @Param("keyword") String keyword,
            @Param("site") String site,
            @Param("categoryId") Long categoryId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            Pageable pageable);
}
