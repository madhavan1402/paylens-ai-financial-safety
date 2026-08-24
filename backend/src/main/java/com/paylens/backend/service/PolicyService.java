package com.paylens.backend.service;

import com.paylens.backend.dto.PolicyEvaluationResult;
import com.paylens.backend.dto.SimulationResult;
import com.paylens.backend.model.PolicyDecision;
import org.springframework.stereotype.Service;

/** Converts simulation facts into deterministic, ordered policy decisions. */
@Service
public class PolicyService {
    private static final String OBLIGATION_SHORTFALL_REASON =
            "Action would leave insufficient funds to cover upcoming obligations.";
    private static final String RESERVE_BREACH_REASON =
            "Action would breach the required safety reserve after upcoming obligations.";
    private static final String LOW_MARGIN_REASON =
            "Action remains financially possible but materially reduces the safety margin.";
    private static final String SAFE_REASON =
            "Action can be completed while preserving upcoming obligation coverage and the required safety margin.";
    private static final String BLOCK_RECOMMENDATION =
            "Reduce the amount or delay the action until additional funds are available.";
    private static final String REVIEW_RECOMMENDATION = "Request human approval before execution.";
    private static final String SAFE_RECOMMENDATION = "Action may proceed to the next execution stage.";

    private final PolicyThresholds policyThresholds;

    public PolicyService(PolicyThresholds policyThresholds) {
        this.policyThresholds = policyThresholds;
    }

    public PolicyEvaluationResult evaluate(SimulationResult simulation) {
        var after = simulation.after();
        if (after.remainingAfterObligations().signum() < 0) {
            return result(PolicyDecision.BLOCK, OBLIGATION_SHORTFALL_REASON, BLOCK_RECOMMENDATION, simulation);
        }
        if (after.safetyBuffer().signum() < 0) {
            return result(PolicyDecision.BLOCK, RESERVE_BREACH_REASON, BLOCK_RECOMMENDATION, simulation);
        }
        if (after.safetyBuffer().compareTo(policyThresholds.minimumSafetyMargin(after)) < 0) {
            return result(PolicyDecision.REVIEW, LOW_MARGIN_REASON, REVIEW_RECOMMENDATION, simulation);
        }
        return result(PolicyDecision.SAFE, SAFE_REASON, SAFE_RECOMMENDATION, simulation);
    }

    private PolicyEvaluationResult result(
            PolicyDecision decision, String reason, String recommendation, SimulationResult simulation) {
        return new PolicyEvaluationResult(decision, reason, recommendation, simulation);
    }
}
