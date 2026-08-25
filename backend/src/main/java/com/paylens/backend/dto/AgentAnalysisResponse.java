package com.paylens.backend.dto;

import java.util.List;

public record AgentAnalysisResponse(
        String message,
        String status,
        AgentFinancialIntent intent,
        List<String> missingFields,
        String clarificationMessage,
        SimulationResult simulation,
        PolicyEvaluationResult policy,
        ExplanationResponse explanation,
        GovernanceResponse governance) {
}
