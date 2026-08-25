package com.paylens.backend.dto;

import com.paylens.backend.model.ExecutionProvider;
import com.paylens.backend.model.ExecutionStatus;
import java.math.BigDecimal;
import java.time.Instant;

public record ExecutionResponse(
        String executionId,
        String decisionId,
        String idempotencyKey,
        ExecutionProvider provider,
        String providerReference,
        String actionType,
        BigDecimal amount,
        String currency,
        String target,
        ExecutionStatus status,
        String failureCode,
        String failureMessage,
        Instant createdAt,
        Instant updatedAt
) {}
