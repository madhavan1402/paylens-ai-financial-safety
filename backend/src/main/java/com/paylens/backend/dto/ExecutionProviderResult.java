package com.paylens.backend.dto;

import com.paylens.backend.model.ExecutionStatus;

public record ExecutionProviderResult(
        ExecutionStatus status,
        String providerReference,
        String failureCode,
        String failureMessage
) {
    public static ExecutionProviderResult success(String reference) {
        return new ExecutionProviderResult(ExecutionStatus.SUCCEEDED, reference, null, null);
    }

    public static ExecutionProviderResult failure(String code, String message) {
        return new ExecutionProviderResult(ExecutionStatus.FAILED, null, code, message);
    }

    public static ExecutionProviderResult unknown(String message) {
        return new ExecutionProviderResult(ExecutionStatus.UNKNOWN, null, "TIMEOUT_UNKNOWN", message);
    }

    public static ExecutionProviderResult unsupported(String message) {
        return new ExecutionProviderResult(ExecutionStatus.UNSUPPORTED_EXECUTION, null, "ACTION_NOT_SUPPORTED", message);
    }
}
