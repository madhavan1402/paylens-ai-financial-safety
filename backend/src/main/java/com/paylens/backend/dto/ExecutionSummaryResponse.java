package com.paylens.backend.dto;

import com.paylens.backend.model.ExecutionProvider;
import com.paylens.backend.model.ExecutionStatus;
import java.math.BigDecimal;
import java.time.Instant;

public record ExecutionSummaryResponse(
        String executionId,
        String decisionId,
        String actionType,
        BigDecimal amount,
        String currency,
        ExecutionProvider provider,
        String providerReference,
        ExecutionStatus status,
        Instant createdAt
) {}
