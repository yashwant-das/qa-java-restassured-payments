package io.github.yashwantdas.payments.backend.repository;

import io.github.yashwantdas.payments.backend.entity.AuditLogEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditLogRepository extends JpaRepository<AuditLogEntity, Long> {
}
