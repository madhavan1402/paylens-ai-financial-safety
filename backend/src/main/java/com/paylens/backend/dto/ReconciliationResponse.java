package com.paylens.backend.dto;

import com.paylens.backend.model.*;
import java.time.Instant;

public record ReconciliationResponse(
        String reconciliationId,
        String executionId,
        String decisionId,
        ExecutionProvider provider,
        String providerReference,
        ExecutionStatus previousExecutionStatus,
        ExecutionStatus resolvedExecutionStatus,
        ReconciliationStatus status,
        NormalizedReconciliationOutcome providerOutcome,
        RetryDecision retryDecision,
        String resolution,
        String failureCode,
        String failureMessage,
        int attemptNumber,
        Instant createdAt,
        Instant updatedAt,
        Instant resolvedAt
) {}
