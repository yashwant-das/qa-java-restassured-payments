package io.github.yashwantdas.payments.backend.repository;

import io.github.yashwantdas.payments.backend.entity.TransactionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TransactionRepository extends JpaRepository<TransactionEntity, String> {
}
