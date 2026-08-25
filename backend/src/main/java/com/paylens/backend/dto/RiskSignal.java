package com.paylens.backend.dto;

import com.paylens.backend.model.RiskSeverity;
import com.paylens.backend.model.RiskSignalType;
import java.time.Instant;

public record RiskSignal(
        String signalId,
        RiskSignalType type,
        RiskSeverity severity,
        String title,
        String description,
        Instant detectedAt,
        String relatedEntityId,
        String recommendedAction
) {}
