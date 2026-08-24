package com.paylens.backend.dto;

import java.math.BigDecimal;
import java.util.List;

public record IntentAgentResponse(
        String status,
        AgentFinancialIntent intent,
        BigDecimal confidence,
        List<String> missingFields,
        String message,
        String providerMode) {
}
