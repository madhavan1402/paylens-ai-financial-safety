package com.paylens.backend.model;

public enum ReconciliationStatus {
    NOT_REQUIRED,
    PENDING,
    IN_PROGRESS,
    CONFIRMED,
    FAILED,
    UNKNOWN,
    MANUAL_REVIEW_REQUIRED
}
