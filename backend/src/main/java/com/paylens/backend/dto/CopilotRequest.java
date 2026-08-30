package com.paylens.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request body for a single Copilot query.
 * The merchant sends a natural-language message and an optional merchantId context.
 * The copilot will NOT execute financial transactions from this request.
 */
public record CopilotRequest(
        @NotBlank(message = "Query message is required")
        @Size(max = 1000, message = "Query must not exceed 1000 characters")
        String message,

        /** Optional action type hint for scenario analysis (e.g. "PAYOUT", "REFUND"). May be null. */
        String actionType,

        /** Optional amount hint for scenario analysis. May be null. */
        java.math.BigDecimal amount
) {}
