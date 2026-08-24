package com.paylens.backend.model;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record Transaction(
        String id,
        TransactionType type,
        BigDecimal amount,
        String description,
        OffsetDateTime timestamp,
        TransactionStatus status) {
}
