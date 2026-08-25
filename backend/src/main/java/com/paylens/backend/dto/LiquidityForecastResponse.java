package com.paylens.backend.dto;

import com.paylens.backend.model.ForecastConfidence;
import com.paylens.backend.model.ForecastDataQuality;
import java.math.BigDecimal;
import java.util.List;

public record LiquidityForecastResponse(
        int forecastDays,
        BigDecimal startingBalance,
        BigDecimal projectedInflows,
        BigDecimal projectedOutflows,
        BigDecimal projectedEndingBalance,
        BigDecimal projectedSafetyBuffer,
        ForecastConfidence confidence,
        ForecastDataQuality dataQuality,
        List<String> assumptions,
        List<LiquidityForecastDay> forecastDaysList
) {}
