package com.paylens.backend.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record LiquidityForecastDay(
        LocalDate date,
        int dayIndex,
        BigDecimal projectedBalance,
        BigDecimal projectedSafetyBuffer,
        BigDecimal scheduledOutflows,
        BigDecimal projectedInflows
) {}
