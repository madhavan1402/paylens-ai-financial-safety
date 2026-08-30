package com.paylens.backend.repository;

import com.paylens.backend.model.Merchant;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface MerchantRepository extends JpaRepository<Merchant, Long> {
    Optional<Merchant> findByMerchantId(String merchantId);
}
