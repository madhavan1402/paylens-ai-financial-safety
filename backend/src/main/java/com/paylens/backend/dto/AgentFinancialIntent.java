package com.paylens.backend.dto;

import java.math.BigDecimal;

/** Raw intent received from the isolated Python agent; it is validated before use. */
public record AgentFinancialIntent(
        String actionType,
        BigDecimal amount,
        String currency,
        String target,
        String description) {
}
