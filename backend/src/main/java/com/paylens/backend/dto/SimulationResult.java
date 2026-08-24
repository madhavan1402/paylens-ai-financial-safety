package com.paylens.backend.dto;

import com.paylens.backend.model.SimulationConsequence;

public record SimulationResult(
        SimulationRequest action,
        FinancialSnapshot before,
        FinancialSnapshot after,
        FinancialImpact impact,
        SimulationConsequence consequence) {
}
