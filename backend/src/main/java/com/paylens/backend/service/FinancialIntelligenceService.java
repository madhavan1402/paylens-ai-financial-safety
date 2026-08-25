package com.paylens.backend.service;

import com.paylens.backend.dto.*;
import com.paylens.backend.model.*;
import com.paylens.backend.repository.ExecutionRepository;
import com.paylens.backend.repository.FinancialStateRepository;
import com.paylens.backend.repository.ReconciliationRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class FinancialIntelligenceService {

    private final FinancialStateRepository financialStateRepository;
    private final SimulationService simulationService;
    private final ExecutionRepository executionRepository;
    private final ReconciliationRepository reconciliationRepository;

    public FinancialIntelligenceService(FinancialStateRepository financialStateRepository,
                                        SimulationService simulationService,
                                        ExecutionRepository executionRepository,
                                        ReconciliationRepository reconciliationRepository) {
        this.financialStateRepository = financialStateRepository;
        this.simulationService = simulationService;
        this.executionRepository = executionRepository;
        this.reconciliationRepository = reconciliationRepository;
    }

    public FinancialHealthResponse getFinancialHealth() {
        var account = financialStateRepository.getAccount();
        var obligations = financialStateRepository.getObligations();

        BigDecimal currentBalance = account.currentBalance();
        BigDecimal safetyReserve = account.safetyReserve();

        BigDecimal upcomingObligations = BigDecimal.ZERO;
        for (var obl : obligations) {
            if (obl.status() == ObligationStatus.UPCOMING || obl.status() == ObligationStatus.DUE) {
                upcomingObligations = upcomingObligations.add(obl.amount());
            }
        }

        BigDecimal availableLiquidity = currentBalance.subtract(safetyReserve);
        BigDecimal remainingAfterObligations = currentBalance.subtract(upcomingObligations);
        BigDecimal safetyBuffer = remainingAfterObligations.subtract(safetyReserve);

        // Deterministic Financial Health Classification
        FinancialHealthStatus healthStatus;
        if (availableLiquidity.compareTo(BigDecimal.ZERO) < 0) {
            healthStatus = FinancialHealthStatus.CRITICAL;
        } else if (safetyBuffer.compareTo(BigDecimal.ZERO) < 0) {
            healthStatus = FinancialHealthStatus.AT_RISK;
        } else if (safetyBuffer.compareTo(safetyReserve) < 0) {
            healthStatus = FinancialHealthStatus.CAUTION;
        } else {
            healthStatus = FinancialHealthStatus.HEALTHY;
        }

        // Deterministic Health Score (0-100)
        int score = 100;
        if (safetyBuffer.compareTo(BigDecimal.ZERO) < 0) {
            score -= 35;
        } else if (safetyBuffer.compareTo(safetyReserve) < 0) {
            score -= 15;
        }

        if (availableLiquidity.compareTo(upcomingObligations) < 0) {
            score -= 20;
        }

        long failedCount = executionRepository.countByStatus(ExecutionStatus.FAILED);
        long unknownCount = executionRepository.countByStatus(ExecutionStatus.UNKNOWN);
        int execPenalty = (int) Math.min(25, (failedCount * 10) + (unknownCount * 15));
        score -= execPenalty;

        score = Math.max(0, Math.min(100, score));

        return new FinancialHealthResponse(
                currentBalance, availableLiquidity, upcomingObligations, safetyReserve,
                remainingAfterObligations, safetyBuffer, healthStatus, score, Instant.now()
        );
    }

    public CashFlowResponse getCashFlow(String period) {
        var transactions = financialStateRepository.getTransactions();
        Map<LocalDate, List<Transaction>> grouped = transactions.stream()
                .collect(Collectors.groupingBy(t -> t.timestamp().toLocalDate()));

        List<CashFlowPoint> points = new ArrayList<>();
        BigDecimal runningBalance = new BigDecimal("500000"); // Baseline before seeded transactions

        List<LocalDate> sortedDates = new ArrayList<>(grouped.keySet());
        Collections.sort(sortedDates);

        for (LocalDate date : sortedDates) {
            List<Transaction> txs = grouped.get(date);
            BigDecimal inflow = BigDecimal.ZERO;
            BigDecimal outflow = BigDecimal.ZERO;

            for (Transaction t : txs) {
                if (t.type() == TransactionType.PAYMENT_IN) {
                    inflow = inflow.add(t.amount());
                } else {
                    outflow = outflow.add(t.amount());
                }
            }
            BigDecimal netFlow = inflow.subtract(outflow);
            runningBalance = runningBalance.add(netFlow);
            points.add(new CashFlowPoint(date, inflow, outflow, netFlow, runningBalance));
        }

        return new CashFlowResponse(
                period == null ? "daily" : period,
                points,
                "Calculated strictly from 4 persisted operational transaction records."
        );
    }

    public ObligationsRiskResponse getObligations() {
        var obligations = financialStateRepository.getObligations();
        var health = getFinancialHealth();
        LocalDate today = LocalDate.of(2026, 8, 25);

        BigDecimal totalUpcoming = BigDecimal.ZERO;
        List<ObligationRiskItem> items = new ArrayList<>();

        for (Obligation obl : obligations) {
            if (obl.status() == ObligationStatus.UPCOMING || obl.status() == ObligationStatus.DUE) {
                totalUpcoming = totalUpcoming.add(obl.amount());
            }

            long daysUntilDue = ChronoUnit.DAYS.between(today, obl.dueDate());

            ObligationRiskLevel riskLevel;
            if (daysUntilDue <= 3 && obl.amount().compareTo(health.availableLiquidity()) > 0) {
                riskLevel = ObligationRiskLevel.CRITICAL;
            } else if (daysUntilDue <= 3) {
                riskLevel = ObligationRiskLevel.HIGH;
            } else if (daysUntilDue <= 7) {
                riskLevel = ObligationRiskLevel.MEDIUM;
            } else {
                riskLevel = ObligationRiskLevel.LOW;
            }

            items.add(new ObligationRiskItem(
                    obl.id(), obl.type(), obl.description(), obl.amount(), obl.dueDate(),
                    obl.status(), daysUntilDue, riskLevel
            ));
        }

        return new ObligationsRiskResponse(items, totalUpcoming);
    }

    public LiquidityForecastResponse getForecast(int days) {
        int forecastPeriod = Math.max(1, Math.min(30, days == 0 ? 7 : days));
        var account = financialStateRepository.getAccount();
        var obligations = financialStateRepository.getObligations();

        LocalDate startDate = LocalDate.of(2026, 8, 25);
        BigDecimal runningBalance = account.currentBalance();
        BigDecimal safetyReserve = account.safetyReserve();

        BigDecimal totalProjectedOutflows = BigDecimal.ZERO;
        BigDecimal totalProjectedInflows = BigDecimal.ZERO;

        List<LiquidityForecastDay> forecastDaysList = new ArrayList<>();

        for (int i = 1; i <= forecastPeriod; i++) {
            LocalDate dayDate = startDate.plusDays(i);
            BigDecimal scheduledOutflows = BigDecimal.ZERO;

            for (Obligation obl : obligations) {
                if (obl.dueDate().equals(dayDate) && (obl.status() == ObligationStatus.UPCOMING || obl.status() == ObligationStatus.DUE)) {
                    scheduledOutflows = scheduledOutflows.add(obl.amount());
                }
            }

            totalProjectedOutflows = totalProjectedOutflows.add(scheduledOutflows);
            runningBalance = runningBalance.subtract(scheduledOutflows);

            // Compute remaining unpaid obligations after dayDate
            BigDecimal remainingObligations = BigDecimal.ZERO;
            for (Obligation obl : obligations) {
                if (obl.dueDate().isAfter(dayDate) && (obl.status() == ObligationStatus.UPCOMING || obl.status() == ObligationStatus.DUE)) {
                    remainingObligations = remainingObligations.add(obl.amount());
                }
            }

            BigDecimal projectedSafetyBuffer = runningBalance.subtract(remainingObligations).subtract(safetyReserve);

            forecastDaysList.add(new LiquidityForecastDay(
                    dayDate, i, runningBalance, projectedSafetyBuffer, scheduledOutflows, BigDecimal.ZERO
            ));
        }

        BigDecimal endingBalance = runningBalance;
        BigDecimal endingSafetyBuffer = forecastDaysList.get(forecastDaysList.size() - 1).projectedSafetyBuffer();

        List<String> assumptions = List.of(
                "Forecast incorporates scheduled upcoming merchant obligations (Payroll, Vendor Invoices, Taxes).",
                "Speculative uncommitted future revenue inflows are excluded.",
                "Safety reserve threshold is fixed at ₹1,00,000."
        );

        return new LiquidityForecastResponse(
                forecastPeriod, account.currentBalance(), totalProjectedInflows, totalProjectedOutflows,
                endingBalance, endingSafetyBuffer, ForecastConfidence.MEDIUM, ForecastDataQuality.LIMITED_HISTORY,
                assumptions, forecastDaysList
        );
    }

    public ForecastScenarioResponse simulateScenario(ForecastScenarioRequest request) {
        var healthBefore = getFinancialHealth();

        SimulationActionType actionEnum;
        try {
            actionEnum = SimulationActionType.valueOf(request.actionType().toUpperCase());
        } catch (Exception e) {
            actionEnum = SimulationActionType.REFUND;
        }
        var simulation = simulationService.simulate(new com.paylens.backend.dto.SimulationRequest(actionEnum, request.amount(), "Scenario Simulation"));

        BigDecimal currentSafetyBuffer = healthBefore.safetyBuffer();
        BigDecimal projectedSafetyBuffer = simulation.after().safetyBuffer();
        BigDecimal impact = projectedSafetyBuffer.subtract(currentSafetyBuffer);

        FinancialHealthStatus projectedStatus;
        if (simulation.after().availableLiquidity().compareTo(BigDecimal.ZERO) < 0) {
            projectedStatus = FinancialHealthStatus.CRITICAL;
        } else if (projectedSafetyBuffer.compareTo(BigDecimal.ZERO) < 0) {
            projectedStatus = FinancialHealthStatus.AT_RISK;
        } else if (projectedSafetyBuffer.compareTo(healthBefore.safetyReserve()) < 0) {
            projectedStatus = FinancialHealthStatus.CAUTION;
        } else {
            projectedStatus = FinancialHealthStatus.HEALTHY;
        }

        PolicyDecision policyDecision;
        if (simulation.impact().reserveBreached() || !simulation.impact().obligationsCovered()) {
            policyDecision = PolicyDecision.BLOCK;
        } else if (projectedSafetyBuffer.compareTo(healthBefore.safetyReserve()) < 0) {
            policyDecision = PolicyDecision.REVIEW;
        } else {
            policyDecision = PolicyDecision.SAFE;
        }

        boolean preservesFullMargin = projectedSafetyBuffer.compareTo(healthBefore.safetyReserve()) >= 0;

        return new ForecastScenarioResponse(
                request.actionType(), request.amount(), policyDecision, currentSafetyBuffer,
                projectedSafetyBuffer, impact, healthBefore.healthStatus(), projectedStatus,
                simulation.consequence().name(), preservesFullMargin
        );
    }

    public RiskSignalsResponse getRiskSignals() {
        List<RiskSignal> signals = new ArrayList<>();
        var health = getFinancialHealth();
        Instant now = Instant.now();

        // 1. Safety Buffer Pressure Signal
        if (health.safetyBuffer().compareTo(health.safetyReserve().multiply(new BigDecimal("1.5"))) < 0) {
            signals.add(new RiskSignal(
                    "sig_buffer_press", RiskSignalType.SAFETY_BUFFER_PRESSURE, RiskSeverity.HIGH,
                    "Safety Buffer Pressure Detected",
                    "Safety buffer (" + health.safetyBuffer() + " INR) is approaching the required ₹1,00,000 safety reserve threshold.",
                    now, "account-merchant-primary", "Review non-essential outgoing vendor payments before approving."
            ));
        }

        // 2. Upcoming Obligation Pressure Signal
        var obligations = getObligations();
        for (var obl : obligations.obligations()) {
            if (obl.daysUntilDue() <= 5 && (obl.status() == ObligationStatus.DUE || obl.status() == ObligationStatus.UPCOMING)) {
                signals.add(new RiskSignal(
                        "sig_obl_" + obl.id(), RiskSignalType.UPCOMING_OBLIGATION, RiskSeverity.MEDIUM,
                        "Approaching Obligation: " + obl.description(),
                        obl.description() + " of " + obl.amount() + " INR is due in " + obl.daysUntilDue() + " days.",
                        now, obl.id(), "Ensure available liquidity remains above reserve threshold."
                ));
            }
        }

        // 3. Execution Failures & Unknown Executions Signals
        List<ExecutionRecord> unknownExecs = executionRepository.findByStatusOrderByCreatedAtDesc(ExecutionStatus.UNKNOWN);
        if (!unknownExecs.isEmpty()) {
            signals.add(new RiskSignal(
                    "sig_exec_unknown", RiskSignalType.UNKNOWN_EXECUTION, RiskSeverity.HIGH,
                    "Uncertain Payment Executions Detected",
                    unknownExecs.size() + " payment execution(s) are in UNKNOWN outcome state due to network timeout.",
                    now, unknownExecs.get(0).getExecutionId(), "Trigger provider reconciliation to verify transaction status."
            ));
        }

        List<ExecutionRecord> failedExecs = executionRepository.findByStatusOrderByCreatedAtDesc(ExecutionStatus.FAILED);
        if (!failedExecs.isEmpty()) {
            signals.add(new RiskSignal(
                    "sig_exec_failed", RiskSignalType.EXECUTION_FAILURE, RiskSeverity.MEDIUM,
                    "Failed Payment Executions",
                    failedExecs.size() + " payment execution(s) failed at the provider gateway.",
                    now, failedExecs.get(0).getExecutionId(), "Inspect failure code and verify recipient details."
            ));
        }

        int criticalCount = (int) signals.stream().filter(s -> s.severity() == RiskSeverity.CRITICAL || s.severity() == RiskSeverity.HIGH).count();
        return new RiskSignalsResponse(signals, signals.size(), criticalCount);
    }

    public RevenueAtRiskResponse getRevenueAtRisk() {
        List<ExecutionRecord> failedExecs = executionRepository.findByStatusOrderByCreatedAtDesc(ExecutionStatus.FAILED);
        List<ExecutionRecord> unknownExecs = executionRepository.findByStatusOrderByCreatedAtDesc(ExecutionStatus.UNKNOWN);

        BigDecimal totalAmount = BigDecimal.ZERO;
        int caseCount = 0;
        BigDecimal highPriorityAmount = BigDecimal.ZERO;

        for (ExecutionRecord exec : failedExecs) {
            totalAmount = totalAmount.add(exec.getAmount());
            caseCount++;
            if (exec.getAmount().compareTo(new BigDecimal("50000")) >= 0) {
                highPriorityAmount = highPriorityAmount.add(exec.getAmount());
            }
        }

        for (ExecutionRecord exec : unknownExecs) {
            totalAmount = totalAmount.add(exec.getAmount());
            caseCount++;
            if (exec.getAmount().compareTo(new BigDecimal("50000")) >= 0) {
                highPriorityAmount = highPriorityAmount.add(exec.getAmount());
            }
        }

        String dataStatus = caseCount == 0 ? "NO_REVENUE_AT_RISK" : "CALCULATED_FROM_PERSISTED_RECORDS";
        return new RevenueAtRiskResponse(totalAmount, caseCount, highPriorityAmount, dataStatus, Instant.now());
    }

    public IntelligenceSummaryResponse getIntelligenceSummary() {
        var health = getFinancialHealth();
        var forecast = getForecast(7);
        var obligations = getObligations().obligations().stream().limit(3).toList();
        var signals = getRiskSignals().signals();
        var revenueAtRisk = getRevenueAtRisk();

        return new IntelligenceSummaryResponse(
                health, forecast, obligations, signals, revenueAtRisk, Instant.now()
        );
    }
}
