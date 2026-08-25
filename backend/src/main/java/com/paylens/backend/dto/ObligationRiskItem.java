package com.paylens.backend.dto;

import com.paylens.backend.model.ObligationRiskLevel;
import com.paylens.backend.model.ObligationStatus;
import com.paylens.backend.model.ObligationType;
import java.math.BigDecimal;
import java.time.LocalDate;

public record ObligationRiskItem(
        String id,
        ObligationType type,
        String description,
        BigDecimal amount,
        LocalDate dueDate,
        ObligationStatus status,
        long daysUntilDue,
        ObligationRiskLevel riskLevel
) {}
