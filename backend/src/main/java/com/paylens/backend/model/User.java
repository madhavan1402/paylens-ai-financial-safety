package com.paylens.backend.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "users", indexes = {
        @Index(name = "idx_user_email", columnList = "email", unique = true),
        @Index(name = "idx_user_merchant", columnList = "merchantId")
})
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, updatable = false)
    private String userId;

    @Column(nullable = false)
    private String merchantId;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(nullable = false)
    private String passwordHash;

    @Column(nullable = false)
    private String displayName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserRole role;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserStatus status;

    private int failedLoginAttempts;
    private Instant lockedUntil;
    private Instant lastLoginAt;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    protected User() {}

    public User(String userId, String merchantId, String email, String passwordHash, String displayName, UserRole role) {
        this.userId = userId;
        this.merchantId = merchantId;
        this.email = email.toLowerCase().trim();
        this.passwordHash = passwordHash;
        this.displayName = displayName;
        this.role = role;
        this.status = UserStatus.ACTIVE;
        this.failedLoginAttempts = 0;
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }

    @PreUpdate
    void touch() {
        this.updatedAt = Instant.now();
    }

    public Long getId() { return id; }
    public String getUserId() { return userId; }
    public String getMerchantId() { return merchantId; }
    public String getEmail() { return email; }
    public String getPasswordHash() { return passwordHash; }
    public String getDisplayName() { return displayName; }
    public UserRole getRole() { return role; }
    public UserStatus getStatus() { return status; }
    public int getFailedLoginAttempts() { return failedLoginAttempts; }
    public Instant getLockedUntil() { return lockedUntil; }
    public Instant getLastLoginAt() { return lastLoginAt; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
    public void setRole(UserRole role) { this.role = role; }
    public void setStatus(UserStatus status) { this.status = status; }
    public void setFailedLoginAttempts(int failedLoginAttempts) { this.failedLoginAttempts = failedLoginAttempts; }
    public void setLockedUntil(Instant lockedUntil) { this.lockedUntil = lockedUntil; }
    public void setLastLoginAt(Instant lastLoginAt) { this.lastLoginAt = lastLoginAt; }
}
