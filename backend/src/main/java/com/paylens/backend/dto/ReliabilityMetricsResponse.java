package com.paylens.backend.dto;

public record ReliabilityMetricsResponse(
        long totalExecutions,
        long confirmedSuccess,
        long confirmedFailure,
        long pending,
        long unknownOrManualReview,
        double successRate
) {}
