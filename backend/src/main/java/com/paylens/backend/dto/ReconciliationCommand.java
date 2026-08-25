package com.paylens.backend.dto;

import com.paylens.backend.model.ExecutionProvider;
import java.math.BigDecimal;

public record ReconciliationCommand(
        String reconciliationId,
        String executionId,
        String decisionId,
        ExecutionProvider provider,
        String providerReference,
        String actionType,
        BigDecimal amount,
        String currency
) {}
