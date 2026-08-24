package com.paylens.backend.dto;

import com.paylens.backend.model.SimulationActionType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record SimulationRequest(
        @NotNull(message = "actionType is required") SimulationActionType actionType,
        @NotNull(message = "amount is required")
        @DecimalMin(value = "0.00", inclusive = false, message = "amount must be greater than zero")
        BigDecimal amount,
        String description) {
}
