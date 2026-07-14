package com.shplatform.common.scheduling;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface ScheduleConfigRepository extends JpaRepository<ScheduleConfig, Long> {
    List<ScheduleConfig> findByModuleNameAndIsEnabledTrue(String moduleName);
    Optional<ScheduleConfig> findByModuleNameAndTaskName(String moduleName, String taskName);
    List<ScheduleConfig> findByIsEnabledTrue();
}
