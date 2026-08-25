package com.paylens.backend.dto;

import java.util.List;

public record RiskSignalsResponse(
        List<RiskSignal> signals,
        int totalCount,
        int criticalCount
) {}
