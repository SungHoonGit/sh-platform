package com.shplatform.common.notification;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface NotificationLogRepository extends JpaRepository<NotificationLog, Long> {
    Page<NotificationLog> findByModuleNameOrderByCreatedAtDesc(String moduleName, Pageable pageable);
    List<NotificationLog> findTop10ByModuleNameOrderByCreatedAtDesc(String moduleName);
    List<NotificationLog> findByStatusOrderByCreatedAtDesc(NotificationLog.NotificationStatus status);
}
