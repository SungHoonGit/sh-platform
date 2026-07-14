package com.shplatform.common.scheduling;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ScheduleService {

    private final ScheduleConfigRepository configRepository;
    private final ScheduleLogRepository logRepository;

    @Transactional(readOnly = true)
    public List<ScheduleConfig> getEnabledConfigs() {
        return configRepository.findByIsEnabledTrue();
    }

    @Transactional(readOnly = true)
    public List<ScheduleConfig> getConfigsByModule(String moduleName) {
        return configRepository.findByModuleNameAndIsEnabledTrue(moduleName);
    }

    @Transactional
    public ScheduleConfig createConfig(ScheduleConfig config) {
        validateCron(config.getCron());
        return configRepository.save(config);
    }

    @Transactional
    public ScheduleConfig updateConfig(Long id, ScheduleConfig updated) {
        ScheduleConfig config = configRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Schedule config not found: " + id));
        
        config.setCron(updated.getCron());
        config.setIsEnabled(updated.getIsEnabled());
        config.setTaskName(updated.getTaskName());
        
        validateCron(config.getCron());
        return configRepository.save(config);
    }

    @Transactional
    public void deleteConfig(Long id) {
        configRepository.deleteById(id);
    }

    @Transactional
    public ScheduleLog startLog(Long configId) {
        ScheduleConfig config = configRepository.findById(configId)
            .orElseThrow(() -> new IllegalArgumentException("Schedule config not found: " + configId));
        
        ScheduleLog scheduleLog = ScheduleLog.builder()
            .scheduleConfig(config)
            .status(ScheduleLog.ScheduleStatus.RUNNING)
            .startedAt(LocalDateTime.now())
            .build();
        
        return logRepository.save(scheduleLog);
    }

    @Transactional
    public void completeLog(Long logId, boolean success, String errorMessage) {
        ScheduleLog scheduleLog = logRepository.findById(logId)
            .orElseThrow(() -> new IllegalArgumentException("Schedule log not found: " + logId));
        
        scheduleLog.setCompletedAt(LocalDateTime.now());
        scheduleLog.setStatus(success ? ScheduleLog.ScheduleStatus.SUCCESS : ScheduleLog.ScheduleStatus.FAILED);
        scheduleLog.setErrorMessage(errorMessage);
        logRepository.save(scheduleLog);
        
        ScheduleConfig config = scheduleLog.getScheduleConfig();
        config.setLastExecutedAt(LocalDateTime.now());
        configRepository.save(config);
    }

    @Transactional
    public void updateCounts(Long logId, int total, int success, int error) {
        ScheduleLog scheduleLog = logRepository.findById(logId)
            .orElseThrow(() -> new IllegalArgumentException("Schedule log not found: " + logId));
        
        scheduleLog.setTotalCount(total);
        scheduleLog.setSuccessCount(success);
        scheduleLog.setErrorCount(error);
        
        if (error > 0 && success > 0) {
            scheduleLog.setStatus(ScheduleLog.ScheduleStatus.PARTIAL);
        }
        
        logRepository.save(scheduleLog);
    }

    @Transactional(readOnly = true)
    public List<ScheduleLog> getRecentLogs(Long configId) {
        return logRepository.findTop10ByScheduleConfigIdOrderByStartedAtDesc(configId);
    }

    private void validateCron(String cron) {
        if (!CronExpression.isValidExpression(cron)) {
            throw new IllegalArgumentException("Invalid cron expression: " + cron);
        }
    }
}
