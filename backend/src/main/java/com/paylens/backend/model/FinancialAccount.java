package com.paylens.backend.model;

import java.math.BigDecimal;

public record FinancialAccount(
        String id,
        String accountName,
        String currency,
        BigDecimal currentBalance,
        BigDecimal safetyReserve) {
}
