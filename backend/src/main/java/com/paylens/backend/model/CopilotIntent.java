package com.paylens.backend.model;

/**
 * Structured intent categories that the Copilot recognises from natural-language queries.
 * The copilot itself never executes financial transactions; it only classifies, explains,
 * and delegates to deterministic services.
 */
public enum CopilotIntent {
    FINANCIAL_STATUS,
    ACTION_ANALYSIS,
    RISK_EXPLANATION,
    POLICY_EXPLANATION,
    FORECAST_QUERY,
    UNKNOWN
}
