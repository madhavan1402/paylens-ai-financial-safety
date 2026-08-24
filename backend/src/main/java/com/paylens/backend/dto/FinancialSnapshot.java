package com.paylens.backend.dto;

import java.math.BigDecimal;

/** A point-in-time financial view used by the simulation response. */
public record FinancialSnapshot(
        BigDecimal currentBalance,
        BigDecimal upcomingObligations,
        BigDecimal safetyReserve,
        BigDecimal availableLiquidity,
        BigDecimal remainingAfterObligations,
        BigDecimal safetyBuffer) {
}
