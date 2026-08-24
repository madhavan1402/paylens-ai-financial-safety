package com.paylens.backend.dto;

import com.paylens.backend.model.TransactionStatus;
import com.paylens.backend.model.TransactionType;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

/** Public representation of a transaction in the fixed demo history. */
public record TransactionResponse(
        String id,
        TransactionType type,
        BigDecimal amount,
        String description,
        OffsetDateTime timestamp,
        TransactionStatus status) {
}
