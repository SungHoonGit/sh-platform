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
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CrawlLogService {

    private static final long GROUPING_MINUTES = 5;

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

        Map<LocalDate, List<CrawlLog>> dateMap = logs.stream()
                .collect(Collectors.groupingBy(
                        log -> log.getStartedAt().toLocalDate(),
                        LinkedHashMap::new,
                        Collectors.toList()
                ));

        List<CrawlLogGroupResponse> result = new ArrayList<>();
        for (Map.Entry<LocalDate, List<CrawlLog>> entry : dateMap.entrySet()) {
            List<CrawlRunGroup> groupedRuns = groupByTimeProximity(entry.getValue());

            int totalNew = groupedRuns.stream().mapToInt(CrawlRunGroup::getNewCount).sum();
            result.add(CrawlLogGroupResponse.builder()
                    .date(entry.getKey())
                    .totalNewCount(totalNew)
                    .totalRunCount(groupedRuns.size())
                    .runs(groupedRuns)
                    .build());
        }

        return result;
    }

    private List<CrawlRunGroup> groupByTimeProximity(List<CrawlLog> logs) {
        List<CrawlLog> sorted = logs.stream()
                .sorted(Comparator.comparing(CrawlLog::getStartedAt).reversed())
                .collect(Collectors.toList());

        List<List<CrawlLog>> groups = new ArrayList<>();
        List<CrawlLog> currentGroup = new ArrayList<>();

        for (CrawlLog log : sorted) {
            if (currentGroup.isEmpty()) {
                currentGroup.add(log);
            } else {
                LocalDateTime groupTime = currentGroup.get(0).getStartedAt();
                long minutesBetween = ChronoUnit.MINUTES.between(log.getStartedAt(), groupTime);
                if (minutesBetween <= GROUPING_MINUTES) {
                    currentGroup.add(log);
                } else {
                    groups.add(currentGroup);
                    currentGroup = new ArrayList<>();
                    currentGroup.add(log);
                }
            }
        }
        if (!currentGroup.isEmpty()) {
            groups.add(currentGroup);
        }

        return groups.stream().map(group -> {
            CrawlLog representative = group.get(0);
            List<String> siteNames = group.stream()
                    .map(l -> l.getSiteDefinition() != null ? l.getSiteDefinition().getSiteName() : "unknown")
                    .collect(Collectors.toList());

            boolean allSuccess = group.stream()
                    .allMatch(l -> l.getStatus() == CrawlLog.CrawlStatus.SUCCESS);
            boolean anyFailed = group.stream()
                    .anyMatch(l -> l.getStatus() == CrawlLog.CrawlStatus.FAILED);

            CrawlLog.CrawlStatus combinedStatus;
            if (allSuccess) {
                combinedStatus = CrawlLog.CrawlStatus.SUCCESS;
            } else if (anyFailed) {
                combinedStatus = CrawlLog.CrawlStatus.FAILED;
            } else {
                combinedStatus = CrawlLog.CrawlStatus.PARTIAL;
            }

            int totalCount = group.stream().mapToInt(l -> l.getTotalCount() != null ? l.getTotalCount() : 0).sum();
            int newCount = group.stream().mapToInt(l -> l.getNewCount() != null ? l.getNewCount() : 0).sum();

            return CrawlRunGroup.builder()
                    .logId(representative.getId())
                    .startedAt(representative.getStartedAt())
                    .status(combinedStatus.name())
                    .totalCount(totalCount)
                    .newCount(newCount)
                    .siteCount(siteNames.size())
                    .siteNames(siteNames)
                    .build();
        }).collect(Collectors.toList());
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
