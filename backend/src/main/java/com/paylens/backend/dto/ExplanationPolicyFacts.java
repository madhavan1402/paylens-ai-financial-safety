package com.paylens.backend.dto;

import com.paylens.backend.model.PolicyDecision;

/** Policy facts intentionally limited to what the explanation layer needs. */
public record ExplanationPolicyFacts(PolicyDecision decision, String reason, String recommendation) {
}
