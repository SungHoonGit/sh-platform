package com.shplatform.common.notification;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PushSubscriptionRepository extends JpaRepository<PushSubscription, Long> {

    List<PushSubscription> findByAccountIdAndIsActiveTrue(Long accountId);

    Optional<PushSubscription> findByEndpoint(String endpoint);

    @Modifying
    @Query("UPDATE PushSubscription p SET p.isActive = false WHERE p.endpoint = :endpoint")
    int deactivateByEndpoint(String endpoint);
}
