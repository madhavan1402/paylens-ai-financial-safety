package com.paylens.backend.repository;

import com.paylens.backend.model.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
    Optional<RefreshToken> findByTokenHash(String tokenHash);
    List<RefreshToken> findByUserIdAndRevokedFalse(String userId);
    void deleteByExpiresAtBefore(Instant now);
}
