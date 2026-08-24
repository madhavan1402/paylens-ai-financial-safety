package com.paylens.backend.dto;

import java.math.BigDecimal;

/** Public representation of the merchant's financial account. */
public record FinancialAccountResponse(
        String id,
        String accountName,
        String currency,
        BigDecimal currentBalance,
        BigDecimal safetyReserve) {
}
