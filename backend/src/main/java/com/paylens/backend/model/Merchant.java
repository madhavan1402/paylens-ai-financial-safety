package com.paylens.backend.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "merchants", indexes = {
        @Index(name = "idx_merchant_id", columnList = "merchantId", unique = true)
})
public class Merchant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, updatable = false)
    private String merchantId;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String status; // ACTIVE, SUSPENDED

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    protected Merchant() {}

    public Merchant(String merchantId, String name) {
        this.merchantId = merchantId;
        this.name = name;
        this.status = "ACTIVE";
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }

    @PreUpdate
    void touch() {
        this.updatedAt = Instant.now();
    }

    public Long getId() { return id; }
    public String getMerchantId() { return merchantId; }
    public String getName() { return name; }
    public String getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    public void setName(String name) { this.name = name; }
    public void setStatus(String status) { this.status = status; }
}
