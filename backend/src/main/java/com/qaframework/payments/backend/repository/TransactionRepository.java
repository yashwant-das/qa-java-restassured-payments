package com.qaframework.payments.backend.repository;

import com.qaframework.payments.backend.entity.TransactionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TransactionRepository extends JpaRepository<TransactionEntity, String> {
}
