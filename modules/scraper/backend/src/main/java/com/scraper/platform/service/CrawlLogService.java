package com.scraper.platform.service;

import com.scraper.platform.model.CrawlLog;
import com.scraper.platform.repository.CrawlLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CrawlLogService {

    private final CrawlLogRepository crawlLogRepository;

    public Page<CrawlLog> getLogsByConfigId(Long configId, Pageable pageable) {
        return crawlLogRepository.findByConfigIdOrderByStartedAtDesc(configId, pageable);
    }

    public List<CrawlLog> getRecentLogsByConfigId(Long configId) {
        return crawlLogRepository.findTop10ByConfigIdOrderByStartedAtDesc(configId);
    }

    public Page<CrawlLog> getLogsByStatus(CrawlLog.CrawlStatus status, Pageable pageable) {
        return crawlLogRepository.findByStatusOrderByStartedAtDesc(status, pageable);
    }

    @Transactional
    public CrawlLog createLog(CrawlLog log) {
        if (log.getStartedAt() == null) {
            log.setStartedAt(LocalDateTime.now());
        }
        return crawlLogRepository.save(log);
    }

    @Transactional
    public CrawlLog completeLog(Long logId, CrawlLog.CrawlStatus status, int totalCount, int newCount, String errorMessage) {
        CrawlLog log = crawlLogRepository.findById(logId)
                .orElseThrow(() -> new RuntimeException("Log not found: " + logId));
        
        log.setStatus(status);
        log.setTotalCount(totalCount);
        log.setNewCount(newCount);
        log.setErrorMessage(errorMessage);
        log.setCompletedAt(LocalDateTime.now());
        
        return crawlLogRepository.save(log);
    }
}
