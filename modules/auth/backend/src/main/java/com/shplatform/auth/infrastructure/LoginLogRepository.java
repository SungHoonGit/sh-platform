package com.shplatform.auth.infrastructure;

import java.time.LocalDateTime;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LoginLogRepository extends JpaRepository<LoginLogEntity, Long> {

    long countBySuccessAndCreatedAtAfter(boolean success, LocalDateTime after);

}
