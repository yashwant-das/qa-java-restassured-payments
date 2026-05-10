package com.payflowx.backend.repository;

import com.payflowx.backend.entity.ProviderEntity;
import com.payflowx.backend.enums.ProviderStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProviderRepository extends JpaRepository<ProviderEntity, String> {
    List<ProviderEntity> findByStatus(ProviderStatus status);
}
