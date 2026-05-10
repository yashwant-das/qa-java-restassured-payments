package com.payflowx.backend.repository;

import com.payflowx.backend.entity.MerchantEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MerchantRepository extends JpaRepository<MerchantEntity, String> {
}
