package com.paylens.backend.dto;

import com.paylens.backend.model.ObligationStatus;
import com.paylens.backend.model.ObligationType;
import java.math.BigDecimal;
import java.time.LocalDate;

/** Public representation of an upcoming or paid merchant obligation. */
public record ObligationResponse(
        String id,
        ObligationType type,
        String description,
        BigDecimal amount,
        LocalDate dueDate,
        ObligationStatus status) {
}
