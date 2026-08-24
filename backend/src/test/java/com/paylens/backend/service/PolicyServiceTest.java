package com.paylens.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.paylens.backend.dto.SimulationRequest;
import com.paylens.backend.model.PolicyDecision;
import com.paylens.backend.model.SimulationActionType;
import com.paylens.backend.repository.InMemoryFinancialStateRepository;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PolicyServiceTest {
    private FinancialStateService financialStateService;
    private SimulationService simulationService;
    private PolicyService policyService;

    @BeforeEach
    void setUp() {
        financialStateService = new FinancialStateService(new InMemoryFinancialStateRepository());
        simulationService = new SimulationService(financialStateService);
        policyService = new PolicyService(new PolicyThresholds());
    }

    @Test
    void approvesActionThatPreservesTheFullSafetyMargin() {
        var result = evaluate("20000");

        assertEquals(PolicyDecision.SAFE, result.decision());
        assertTrue(result.simulation().impact().obligationsCovered());
        assertEquals(0, result.simulation().after().safetyBuffer()
                .compareTo(result.simulation().after().safetyReserve()));
    }

    @Test
    void sendsLowPositiveSafetyBufferForReview() {
        var result = evaluate("50000");

        assertEquals(PolicyDecision.REVIEW, result.decision());
        assertTrue(result.simulation().after().safetyBuffer().signum() >= 0);
        assertTrue(result.simulation().after().safetyBuffer()
                .compareTo(result.simulation().after().safetyReserve()) < 0);
    }

    @Test
    void blocksObligationShortfallBeforeOtherBreaches() {
        var result = evaluate("250000");

        assertEquals(PolicyDecision.BLOCK, result.decision());
        assertEquals("Action would leave insufficient funds to cover upcoming obligations.", result.reason());
    }

    @Test
    void blocksReserveBreachWithoutObligationShortfall() {
        var result = evaluate("130000");

        assertEquals(PolicyDecision.BLOCK, result.decision());
        assertTrue(result.simulation().after().remainingAfterObligations().signum() >= 0);
        assertTrue(result.simulation().after().safetyBuffer().signum() < 0);
    }

    @Test
    void doesNotMutateFinancialStateDuringPolicyEvaluation() {
        var before = financialStateService.getDashboard();
        evaluate("250000");

        assertEquals(before, financialStateService.getDashboard());
    }

    private com.paylens.backend.dto.PolicyEvaluationResult evaluate(String amount) {
        var action = new SimulationRequest(SimulationActionType.REFUND, new BigDecimal(amount), "Policy test");
        return policyService.evaluate(simulationService.simulate(action));
    }
}
