package io.github.yashwantdas.payments.backend.repository;

import io.github.yashwantdas.payments.backend.entity.ProviderEntity;
import io.github.yashwantdas.payments.backend.enums.ProviderStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProviderRepository extends JpaRepository<ProviderEntity, String> {
    List<ProviderEntity> findByStatus(ProviderStatus status);
}
