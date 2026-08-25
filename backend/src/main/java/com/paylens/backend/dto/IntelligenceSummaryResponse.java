package com.paylens.backend.dto;

import java.time.Instant;
import java.util.List;

public record IntelligenceSummaryResponse(
        FinancialHealthResponse health,
        LiquidityForecastResponse forecast,
        List<ObligationRiskItem> topObligations,
        List<RiskSignal> activeSignals,
        RevenueAtRiskResponse revenueAtRisk,
        Instant calculatedAt
) {}
