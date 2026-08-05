package com.scraper.platform.service;

import com.scraper.platform.api.dto.CrawlLogGroupResponse;
import com.scraper.platform.api.dto.CrawlLogGroupResponse.CrawlRunGroup;
import com.scraper.platform.model.CrawlLog;
import com.scraper.platform.repository.CrawlLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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

    public List<CrawlLogGroupResponse> getLogsGroupedByDate(Long configId, int days) {
        LocalDateTime since = LocalDateTime.now().minusDays(days);
        List<CrawlLog> logs = crawlLogRepository.findByConfigIdAndStartedAtBetweenOrderByStartedAtDesc(
                configId, since, LocalDateTime.now());

        Map< LocalDate, List<CrawlRunGroup>> dateMap = new LinkedHashMap<>();

        for (CrawlLog log : logs) {
            LocalDate date = log.getStartedAt().toLocalDate();

            CrawlRunGroup run = CrawlRunGroup.builder()
                    .logId(log.getId())
                    .startedAt(log.getStartedAt())
                    .status(log.getStatus().name())
                    .totalCount(log.getTotalCount() != null ? log.getTotalCount() : 0)
                    .newCount(log.getNewCount() != null ? log.getNewCount() : 0)
                    .build();

            dateMap.computeIfAbsent(date, k -> new ArrayList<>()).add(run);
        }

        List<CrawlLogGroupResponse> result = new ArrayList<>();
        for (Map.Entry<LocalDate, List<CrawlRunGroup>> entry : dateMap.entrySet()) {
            int totalNew = entry.getValue().stream().mapToInt(CrawlRunGroup::getNewCount).sum();
            result.add(CrawlLogGroupResponse.builder()
                    .date(entry.getKey())
                    .totalNewCount(totalNew)
                    .runs(entry.getValue())
                    .build());
        }

        return result;
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
