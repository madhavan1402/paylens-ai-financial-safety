package com.paylens.backend.dto;
import com.paylens.backend.model.*; import java.math.BigDecimal; import java.time.Instant;
public record DecisionSummaryResponse(String decisionId,String actionType,BigDecimal amount,String currency,String target,PolicyDecision decision,GovernanceStatus status,Instant createdAt) {}
