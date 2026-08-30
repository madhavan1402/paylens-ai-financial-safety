package com.paylens.backend.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "refresh_tokens", indexes = {
        @Index(name = "idx_token_user", columnList = "userId"),
        @Index(name = "idx_token_hash", columnList = "tokenHash", unique = true)
})
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, updatable = false)
    private String tokenId;

    @Column(nullable = false, updatable = false)
    private String userId;

    @Column(nullable = false, updatable = false)
    private String merchantId;

    @Column(unique = true, nullable = false)
    private String tokenHash;

    @Column(nullable = false)
    private Instant expiresAt;

    private boolean revoked;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    protected RefreshToken() {}

    public RefreshToken(String tokenId, String userId, String merchantId, String tokenHash, Instant expiresAt) {
        this.tokenId = tokenId;
        this.userId = userId;
        this.merchantId = merchantId;
        this.tokenHash = tokenHash;
        this.expiresAt = expiresAt;
        this.revoked = false;
        this.createdAt = Instant.now();
    }

    public Long getId() { return id; }
    public String getTokenId() { return tokenId; }
    public String getUserId() { return userId; }
    public String getMerchantId() { return merchantId; }
    public String getTokenHash() { return tokenHash; }
    public Instant getExpiresAt() { return expiresAt; }
    public boolean isRevoked() { return revoked; }
    public Instant getCreatedAt() { return createdAt; }

    public void setRevoked(boolean revoked) { this.revoked = revoked; }
}
