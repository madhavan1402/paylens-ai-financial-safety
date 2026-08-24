package com.paylens.backend.dto;

import java.math.BigDecimal;

public record DashboardResponse(
        String currency,
        BigDecimal currentBalance,
        BigDecimal upcomingObligations,
        BigDecimal safetyReserve,
        BigDecimal availableLiquidity,
        BigDecimal remainingAfterObligations,
        BigDecimal safetyBuffer) {
}
