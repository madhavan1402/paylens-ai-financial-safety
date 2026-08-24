package com.paylens.backend.service;

import com.paylens.backend.dto.FinancialImpact;
import com.paylens.backend.dto.FinancialSnapshot;
import com.paylens.backend.dto.SimulationRequest;
import com.paylens.backend.dto.SimulationResult;
import com.paylens.backend.model.SimulationConsequence;
import java.math.BigDecimal;
import org.springframework.stereotype.Service;

/** Calculates hypothetical outgoing actions without changing the financial state. */
@Service
public class SimulationService {
    private final FinancialStateService financialStateService;

    public SimulationService(FinancialStateService financialStateService) {
        this.financialStateService = financialStateService;
    }

    public SimulationResult simulate(SimulationRequest request) {
        var dashboard = financialStateService.getDashboard();
        var before = new FinancialSnapshot(
                dashboard.currentBalance(), dashboard.upcomingObligations(), dashboard.safetyReserve(),
                dashboard.availableLiquidity(), dashboard.remainingAfterObligations(), dashboard.safetyBuffer());

        BigDecimal afterBalance = before.currentBalance().subtract(request.amount());
        BigDecimal afterLiquidity = afterBalance.subtract(before.safetyReserve());
        BigDecimal afterRemaining = afterBalance.subtract(before.upcomingObligations());
        BigDecimal afterSafetyBuffer = afterRemaining.subtract(before.safetyReserve());
        var after = new FinancialSnapshot(
                afterBalance, before.upcomingObligations(), before.safetyReserve(), afterLiquidity,
                afterRemaining, afterSafetyBuffer);

        boolean reserveBreached = after.safetyBuffer().signum() < 0;
        boolean obligationsCovered = after.currentBalance().compareTo(after.upcomingObligations()) >= 0;
        var impact = new FinancialImpact(
                after.availableLiquidity().subtract(before.availableLiquidity()),
                after.remainingAfterObligations().subtract(before.remainingAfterObligations()),
                after.safetyBuffer().subtract(before.safetyBuffer()),
                reserveBreached,
                obligationsCovered);

        return new SimulationResult(request, before, after, impact, consequenceFor(reserveBreached, obligationsCovered));
    }

    private SimulationConsequence consequenceFor(boolean reserveBreached, boolean obligationsCovered) {
        if (!obligationsCovered) {
            return SimulationConsequence.OBLIGATION_SHORTFALL;
        }
        return reserveBreached ? SimulationConsequence.RESERVE_BREACH : SimulationConsequence.NORMAL;
    }
}
