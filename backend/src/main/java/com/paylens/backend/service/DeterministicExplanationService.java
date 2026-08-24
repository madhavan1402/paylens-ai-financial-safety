package com.paylens.backend.service;

import com.paylens.backend.dto.ExplanationAgentRequest;
import com.paylens.backend.dto.ExplanationResponse;
import com.paylens.backend.model.PolicyDecision;
import java.util.List;
import org.springframework.stereotype.Service;

/** Last-resort explanation, built solely from authoritative simulation and policy results. */
@Service
public class DeterministicExplanationService {
    public ExplanationResponse explain(ExplanationAgentRequest request) {
        var policy = request.policy();
        var after = request.simulation().after();
        var action = request.intent().actionType().replace('_', ' ').toLowerCase();
        if (policy.decision() == PolicyDecision.BLOCK) {
            return new ExplanationResponse("SUCCESS", policy.decision(), capitalize(action) + " blocked",
                    "The proposed " + action + " would leave insufficient funds to cover upcoming obligations.",
                    List.of(policy.reason(), "Simulated safety buffer: " + after.safetyBuffer() + "."),
                    policy.recommendation(), "deterministic-local");
        }
        if (policy.decision() == PolicyDecision.REVIEW) {
            return new ExplanationResponse("SUCCESS", policy.decision(), "Human review required",
                    "The proposed action is financially possible, but it would materially reduce the configured safety margin.",
                    List.of(policy.reason(), "Simulated safety buffer: " + after.safetyBuffer() + "."),
                    policy.recommendation(), "deterministic-local");
        }
        return new ExplanationResponse("SUCCESS", policy.decision(), "Action appears financially safe",
                "The proposed action preserves upcoming obligation coverage and the required safety margin.",
                List.of(policy.reason(), "Upcoming obligations remain covered."), policy.recommendation(), "deterministic-local");
    }

    private String capitalize(String value) {
        return Character.toUpperCase(value.charAt(0)) + value.substring(1);
    }
}
