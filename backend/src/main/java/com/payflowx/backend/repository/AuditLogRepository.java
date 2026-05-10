package com.payflowx.backend.repository;

import com.payflowx.backend.entity.AuditLogEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditLogRepository extends JpaRepository<AuditLogEntity, Long> {
}
