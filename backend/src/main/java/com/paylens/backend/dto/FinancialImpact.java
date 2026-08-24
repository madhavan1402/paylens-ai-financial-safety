package com.paylens.backend.dto;

import java.math.BigDecimal;

public record FinancialImpact(
        BigDecimal liquidityChange,
        BigDecimal obligationCoverageChange,
        BigDecimal safetyBufferChange,
        boolean reserveBreached,
        boolean obligationsCovered) {
}
