package com.paylens.backend.dto;

import com.paylens.backend.model.*;
import java.math.BigDecimal;
import java.time.Instant;

public record RiskEventResponse(
        String riskEventId,
        String fingerprint,
        RiskSignalType riskSignalType,
        RiskSeverity severity,
        RiskPriority priority,
        String title,
        String description,
        RiskEventStatus status,
        RiskEventSource source,
        Instant detectedAt,
        Instant firstDetectedAt,
        Instant lastDetectedAt,
        Instant resolvedAt,
        int occurrenceCount,
        String relatedEntityType,
        String relatedEntityId,
        String recommendedAction,
        BigDecimal financialImpact,
        String dismissalReason,
        String resolutionReason,
        Instant createdAt,
        Instant updatedAt
) {
    public static RiskEventResponse from(RiskEvent event) {
        return new RiskEventResponse(
                event.getRiskEventId(),
                event.getFingerprint(),
                event.getRiskSignalType(),
                event.getSeverity(),
                event.getPriority(),
                event.getTitle(),
                event.getDescription(),
                event.getStatus(),
                event.getSource(),
                event.getDetectedAt(),
                event.getFirstDetectedAt(),
                event.getLastDetectedAt(),
                event.getResolvedAt(),
                event.getOccurrenceCount(),
                event.getRelatedEntityType(),
                event.getRelatedEntityId(),
                event.getRecommendedAction(),
                event.getFinancialImpact(),
                event.getDismissalReason(),
                event.getResolutionReason(),
                event.getCreatedAt(),
                event.getUpdatedAt()
        );
    }
}
