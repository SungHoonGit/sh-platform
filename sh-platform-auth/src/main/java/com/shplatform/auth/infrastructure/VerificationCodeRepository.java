package com.shplatform.auth.infrastructure;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VerificationCodeRepository extends JpaRepository<VerificationCodeEntity, Long> {
    Optional<VerificationCodeEntity> findTopByEmailAndPurposeOrderByCreatedAtDesc(
            String email, String purpose);
}
