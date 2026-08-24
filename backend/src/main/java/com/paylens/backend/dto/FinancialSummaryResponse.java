package com.paylens.backend.dto;

import java.math.BigDecimal;

public record FinancialSummaryResponse(
        BigDecimal upcomingObligations,
        BigDecimal availableLiquidity,
        BigDecimal remainingAfterObligations,
        BigDecimal safetyBuffer) {
}
