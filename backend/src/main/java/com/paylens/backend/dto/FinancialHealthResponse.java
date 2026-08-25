package com.paylens.backend.dto;

import com.paylens.backend.model.FinancialHealthStatus;
import java.math.BigDecimal;
import java.time.Instant;

public record FinancialHealthResponse(
        BigDecimal currentBalance,
        BigDecimal availableLiquidity,
        BigDecimal unpaidObligations,
        BigDecimal safetyReserve,
        BigDecimal remainingAfterObligations,
        BigDecimal safetyBuffer,
        FinancialHealthStatus healthStatus,
        int healthScore,
        Instant calculatedAt
) {}
