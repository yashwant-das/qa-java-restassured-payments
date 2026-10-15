package io.github.yashwantdas.payments.backend.repository;

import io.github.yashwantdas.payments.backend.entity.MerchantEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MerchantRepository extends JpaRepository<MerchantEntity, String> {
}
