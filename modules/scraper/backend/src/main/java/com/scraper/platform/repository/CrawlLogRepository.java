package com.scraper.platform.repository;

import com.scraper.platform.model.CrawlLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface CrawlLogRepository extends JpaRepository<CrawlLog, Long> {
    Page<CrawlLog> findByConfigIdOrderByStartedAtDesc(Long configId, Pageable pageable);
    List<CrawlLog> findTop10ByConfigIdOrderByStartedAtDesc(Long configId);

    /**
     * 재시작 시 스케줄 중복 실행 방지를 위해 마지막 실행 시각을 시드하는 용도.
     */
    Optional<CrawlLog> findFirstByConfigIdOrderByStartedAtDesc(Long configId);
    Page<CrawlLog> findByStatusOrderByStartedAtDesc(CrawlLog.CrawlStatus status, Pageable pageable);

    @EntityGraph(attributePaths = {"siteDefinition"})
    List<CrawlLog> findByConfigIdAndStartedAtBetweenOrderByStartedAtDesc(
            Long configId, LocalDateTime start, LocalDateTime end);

    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.transaction.annotation.Transactional
    void deleteByConfigId(Long configId);
}
