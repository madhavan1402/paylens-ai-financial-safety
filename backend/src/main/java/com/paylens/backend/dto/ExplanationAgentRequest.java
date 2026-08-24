package com.paylens.backend.dto;

/** Structured, backend-authored facts sent to the explanation agent. */
public record ExplanationAgentRequest(
        String originalMessage,
        AgentFinancialIntent intent,
        SimulationResult simulation,
        ExplanationPolicyFacts policy) {
}
