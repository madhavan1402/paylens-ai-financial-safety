package com.paylens.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.paylens.backend.dto.SimulationRequest;
import com.paylens.backend.dto.SimulationResult;
import com.paylens.backend.model.SimulationActionType;
import com.paylens.backend.model.SimulationConsequence;
import com.paylens.backend.repository.InMemoryFinancialStateRepository;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SimulationServiceTest {
    private FinancialStateService financialStateService;
    private SimulationService simulationService;

    @BeforeEach
    void setUp() {
        financialStateService = new FinancialStateService(new InMemoryFinancialStateRepository());
        simulationService = new SimulationService(financialStateService);
    }

    @Test
    void simulatesFiftyThousandRupeeRefund() {
        var result = simulate(SimulationActionType.REFUND, "50000");

        assertMoney("840000", result.before().currentBalance());
        assertMoney("790000", result.after().currentBalance());
        assertMoney("70000", result.after().safetyBuffer());
        assertTrue(result.impact().obligationsCovered());
        assertFalse(result.impact().reserveBreached());
        assertEquals(SimulationConsequence.NORMAL, result.consequence());
    }

    @Test
    void identifiesObligationShortfallForLargeRefund() {
        var result = simulate(SimulationActionType.REFUND, "250000");

        assertMoney("590000", result.after().currentBalance());
        assertMoney("-30000", result.after().remainingAfterObligations());
        assertMoney("-130000", result.after().safetyBuffer());
        assertMoney("-250000", result.impact().liquidityChange());
        assertMoney("-250000", result.impact().obligationCoverageChange());
        assertTrue(result.impact().reserveBreached());
        assertFalse(result.impact().obligationsCovered());
        assertEquals(SimulationConsequence.OBLIGATION_SHORTFALL, result.consequence());
    }

    @Test
    void simulatesVendorPaymentWithTheSameDeterministicMath() {
        var result = simulate(SimulationActionType.VENDOR_PAYMENT, "100000");

        assertMoney("740000", result.after().currentBalance());
        assertMoney("20000", result.after().safetyBuffer());
        assertMoney("-100000", result.impact().safetyBufferChange());
    }

    @Test
    void leavesTheAuthoritativeFinancialStateUnchanged() {
        var before = financialStateService.getDashboard();
        simulationService.simulate(new SimulationRequest(SimulationActionType.TAX_PAYMENT, new BigDecimal("250000"), "GST"));

        assertEquals(before, financialStateService.getDashboard());
    }

    private SimulationResult simulate(SimulationActionType actionType, String amount) {
        return simulationService.simulate(new SimulationRequest(actionType, new BigDecimal(amount), "Demo action"));
    }

    private void assertMoney(String expected, BigDecimal actual) {
        assertEquals(0, new BigDecimal(expected).compareTo(actual));
    }
}
