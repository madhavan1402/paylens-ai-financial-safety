package com.paylens.backend.dto;

import java.math.BigDecimal;

public record ExecutionCommand(
        String executionId,
        String decisionId,
        String actionType,
        BigDecimal amount,
        String currency,
        String target,
        String description
) {}
