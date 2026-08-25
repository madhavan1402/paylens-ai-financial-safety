package com.paylens.backend.dto;

import com.paylens.backend.model.FinancialHealthStatus;
import com.paylens.backend.model.PolicyDecision;
import java.math.BigDecimal;

public record ForecastScenarioResponse(
        String actionType,
        BigDecimal amount,
        PolicyDecision policyDecision,
        BigDecimal currentSafetyBuffer,
        BigDecimal projectedSafetyBuffer,
        BigDecimal safetyBufferImpact,
        FinancialHealthStatus currentHealthStatus,
        FinancialHealthStatus projectedHealthStatus,
        String consequenceSummary,
        boolean preservesFullMargin
) {}
