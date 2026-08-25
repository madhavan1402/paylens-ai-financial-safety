package com.paylens.backend.dto;

import java.time.Instant;

public record MonitoringStatusResponse(
        Instant lastRunAt,
        Instant nextRunAt,
        boolean monitoringEnabled,
        long lastRunDurationMs,
        String lastRunStatus,
        int eventsDetected,
        int eventsUpdated,
        int eventsResolved,
        long openCount,
        long acknowledgedCount,
        long resolvedCount,
        long dismissedCount
) {}
