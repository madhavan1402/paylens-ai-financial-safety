package com.paylens.backend.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "reconciliation_records", indexes = {
        @Index(name = "idx_recon_id", columnList = "reconciliationId", unique = true),
        @Index(name = "idx_recon_exec_id", columnList = "executionId")
})
public class ReconciliationRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, updatable = false)
    private String reconciliationId;

    @Column(nullable = false, updatable = false)
    private String executionId;

    @Column(nullable = false, updatable = false)
    private String decisionId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ExecutionProvider provider;

    private String providerReference;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ExecutionStatus previousExecutionStatus;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ExecutionStatus resolvedExecutionStatus;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReconciliationStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NormalizedReconciliationOutcome providerOutcome;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RetryDecision retryDecision;

    private String resolution;
    private String failureCode;

    @Column(length = 2000)
    private String failureMessage;

    private int attemptNumber;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    private Instant resolvedAt;

    protected ReconciliationRecord() {}

    public ReconciliationRecord(String reconciliationId, String executionId, String decisionId, ExecutionProvider provider,
                                String providerReference, ExecutionStatus previousExecutionStatus, int attemptNumber) {
        this.reconciliationId = reconciliationId;
        this.executionId = executionId;
        this.decisionId = decisionId;
        this.provider = provider;
        this.providerReference = providerReference;
        this.previousExecutionStatus = previousExecutionStatus;
        this.resolvedExecutionStatus = previousExecutionStatus;
        this.status = ReconciliationStatus.PENDING;
        this.providerOutcome = NormalizedReconciliationOutcome.UNKNOWN;
        this.retryDecision = RetryDecision.NOT_SAFE;
        this.attemptNumber = attemptNumber;
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }

    @PreUpdate
    void touch() {
        this.updatedAt = Instant.now();
    }

    public Long getId() { return id; }
    public String getReconciliationId() { return reconciliationId; }
    public String getExecutionId() { return executionId; }
    public String getDecisionId() { return decisionId; }
    public ExecutionProvider getProvider() { return provider; }
    public String getProviderReference() { return providerReference; }
    public ExecutionStatus getPreviousExecutionStatus() { return previousExecutionStatus; }
    public ExecutionStatus getResolvedExecutionStatus() { return resolvedExecutionStatus; }
    public ReconciliationStatus getStatus() { return status; }
    public NormalizedReconciliationOutcome getProviderOutcome() { return providerOutcome; }
    public RetryDecision getRetryDecision() { return retryDecision; }
    public String getResolution() { return resolution; }
    public String getFailureCode() { return failureCode; }
    public String getFailureMessage() { return failureMessage; }
    public int getAttemptNumber() { return attemptNumber; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public Instant getResolvedAt() { return resolvedAt; }

    public void setResolvedExecutionStatus(ExecutionStatus status) { this.resolvedExecutionStatus = status; }
    public void setStatus(ReconciliationStatus status) { this.status = status; }
    public void setProviderOutcome(NormalizedReconciliationOutcome outcome) { this.providerOutcome = outcome; }
    public void setRetryDecision(RetryDecision retryDecision) { this.retryDecision = retryDecision; }
    public void setResolution(String resolution) { this.resolution = resolution; }
    public void setFailureCode(String failureCode) { this.failureCode = failureCode; }
    public void setFailureMessage(String failureMessage) { this.failureMessage = failureMessage; }
    public void setResolvedAt(Instant resolvedAt) { this.resolvedAt = resolvedAt; }
}
