package com.paylens.backend.repository;

import com.paylens.backend.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    Optional<User> findByUserId(String userId);
    List<User> findByMerchantId(String merchantId);
    List<User> findByMerchantIdAndUserIdNot(String merchantId, String userId);
}
