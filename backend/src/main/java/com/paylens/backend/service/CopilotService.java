package com.paylens.backend.service;

import com.paylens.backend.dto.*;
import com.paylens.backend.model.*;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Phase 13 — AI Fintech Copilot orchestration service.
 *
 * <p>The Copilot answers merchant questions proactively by composing deterministic facts
 * from existing services. It NEVER executes or authorises financial transactions.
 * The deterministic Policy Engine always wins over any AI-generated content.
 *
 * <p>Intent routing rules (order matters):
 * <ol>
 *   <li>ACTION_ANALYSIS — message contains an action keyword (refund / pay / payout / vendor) and an amount hint
 *   <li>RISK_EXPLANATION — message contains "risk" / "alert" / "monitoring"
 *   <li>FORECAST_QUERY  — message contains "forecast" / "future" / "upcoming" / "liquidity"
 *   <li>POLICY_EXPLANATION — message contains "policy" / "block" / "safe" / "threshold"
 *   <li>FINANCIAL_STATUS — default / "balance" / "health" / "status" / "how are"
 *   <li>UNKNOWN          — fallback
 * </ol>
 */
@Service
public class CopilotService {

    private final FinancialIntelligenceService intelligenceService;
    private final SimulationService simulationService;
    private final PolicyService policyService;
    private final RiskMonitoringService riskMonitoringService;
    private final AuditService auditService;

    public CopilotService(
            FinancialIntelligenceService intelligenceService,
            SimulationService simulationService,
            PolicyService policyService,
            RiskMonitoringService riskMonitoringService,
            AuditService auditService) {
        this.intelligenceService = intelligenceService;
        this.simulationService = simulationService;
        this.policyService = policyService;
        this.riskMonitoringService = riskMonitoringService;
        this.auditService = auditService;
    }

    /**
     * Main entry point. Classifies the merchant's natural-language query and returns a
     * structured {@link CopilotResponse} built entirely from deterministic facts.
     */
    public CopilotResponse query(CopilotRequest request) {
        CopilotIntent intent = classifyIntent(request);
        CopilotResponse response = switch (intent) {
            case FINANCIAL_STATUS   -> handleFinancialStatus();
            case ACTION_ANALYSIS    -> handleActionAnalysis(request);
            case RISK_EXPLANATION   -> handleRiskExplanation();
            case FORECAST_QUERY     -> handleForecastQuery();
            case POLICY_EXPLANATION -> handlePolicyExplanation(request);
            case UNKNOWN            -> handleUnknown(request.message());
        };

        // Audit every copilot query — never silently bypass the audit trail.
        auditService.record(
                "copilot-" + java.util.UUID.randomUUID().toString().substring(0, 8),
                AuditEventType.COPILOT_QUERY,
                ActorType.AI_AGENT,
                "copilot",
                "Copilot query: intent=" + intent + " | message=" + truncate(request.message(), 120)
        );

        return response;
    }

    // =========================================================================
    // Intent classification
    // =========================================================================

    private CopilotIntent classifyIntent(CopilotRequest request) {
        String lower = request.message() == null ? "" : request.message().toLowerCase();

        // ACTION_ANALYSIS: explicit action + amount hint or actionType provided
        boolean hasActionType = request.actionType() != null && !request.actionType().isBlank();
        boolean hasAmount     = request.amount() != null && request.amount().compareTo(BigDecimal.ZERO) > 0;
        boolean hasActionWord = lower.contains("refund") || lower.contains("payout") || lower.contains("pay ")
                             || lower.contains("vendor") || lower.contains("payroll") || lower.contains("salary");
        if (hasActionType || (hasActionWord && hasAmount)) {
            return CopilotIntent.ACTION_ANALYSIS;
        }

        // RISK_EXPLANATION
        if (lower.contains("risk") || lower.contains("alert") || lower.contains("monitoring")
                || lower.contains("danger") || lower.contains("problem") || lower.contains("warning")) {
            return CopilotIntent.RISK_EXPLANATION;
        }

        // FORECAST_QUERY
        if (lower.contains("forecast") || lower.contains("future") || lower.contains("upcoming")
                || lower.contains("liquidity") || lower.contains("next") || lower.contains("project")) {
            return CopilotIntent.FORECAST_QUERY;
        }

        // POLICY_EXPLANATION
        if (lower.contains("policy") || lower.contains("block") || lower.contains("safe")
                || lower.contains("threshold") || lower.contains("why") || lower.contains("rule")) {
            return CopilotIntent.POLICY_EXPLANATION;
        }

        // FINANCIAL_STATUS (default for status-type queries)
        if (lower.contains("balance") || lower.contains("health") || lower.contains("status")
                || lower.contains("how") || lower.contains("current") || lower.contains("overview")) {
            return CopilotIntent.FINANCIAL_STATUS;
        }

        return CopilotIntent.UNKNOWN;
    }

    // =========================================================================
    // Intent handlers
    // =========================================================================

    private CopilotResponse handleFinancialStatus() {
        var health   = intelligenceService.getFinancialHealth();
        var signals  = intelligenceService.getRiskSignals();
        var summary  = intelligenceService.getIntelligenceSummary();

        List<String> keyFactors = new ArrayList<>();
        keyFactors.add("Current balance: ₹" + health.currentBalance());
        keyFactors.add("Available liquidity: ₹" + health.availableLiquidity());
        keyFactors.add("Upcoming obligations: ₹" + health.unpaidObligations());
        keyFactors.add("Safety buffer: ₹" + health.safetyBuffer());
        keyFactors.add("Health score: " + health.healthScore() + "/100");
        if (signals.totalCount() > 0) {
            keyFactors.add("Active risk signals: " + signals.totalCount() + " (" + signals.criticalCount() + " critical/high)");
        }

        String headline = buildHealthHeadline(health);
        String explanation = buildHealthExplanation(health, signals);
        String impact = "Safety buffer stands at ₹" + health.safetyBuffer() +
                ". " + (health.healthScore() >= 70 ? "Financial position is stable." :
                        health.healthScore() >= 40 ? "Moderate financial pressure detected." :
                        "Critical financial stress — immediate action required.");
        String recommendation = buildHealthRecommendation(health);
        boolean requiresReview = health.healthStatus() == FinancialHealthStatus.CRITICAL
                || health.healthStatus() == FinancialHealthStatus.AT_RISK;

        return new CopilotResponse(
                CopilotIntent.FINANCIAL_STATUS,
                headline, explanation, keyFactors,
                impact, recommendation, requiresReview,
                null, null, health, Instant.now()
        );
    }

    private CopilotResponse handleActionAnalysis(CopilotRequest request) {
        var health = intelligenceService.getFinancialHealth();

        // Resolve action type — default to PAYOUT if ambiguous
        SimulationActionType actionType;
        try {
            String at = request.actionType() != null ? request.actionType().toUpperCase() : inferActionType(request.message());
            actionType = SimulationActionType.valueOf(at);
        } catch (Exception e) {
            actionType = SimulationActionType.VENDOR_PAYMENT;
        }

        // Resolve amount — if no amount can be determined, fall back to financial status
        BigDecimal amount = request.amount() != null && request.amount().compareTo(BigDecimal.ZERO) > 0
                ? request.amount()
                : extractAmountFromMessage(request.message());

        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            // Cannot simulate without a valid amount — return financial status instead
            return handleFinancialStatus();
        }

        SimulationRequest simReq = new SimulationRequest(actionType, amount,
                "Copilot scenario: " + truncate(request.message(), 80));
        SimulationResult simulation = simulationService.simulate(simReq);
        PolicyEvaluationResult policy = policyService.evaluate(simulation);

        List<String> keyFactors = new ArrayList<>();
        keyFactors.add("Proposed amount: ₹" + amount);
        keyFactors.add("Action type: " + actionType.name());
        keyFactors.add("Balance before: ₹" + simulation.before().currentBalance());
        keyFactors.add("Balance after: ₹" + simulation.after().currentBalance());
        keyFactors.add("Safety buffer after: ₹" + simulation.after().safetyBuffer());
        keyFactors.add("Policy decision: " + policy.decision().name());
        keyFactors.add("Obligations covered: " + (!simulation.impact().obligationsCovered() ? "NO ⚠" : "YES ✓"));

        String headline = switch (policy.decision()) {
            case SAFE   -> "✅ Action is financially safe to proceed.";
            case REVIEW -> "⚠️ Action requires human review before execution.";
            case BLOCK  -> "🚫 Action is BLOCKED by the PayLens Policy Engine.";
        };

        String explanation = policy.reason() + " " + buildImpactNarrative(simulation, policy);
        String impact = "This action would change the safety buffer from ₹"
                + simulation.before().safetyBuffer() + " to ₹" + simulation.after().safetyBuffer()
                + " — a change of ₹" + simulation.impact().safetyBufferChange() + ".";

        boolean requiresReview = policy.decision() == PolicyDecision.REVIEW
                || policy.decision() == PolicyDecision.BLOCK;

        return new CopilotResponse(
                CopilotIntent.ACTION_ANALYSIS,
                headline, explanation, keyFactors,
                impact, policy.recommendation(), requiresReview,
                policy.decision().name(), simulation, health, Instant.now()
        );
    }

    private CopilotResponse handleRiskExplanation() {
        var health   = intelligenceService.getFinancialHealth();
        var signals  = intelligenceService.getRiskSignals();
        var revenueAtRisk = intelligenceService.getRevenueAtRisk();

        List<String> keyFactors = new ArrayList<>();
        for (var signal : signals.signals()) {
            keyFactors.add("[" + signal.severity() + "] " + signal.title() + ": " + signal.description());
        }
        if (keyFactors.isEmpty()) {
            keyFactors.add("No active risk signals detected at this time.");
        }
        keyFactors.add("Revenue at risk: ₹" + revenueAtRisk.totalAmount() + " across " + revenueAtRisk.caseCount() + " case(s).");

        String headline;
        if (signals.criticalCount() > 0) {
            headline = "🔴 " + signals.criticalCount() + " critical/high risk signal(s) require immediate attention.";
        } else if (signals.totalCount() > 0) {
            headline = "⚠️ " + signals.totalCount() + " active risk signal(s) detected — monitoring recommended.";
        } else {
            headline = "✅ No active risk signals. Financial position is stable.";
        }

        String explanation = signals.totalCount() == 0
                ? "PayLens has completed the latest monitoring cycle and found no new risk signals. " +
                  "Your safety buffer (₹" + health.safetyBuffer() + ") remains above the required reserve."
                : "PayLens Risk Monitoring has detected " + signals.totalCount() + " signal(s). " +
                  "The highest severity level is " +
                  signals.signals().stream().map(s -> s.severity().name()).findFirst().orElse("UNKNOWN") + ". " +
                  "Revenue at risk totals ₹" + revenueAtRisk.totalAmount() + ".";

        String impact = "₹" + revenueAtRisk.totalAmount() + " is potentially at risk across "
                + revenueAtRisk.caseCount() + " case(s).";

        String recommendation = signals.criticalCount() > 0
                ? "Immediate action required: review high-severity risk events in the Risk Center and trigger reconciliation for unknown executions."
                : signals.totalCount() > 0
                    ? "Monitor risk events closely and ensure upcoming obligations can be covered. No immediate action required."
                    : "No action required. Continue monitoring via the Risk Center.";

        return new CopilotResponse(
                CopilotIntent.RISK_EXPLANATION,
                headline, explanation, keyFactors,
                impact, recommendation,
                signals.criticalCount() > 0,
                null, null, health, Instant.now()
        );
    }

    private CopilotResponse handleForecastQuery() {
        var health   = intelligenceService.getFinancialHealth();
        var forecast = intelligenceService.getForecast(7);

        List<String> keyFactors = new ArrayList<>();
        keyFactors.add("Forecast period: " + forecast.forecastDays() + " days");
        keyFactors.add("Starting balance: ₹" + forecast.startingBalance());
        keyFactors.add("Projected outflows: ₹" + forecast.projectedOutflows());
        keyFactors.add("Projected ending balance: ₹" + forecast.projectedEndingBalance());
        keyFactors.add("Ending safety buffer: ₹" + forecast.projectedSafetyBuffer());
        keyFactors.add("Forecast confidence: " + forecast.confidence().name());

        boolean bufferTight = forecast.projectedSafetyBuffer().compareTo(BigDecimal.ZERO) < 0;

        String headline = bufferTight
                ? "⚠️ Forecast shows safety buffer will turn negative within " + forecast.forecastDays() + " days."
                : "📊 Liquidity forecast is stable over the next " + forecast.forecastDays() + " days.";

        String explanation = "Over the next " + forecast.forecastDays() + " days, projected outflows of ₹"
                + forecast.projectedOutflows() + " will reduce the balance to ₹" + forecast.projectedEndingBalance()
                + ". The ending safety buffer is projected at ₹" + forecast.projectedSafetyBuffer() + "."
                + (bufferTight ? " This is BELOW the required safety reserve — PayLens recommends deferring non-critical payments." : "");

        String impact = "Projected outflows of ₹" + forecast.projectedOutflows()
                + " will reduce the safety buffer to ₹" + forecast.projectedSafetyBuffer() + ".";

        String recommendation = bufferTight
                ? "Defer non-essential outgoing payments. Review upcoming obligation schedule and secure additional funds before the buffer is depleted."
                : "No immediate action required. Continue monitoring the obligation schedule.";

        return new CopilotResponse(
                CopilotIntent.FORECAST_QUERY,
                headline, explanation, keyFactors,
                impact, recommendation, bufferTight,
                null, null, health, Instant.now()
        );
    }

    private CopilotResponse handlePolicyExplanation(CopilotRequest request) {
        var health = intelligenceService.getFinancialHealth();

        // If an action was provided, simulate and explain the policy decision
        if (request.actionType() != null || request.amount() != null) {
            return handleActionAnalysis(request);
        }

        List<String> keyFactors = new ArrayList<>();
        keyFactors.add("PayLens operates a 3-tier deterministic policy: SAFE → REVIEW → BLOCK.");
        keyFactors.add("SAFE: action preserves obligations and safety reserve — proceeds to execution.");
        keyFactors.add("REVIEW: safety margin would be reduced — requires human approval.");
        keyFactors.add("BLOCK: obligations or safety reserve would be breached — execution is prevented.");
        keyFactors.add("Current safety reserve threshold: ₹1,00,000.");
        keyFactors.add("Current safety buffer: ₹" + health.safetyBuffer() + ".");
        keyFactors.add("The Policy Engine is deterministic — AI cannot override its decisions.");

        String headline = "📋 PayLens Policy Engine: Three-tier deterministic financial safety system.";
        String explanation = "The PayLens Policy Engine evaluates every proposed action against your current financial state. " +
                "It is deterministic and cannot be overridden by AI. " +
                "A BLOCK decision means the action would leave insufficient funds to cover upcoming obligations " +
                "or breach the required ₹1,00,000 safety reserve. " +
                "A REVIEW decision means the action is financially possible but would materially reduce the safety margin, " +
                "requiring human governance approval. " +
                "SAFE means the action fully preserves all constraints.";
        String impact = "Current safety buffer is ₹" + health.safetyBuffer()
                + ". Actions consuming more than ₹" + health.safetyBuffer()
                + " may trigger a REVIEW or BLOCK decision.";
        String recommendation = "Use the Scenario Simulator to test proposed actions before requesting execution.";

        return new CopilotResponse(
                CopilotIntent.POLICY_EXPLANATION,
                headline, explanation, keyFactors,
                impact, recommendation, false,
                null, null, health, Instant.now()
        );
    }

    private CopilotResponse handleUnknown(String message) {
        var health = intelligenceService.getFinancialHealth();
        List<String> keyFactors = List.of(
                "Try asking: 'What is my current financial status?'",
                "Try asking: 'Should I refund ₹50,000 to a customer?'",
                "Try asking: 'What are the active risk signals?'",
                "Try asking: 'What will my liquidity look like next week?'",
                "Try asking: 'Why was my payment blocked?'"
        );
        return new CopilotResponse(
                CopilotIntent.UNKNOWN,
                "I'm not sure what you're asking — here are some things I can help with:",
                "PayLens Copilot can explain your financial status, analyse proposed actions, " +
                        "surface risk signals, forecast liquidity, and explain policy decisions. " +
                        "It cannot authorise or execute financial transactions.",
                keyFactors,
                null,
                "Rephrase your question or choose one of the suggested queries.",
                false,
                null, null, health, Instant.now()
        );
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private String buildHealthHeadline(FinancialHealthResponse health) {
        return switch (health.healthStatus()) {
            case HEALTHY  -> "✅ Financial health is HEALTHY — score " + health.healthScore() + "/100.";
            case CAUTION  -> "⚠️ Financial health is at CAUTION — score " + health.healthScore() + "/100.";
            case AT_RISK  -> "🔶 Financial health is AT RISK — score " + health.healthScore() + "/100. Action recommended.";
            case CRITICAL -> "🚨 Financial health is CRITICAL — score " + health.healthScore() + "/100. Immediate action required.";
        };
    }

    private String buildHealthExplanation(FinancialHealthResponse health,
            com.paylens.backend.dto.RiskSignalsResponse signals) {
        return "Current balance is ₹" + health.currentBalance() + " with ₹" + health.unpaidObligations()
                + " in upcoming obligations. " +
                "Available liquidity after safety reserve: ₹" + health.availableLiquidity() + ". " +
                "Safety buffer: ₹" + health.safetyBuffer() + ". " +
                (signals.totalCount() > 0
                        ? signals.totalCount() + " active risk signal(s) detected — review the Risk Center."
                        : "No active risk signals.");
    }

    private String buildHealthRecommendation(FinancialHealthResponse health) {
        return switch (health.healthStatus()) {
            case HEALTHY  -> "Financial position is stable. Maintain current reserve levels.";
            case CAUTION  -> "Monitor upcoming obligations closely. Avoid large discretionary outflows.";
            case AT_RISK  -> "Defer non-essential payments. Review the obligation schedule and ensure liquidity is maintained.";
            case CRITICAL -> "Halt discretionary outflows immediately. Secure additional funding or defer obligations. Human review required.";
        };
    }

    private String buildImpactNarrative(SimulationResult simulation, PolicyEvaluationResult policy) {
        String consequence = simulation.consequence() != null ? simulation.consequence().name().replace('_', ' ').toLowerCase() : "impact";
        return "The consequence of this action is categorised as: " + consequence + ".";
    }

    private String inferActionType(String message) {
        if (message == null) return "VENDOR_PAYMENT";
        String lower = message.toLowerCase();
        if (lower.contains("refund"))  return "REFUND";
        if (lower.contains("payroll") || lower.contains("salary")) return "PAYROLL";
        if (lower.contains("tax"))     return "TAX_PAYMENT";
        if (lower.contains("vendor") || lower.contains("supplier")) return "VENDOR_PAYMENT";
        return "VENDOR_PAYMENT";
    }

    private BigDecimal extractAmountFromMessage(String message) {
        if (message == null) return BigDecimal.ZERO;
        // Extract first numeric sequence (with optional commas/dots) from the message
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("[₹]?\\s*([\\d,]+(?:\\.\\d+)?)")
                .matcher(message.replace(",", ""));
        if (m.find()) {
            try {
                return new BigDecimal(m.group(1).replace(",", ""));
            } catch (NumberFormatException ignored) {}
        }
        return BigDecimal.ZERO;
    }

    private String truncate(String s, int maxLen) {
        if (s == null) return "";
        return s.length() <= maxLen ? s : s.substring(0, maxLen) + "…";
    }
}
