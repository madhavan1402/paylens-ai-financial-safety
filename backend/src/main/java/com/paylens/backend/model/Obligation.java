package com.paylens.backend.model;

import java.math.BigDecimal;
import java.time.LocalDate;

public record Obligation(
        String id,
        ObligationType type,
        String description,
        BigDecimal amount,
        LocalDate dueDate,
        ObligationStatus status) {
}
