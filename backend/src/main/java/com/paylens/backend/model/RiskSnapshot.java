package com.paylens.backend.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "risk_snapshots")
public class RiskSnapshot {

    @Id
    private String snapshotId;

    @Column(nullable = false)
    private Instant capturedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FinancialHealthStatus healthStatus;

    @Column(nullable = false)
    private int healthScore;

    @Column(precision = 19, scale = 2, nullable = false)
    private BigDecimal availableLiquidity;

    @Column(precision = 19, scale = 2, nullable = false)
    private BigDecimal safetyBuffer;

    @Column(precision = 19, scale = 2, nullable = false)
    private BigDecimal revenueAtRisk;

    @Column(precision = 19, scale = 2, nullable = false)
    private BigDecimal highRiskObligationAmount;

    @Column(nullable = false)
    private long unknownExecutionCount;

    @Column(nullable = false)
    private long manualReviewCount;

    @Column(nullable = false)
    private boolean isBaseline;

    public RiskSnapshot() {}

    public RiskSnapshot(String snapshotId, Instant capturedAt, FinancialHealthStatus healthStatus,
                        int healthScore, BigDecimal availableLiquidity, BigDecimal safetyBuffer,
                        BigDecimal revenueAtRisk, BigDecimal highRiskObligationAmount,
                        long unknownExecutionCount, long manualReviewCount, boolean isBaseline) {
        this.snapshotId = snapshotId;
        this.capturedAt = capturedAt;
        this.healthStatus = healthStatus;
        this.healthScore = healthScore;
        this.availableLiquidity = availableLiquidity;
        this.safetyBuffer = safetyBuffer;
        this.revenueAtRisk = revenueAtRisk;
        this.highRiskObligationAmount = highRiskObligationAmount;
        this.unknownExecutionCount = unknownExecutionCount;
        this.manualReviewCount = manualReviewCount;
        this.isBaseline = isBaseline;
    }

    public String getSnapshotId() { return snapshotId; }
    public Instant getCapturedAt() { return capturedAt; }
    public FinancialHealthStatus getHealthStatus() { return healthStatus; }
    public int getHealthScore() { return healthScore; }
    public BigDecimal getAvailableLiquidity() { return availableLiquidity; }
    public BigDecimal getSafetyBuffer() { return safetyBuffer; }
    public BigDecimal getRevenueAtRisk() { return revenueAtRisk; }
    public BigDecimal getHighRiskObligationAmount() { return highRiskObligationAmount; }
    public long getUnknownExecutionCount() { return unknownExecutionCount; }
    public long getManualReviewCount() { return manualReviewCount; }
    public boolean isBaseline() { return isBaseline; }
}
