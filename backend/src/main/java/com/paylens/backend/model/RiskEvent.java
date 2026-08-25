package com.paylens.backend.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "risk_events", indexes = {
        @Index(name = "idx_risk_event_fingerprint", columnList = "fingerprint")
})
public class RiskEvent {

    @Id
    private String riskEventId;

    @Column(nullable = false)
    private String fingerprint;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RiskSignalType riskSignalType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RiskSeverity severity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RiskPriority priority;

    @Column(nullable = false)
    private String title;

    @Column(length = 2000, nullable = false)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RiskEventStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RiskEventSource source;

    @Column(nullable = false)
    private Instant detectedAt;

    @Column(nullable = false)
    private Instant firstDetectedAt;

    @Column(nullable = false)
    private Instant lastDetectedAt;

    private Instant resolvedAt;

    @Column(nullable = false)
    private int occurrenceCount;

    private String relatedEntityType;
    private String relatedEntityId;

    @Column(length = 1000)
    private String recommendedAction;

    @Column(precision = 19, scale = 2)
    private BigDecimal financialImpact;

    private String dismissalReason;
    private String resolutionReason;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    public RiskEvent() {}

    public RiskEvent(String riskEventId, String fingerprint, RiskSignalType riskSignalType,
                     RiskSeverity severity, RiskPriority priority, String title, String description,
                     RiskEventStatus status, RiskEventSource source, Instant detectedAt,
                     Instant firstDetectedAt, Instant lastDetectedAt, Instant resolvedAt,
                     int occurrenceCount, String relatedEntityType, String relatedEntityId,
                     String recommendedAction, BigDecimal financialImpact, String dismissalReason,
                     String resolutionReason, Instant createdAt, Instant updatedAt) {
        this.riskEventId = riskEventId;
        this.fingerprint = fingerprint;
        this.riskSignalType = riskSignalType;
        this.severity = severity;
        this.priority = priority;
        this.title = title;
        this.description = description;
        this.status = status;
        this.source = source;
        this.detectedAt = detectedAt;
        this.firstDetectedAt = firstDetectedAt;
        this.lastDetectedAt = lastDetectedAt;
        this.resolvedAt = resolvedAt;
        this.occurrenceCount = occurrenceCount;
        this.relatedEntityType = relatedEntityType;
        this.relatedEntityId = relatedEntityId;
        this.recommendedAction = recommendedAction;
        this.financialImpact = financialImpact;
        this.dismissalReason = dismissalReason;
        this.resolutionReason = resolutionReason;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public String getRiskEventId() { return riskEventId; }
    public void setRiskEventId(String riskEventId) { this.riskEventId = riskEventId; }

    public String getFingerprint() { return fingerprint; }
    public void setFingerprint(String fingerprint) { this.fingerprint = fingerprint; }

    public RiskSignalType getRiskSignalType() { return riskSignalType; }
    public void setRiskSignalType(RiskSignalType riskSignalType) { this.riskSignalType = riskSignalType; }

    public RiskSeverity getSeverity() { return severity; }
    public void setSeverity(RiskSeverity severity) { this.severity = severity; }

    public RiskPriority getPriority() { return priority; }
    public void setPriority(RiskPriority priority) { this.priority = priority; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public RiskEventStatus getStatus() { return status; }
    public void setStatus(RiskEventStatus status) { this.status = status; }

    public RiskEventSource getSource() { return source; }
    public void setSource(RiskEventSource source) { this.source = source; }

    public Instant getDetectedAt() { return detectedAt; }
    public void setDetectedAt(Instant detectedAt) { this.detectedAt = detectedAt; }

    public Instant getFirstDetectedAt() { return firstDetectedAt; }
    public void setFirstDetectedAt(Instant firstDetectedAt) { this.firstDetectedAt = firstDetectedAt; }

    public Instant getLastDetectedAt() { return lastDetectedAt; }
    public void setLastDetectedAt(Instant lastDetectedAt) { this.lastDetectedAt = lastDetectedAt; }

    public Instant getResolvedAt() { return resolvedAt; }
    public void setResolvedAt(Instant resolvedAt) { this.resolvedAt = resolvedAt; }

    public int getOccurrenceCount() { return occurrenceCount; }
    public void setOccurrenceCount(int occurrenceCount) { this.occurrenceCount = occurrenceCount; }

    public String getRelatedEntityType() { return relatedEntityType; }
    public void setRelatedEntityType(String relatedEntityType) { this.relatedEntityType = relatedEntityType; }

    public String getRelatedEntityId() { return relatedEntityId; }
    public void setRelatedEntityId(String relatedEntityId) { this.relatedEntityId = relatedEntityId; }

    public String getRecommendedAction() { return recommendedAction; }
    public void setRecommendedAction(String recommendedAction) { this.recommendedAction = recommendedAction; }

    public BigDecimal getFinancialImpact() { return financialImpact; }
    public void setFinancialImpact(BigDecimal financialImpact) { this.financialImpact = financialImpact; }

    public String getDismissalReason() { return dismissalReason; }
    public void setDismissalReason(String dismissalReason) { this.dismissalReason = dismissalReason; }

    public String getResolutionReason() { return resolutionReason; }
    public void setResolutionReason(String resolutionReason) { this.resolutionReason = resolutionReason; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
