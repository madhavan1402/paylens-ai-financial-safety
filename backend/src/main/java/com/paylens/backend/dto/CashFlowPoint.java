package com.paylens.backend.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record CashFlowPoint(
        LocalDate date,
        BigDecimal inflow,
        BigDecimal outflow,
        BigDecimal netFlow,
        BigDecimal balance
) {}
