package com.paylens.backend.dto;
import com.paylens.backend.model.GovernanceStatus;
public record GovernanceResponse(String decisionId, GovernanceStatus status) {}
