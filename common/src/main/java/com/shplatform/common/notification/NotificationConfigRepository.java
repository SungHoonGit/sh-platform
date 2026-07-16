package com.shplatform.common.notification;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface NotificationConfigRepository extends JpaRepository<NotificationConfig, Long> {
    List<NotificationConfig> findByModuleNameAndIsEnabledTrue(String moduleName);
    Optional<NotificationConfig> findByModuleNameAndEventType(String moduleName, String eventType);
    List<NotificationConfig> findByIsEnabledTrue();
}
