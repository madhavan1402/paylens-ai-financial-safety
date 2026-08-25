package com.paylens.backend.dto;
import com.paylens.backend.model.*; import java.math.BigDecimal; import java.time.Instant;
public record DecisionDetailResponse(String decisionId,String originalMessage,AgentFinancialIntent intent,SimulationResult simulation,PolicyEvaluationResult policy,ExplanationResponse explanation,GovernanceStatus status,Instant createdAt,Instant updatedAt) {}
