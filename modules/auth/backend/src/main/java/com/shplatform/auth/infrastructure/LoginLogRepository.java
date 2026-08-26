package com.shplatform.auth.infrastructure;

import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LoginLogRepository extends JpaRepository<LoginLogEntity, Long> {

    List<LoginLogEntity> findByUserIdOrderByCreatedAtDesc(Long userId, LocalDateTime since);

    long countBySuccessAndCreatedAtAfter(boolean success, LocalDateTime after);
}
