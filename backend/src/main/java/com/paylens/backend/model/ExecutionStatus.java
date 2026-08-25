package com.paylens.backend.model;

public enum ExecutionStatus {
    REQUESTED,
    ELIGIBILITY_REJECTED,
    PROCESSING,
    SUCCEEDED,
    FAILED,
    DUPLICATE,
    UNKNOWN,
    UNSUPPORTED_EXECUTION
}
