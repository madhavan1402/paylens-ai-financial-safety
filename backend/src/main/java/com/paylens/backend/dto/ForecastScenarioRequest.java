package com.paylens.backend.dto;

import java.math.BigDecimal;

public record ForecastScenarioRequest(
        String actionType,
        BigDecimal amount,
        String currency,
        String target
) {}
