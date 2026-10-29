package com.qaframework.payments.backend.repository;

import com.qaframework.payments.backend.entity.SubscriptionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SubscriptionRepository extends JpaRepository<SubscriptionEntity, String> {
    Optional<SubscriptionEntity> findByTransactionId(String transactionId);
}
