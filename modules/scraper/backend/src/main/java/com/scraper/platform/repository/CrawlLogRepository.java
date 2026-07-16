package com.scraper.platform.repository;

import com.scraper.platform.model.CrawlLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CrawlLogRepository extends JpaRepository<CrawlLog, Long> {
    Page<CrawlLog> findByConfigIdOrderByStartedAtDesc(Long configId, Pageable pageable);
    List<CrawlLog> findTop10ByConfigIdOrderByStartedAtDesc(Long configId);
    Page<CrawlLog> findByStatusOrderByStartedAtDesc(CrawlLog.CrawlStatus status, Pageable pageable);
}
