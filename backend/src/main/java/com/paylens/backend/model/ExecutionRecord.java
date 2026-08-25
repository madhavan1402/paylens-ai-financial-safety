package com.paylens.backend.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "execution_records", indexes = {
        @Index(name = "idx_exec_idempotency", columnList = "idempotencyKey", unique = true),
        @Index(name = "idx_exec_decision_id", columnList = "decisionId")
})
public class ExecutionRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, updatable = false)
    private String executionId;

    @Column(nullable = false, updatable = false)
    private String decisionId;

    @Column(unique = true, nullable = false, updatable = false)
    private String idempotencyKey;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ExecutionProvider provider;

    private String providerReference;
    private String actionType;
    private BigDecimal amount;
    private String currency;
    private String target;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ExecutionStatus status;

    private String failureCode;
    @Column(length = 2000)
    private String failureMessage;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    protected ExecutionRecord() {}

    public ExecutionRecord(String executionId, String decisionId, String idempotencyKey, ExecutionProvider provider,
                           String actionType, BigDecimal amount, String currency, String target, ExecutionStatus status) {
        this.executionId = executionId;
        this.decisionId = decisionId;
        this.idempotencyKey = idempotencyKey;
        this.provider = provider;
        this.actionType = actionType;
        this.amount = amount;
        this.currency = currency;
        this.target = target;
        this.status = status;
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }

    @PreUpdate
    void touch() {
        this.updatedAt = Instant.now();
    }

    public Long getId() { return id; }
    public String getExecutionId() { return executionId; }
    public String getDecisionId() { return decisionId; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public ExecutionProvider getProvider() { return provider; }
    public String getProviderReference() { return providerReference; }
    public String getActionType() { return actionType; }
    public BigDecimal getAmount() { return amount; }
    public String getCurrency() { return currency; }
    public String getTarget() { return target; }
    public ExecutionStatus getStatus() { return status; }
    public String getFailureCode() { return failureCode; }
    public String getFailureMessage() { return failureMessage; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    public void setStatus(ExecutionStatus status) { this.status = status; }
    public void setProviderReference(String providerReference) { this.providerReference = providerReference; }
    public void setFailureCode(String failureCode) { this.failureCode = failureCode; }
    public void setFailureMessage(String failureMessage) { this.failureMessage = failureMessage; }
}
