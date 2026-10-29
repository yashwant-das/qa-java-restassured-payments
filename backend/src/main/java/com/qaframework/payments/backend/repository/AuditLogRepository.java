package com.qaframework.payments.backend.repository;

import com.qaframework.payments.backend.entity.AuditLogEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditLogRepository extends JpaRepository<AuditLogEntity, Long> {
}
