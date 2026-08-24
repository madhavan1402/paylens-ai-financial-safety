package com.paylens.backend.dto;

import java.util.List;

public record FinancialStateResponse(
        FinancialAccountResponse account,
        List<TransactionResponse> transactions,
        List<ObligationResponse> obligations,
        FinancialSummaryResponse summary) {
}
