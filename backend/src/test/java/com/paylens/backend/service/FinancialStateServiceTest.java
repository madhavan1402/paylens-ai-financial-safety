package com.paylens.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.paylens.backend.repository.InMemoryFinancialStateRepository;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class FinancialStateServiceTest {
    private FinancialStateService financialStateService;

    @BeforeEach
    void setUp() {
        financialStateService = new FinancialStateService(new InMemoryFinancialStateRepository());
    }

    @Test
    void calculatesDeterministicMerchantFinancialMetrics() {
        var dashboard = financialStateService.getDashboard();

        assertMoney("840000", dashboard.currentBalance());
        assertMoney("620000", dashboard.upcomingObligations());
        assertMoney("100000", dashboard.safetyReserve());
        assertMoney("740000", dashboard.availableLiquidity());
        assertMoney("220000", dashboard.remainingAfterObligations());
        assertMoney("120000", dashboard.safetyBuffer());
    }

    @Test
    void returnsTheSameSeededStateForEveryRead() {
        assertEquals(financialStateService.getFinancialState(), financialStateService.getFinancialState());
    }

    private void assertMoney(String expected, BigDecimal actual) {
        assertEquals(0, new BigDecimal(expected).compareTo(actual));
    }
}
