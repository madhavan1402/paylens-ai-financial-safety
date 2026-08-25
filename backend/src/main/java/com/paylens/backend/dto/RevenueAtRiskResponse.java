package com.paylens.backend.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record RevenueAtRiskResponse(
        BigDecimal totalAmount,
        int caseCount,
        BigDecimal highPriorityAmount,
        String dataStatus,
        Instant calculatedAt
) {}
