package com.paylens.backend.dto;

import com.paylens.backend.model.PolicyDecision;

public record PolicyEvaluationResult(
        PolicyDecision decision,
        String reason,
        String recommendation,
        SimulationResult simulation) {
}
