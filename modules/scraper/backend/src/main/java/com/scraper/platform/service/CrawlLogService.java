package com.scraper.platform.service;

import com.scraper.platform.api.dto.CrawlLogGroupResponse;
import com.scraper.platform.api.dto.CrawlLogGroupResponse.CrawlRunGroup;
import com.scraper.platform.model.CrawlLog;
import com.scraper.platform.repository.CrawlLogRepository;
import com.scraper.platform.repository.JobPostingRepository;
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

    private static final long GROUPING_MINUTES = 30;

    private final CrawlLogRepository crawlLogRepository;
    private final JobPostingRepository jobPostingRepository;

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
        List<Object[]> dateCounts = jobPostingRepository.countByConfigIdGroupedByDate(configId);

        List<CrawlLog> allLogs = crawlLogRepository.findByConfigIdAndStartedAtBetweenOrderByStartedAtDesc(
                configId, LocalDateTime.now().minusDays(days), LocalDateTime.now());

        List<CrawlLogGroupResponse> result = new ArrayList<>();
        for (Object[] row : dateCounts) {
            LocalDate date = (LocalDate) row[0];
            Long totalCount = (Long) row[1];

            LocalDate logDateMin = date;
            LocalDate logDateMax = date;
            List<CrawlLog> dayLogs = allLogs.stream()
                    .filter(l -> {
                        LocalDate logDate = l.getStartedAt().toLocalDate();
                        return !logDate.isBefore(logDateMin) && !logDate.isAfter(logDateMax);
                    })
                    .collect(Collectors.toList());

            List<CrawlRunGroup> groupedRuns = groupByBatch(dayLogs);
            groupedRuns = markNewCriteria(groupedRuns);

            result.add(CrawlLogGroupResponse.builder()
                    .date(date)
                    .totalNewCount(totalCount.intValue())
                    .totalRunCount(Math.max(groupedRuns.size(), 1))
                    .runs(groupedRuns)
                    .build());
        }

        return result;
    }

    private List<CrawlRunGroup> groupByBatch(List<CrawlLog> logs) {
        if (logs.isEmpty()) return Collections.emptyList();

        List<CrawlLog> sorted = logs.stream()
                .sorted(Comparator.comparing(CrawlLog::getStartedAt).reversed())
                .collect(Collectors.toList());

        List<List<CrawlLog>> groups = new ArrayList<>();
        Map<String, List<CrawlLog>> batchGroups = new LinkedHashMap<>();
        List<CrawlLog> legacyLogs = new ArrayList<>();

        for (CrawlLog log : sorted) {
            String batchId = log.getBatchId();
            if (batchId != null && !batchId.isEmpty()) {
                // batch_id가 있으면 같은 배치끼리 그룹핑
                batchGroups.computeIfAbsent(batchId, k -> new ArrayList<>()).add(log);
            } else {
                // batch_id 없는 레거시 로그는 시간 근접성으로 그룹핑
                legacyLogs.add(log);
            }
        }
        groups.addAll(batchGroups.values());
        groups.addAll(groupLegacyByTimeProximity(legacyLogs));

        // 최신 실행부터 표시
        groups.sort(Comparator.comparing(
                l -> l.stream().map(CrawlLog::getStartedAt).max(LocalDateTime::compareTo).orElse(LocalDateTime.MIN),
                Comparator.reverseOrder()));

        return groups.stream().map(this::toRunGroup).collect(Collectors.toList());
    }

    /**
     * batch_id가 없는 레거시 로그를 시작 시간 근접성 기준으로 그룹핑한다.
     * 같은 실행에서 만들어진 사이트별 로그(수십 초 간격)는 한 그룹으로 묶고,
     * 별도의 실행(수 분 이상 간격)은 분리한다.
     *
     * @param logs batch_id가 없는 로그 목록
     * @return 시간 근접 그룹 목록
     */
    private List<List<CrawlLog>> groupLegacyByTimeProximity(List<CrawlLog> logs) {
        List<List<CrawlLog>> groups = new ArrayList<>();
        if (logs.isEmpty()) return groups;

        List<CrawlLog> sorted = logs.stream()
                .sorted(Comparator.comparing(CrawlLog::getStartedAt).reversed())
                .collect(Collectors.toList());

        List<CrawlLog> currentGroup = new ArrayList<>();
        for (CrawlLog log : sorted) {
            if (currentGroup.isEmpty()) {
                currentGroup.add(log);
            } else {
                LocalDateTime groupTime = currentGroup.get(0).getStartedAt();
                long minutesBetween = Math.abs(ChronoUnit.MINUTES.between(log.getStartedAt(), groupTime));
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
        return groups;
    }

    private CrawlRunGroup toRunGroup(List<CrawlLog> group) {
        CrawlLog representative = group.get(0);
        List<String> siteNames = group.stream()
                .map(l -> l.getSiteDefinition() != null ? l.getSiteDefinition().getSiteName() : "unknown")
                .collect(Collectors.toList());

        boolean allSuccess = group.stream()
                .allMatch(l -> l.getStatus() == CrawlLog.CrawlStatus.SUCCESS);
        boolean anyFailed = group.stream()
                .anyMatch(l -> l.getStatus() == CrawlLog.CrawlStatus.FAILED);

        CrawlLog.CrawlStatus combinedStatus;
        if (allSuccess) combinedStatus = CrawlLog.CrawlStatus.SUCCESS;
        else if (anyFailed) combinedStatus = CrawlLog.CrawlStatus.FAILED;
        else combinedStatus = CrawlLog.CrawlStatus.PARTIAL;

        int totalCount = group.stream().mapToInt(l -> l.getTotalCount() != null ? l.getTotalCount() : 0).sum();
        int newCount = group.stream().mapToInt(l -> l.getNewCount() != null ? l.getNewCount() : 0).sum();

        List<Long> logIds = group.stream()
                .map(CrawlLog::getId)
                .collect(Collectors.toList());

        return CrawlRunGroup.builder()
                .logId(representative.getId())
                .logIds(logIds)
                .startedAt(representative.getStartedAt())
                .status(combinedStatus.name())
                .totalCount(totalCount)
                .newCount(newCount)
                .siteCount(siteNames.size())
                .siteNames(siteNames)
                .searchCriteria(representative.getSearchCriteria())
                .build();
    }

    /**
     * 직전 실행 대비 검색 조건이 변경된 실행에 newCriteria 플래그를 설정한다.
     * 최신 실행부터 정렬된 목록에서 이전(더 과거) 실행과 비교한다.
     *
     * @param runs 최신순으로 정렬된 실행 그룹 목록
     * @return newCriteria가 마킹된 목록
     */
    private List<CrawlRunGroup> markNewCriteria(List<CrawlRunGroup> runs) {
        if (runs.size() <= 1) return runs;

        for (int i = 0; i < runs.size(); i++) {
            CrawlRunGroup current = runs.get(i);
            String currentCriteria = current.getSearchCriteria();
            for (int j = i + 1; j < runs.size(); j++) {
                CrawlRunGroup previous = runs.get(j);
                String previousCriteria = previous.getSearchCriteria();
                if (!Objects.equals(currentCriteria, previousCriteria)) {
                    current.setNewCriteria(true);
                    break;
                }
            }
        }
        return runs;
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
