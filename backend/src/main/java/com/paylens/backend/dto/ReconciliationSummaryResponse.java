package com.paylens.backend.dto;

import com.paylens.backend.model.*;
import java.time.Instant;

public record ReconciliationSummaryResponse(
        String reconciliationId,
        String executionId,
        String decisionId,
        ExecutionProvider provider,
        String providerReference,
        ExecutionStatus resolvedExecutionStatus,
        ReconciliationStatus status,
        NormalizedReconciliationOutcome providerOutcome,
        RetryDecision retryDecision,
        Instant createdAt
) {}
