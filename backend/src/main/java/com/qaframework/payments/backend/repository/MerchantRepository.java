package com.qaframework.payments.backend.repository;

import com.qaframework.payments.backend.entity.MerchantEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MerchantRepository extends JpaRepository<MerchantEntity, String> {
}
