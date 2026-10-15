package io.github.yashwantdas.payments.backend.repository;

import io.github.yashwantdas.payments.backend.entity.SubscriptionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SubscriptionRepository extends JpaRepository<SubscriptionEntity, String> {
    Optional<SubscriptionEntity> findByTransactionId(String transactionId);
}
