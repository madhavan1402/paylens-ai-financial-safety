package com.paylens.backend.dto;

import java.time.Instant;

public record MonitoringCycleResponse(
        String cycleId,
        Instant executedAt,
        long durationMs,
        int eventsDetected,
        int eventsUpdated,
        int eventsResolved,
        String status
) {}
