package com.shplatform.common.scheduling;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ScheduleLogRepository extends JpaRepository<ScheduleLog, Long> {
    Page<ScheduleLog> findByScheduleConfigIdOrderByStartedAtDesc(Long configId, Pageable pageable);
    List<ScheduleLog> findTop10ByScheduleConfigIdOrderByStartedAtDesc(Long configId);
    List<ScheduleLog> findByStatusOrderByStartedAtDesc(ScheduleLog.ScheduleStatus status);
}
