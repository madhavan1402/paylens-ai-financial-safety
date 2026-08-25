package com.paylens.backend.dto;

import com.paylens.backend.model.NormalizedReconciliationOutcome;

public record ReconciliationProviderResult(
        NormalizedReconciliationOutcome outcome,
        String rawProviderStatus,
        String failureCode,
        String failureMessage
) {
    public static ReconciliationProviderResult confirmedSuccess(String rawStatus) {
        return new ReconciliationProviderResult(NormalizedReconciliationOutcome.CONFIRMED_SUCCESS, rawStatus, null, null);
    }

    public static ReconciliationProviderResult confirmedFailure(String rawStatus, String code, String message) {
        return new ReconciliationProviderResult(NormalizedReconciliationOutcome.CONFIRMED_FAILURE, rawStatus, code, message);
    }

    public static ReconciliationProviderResult stillProcessing(String rawStatus) {
        return new ReconciliationProviderResult(NormalizedReconciliationOutcome.STILL_PROCESSING, rawStatus, null, null);
    }

    public static ReconciliationProviderResult notFound(String message) {
        return new ReconciliationProviderResult(NormalizedReconciliationOutcome.NOT_FOUND, "NOT_FOUND", "PROVIDER_REF_NOT_FOUND", message);
    }

    public static ReconciliationProviderResult unknown(String code, String message) {
        return new ReconciliationProviderResult(NormalizedReconciliationOutcome.UNKNOWN, "UNKNOWN", code, message);
    }
}
