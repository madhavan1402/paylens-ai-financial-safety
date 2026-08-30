package com.paylens.backend.dto;

import com.paylens.backend.model.CopilotIntent;
import java.time.Instant;
import java.util.List;

/**
 * Structured response returned by the Copilot for every merchant query.
 *
 * <p>The Copilot answers these five questions deterministically:
 * <ol>
 *   <li>What changed? ({@link #headline})
 *   <li>Why does it matter? ({@link #explanation})
 *   <li>What is the financial impact? ({@link #financialImpact})
 *   <li>What does PayLens recommend? ({@link #recommendation})
 *   <li>Is human review required? ({@link #requiresHumanReview})
 * </ol>
 *
 * <p>NOTE: the Copilot NEVER authorises or executes financial transactions.
 */
public record CopilotResponse(
        /** Detected intent category. */
        CopilotIntent intent,

        /** One-line headline that directly answers the merchant question. */
        String headline,

        /** Detailed explanation built from deterministic facts only. */
        String explanation,

        /** Key supporting facts cited from live financial data. */
        List<String> keyFactors,

        /** Quantified financial impact description. May be null for informational queries. */
        String financialImpact,

        /** Actionable recommendation from the Policy Engine. */
        String recommendation,

        /** True when governance / human approval should be triggered. */
        boolean requiresHumanReview,

        /** Optional policy decision when an action was simulated. */
        String policyDecision,

        /** Optional full simulation result when an action was analysed. */
        SimulationResult simulation,

        /** Raw financial health snapshot used to build this response. */
        FinancialHealthResponse financialHealth,

        /** ISO-8601 timestamp of when this response was generated. */
        Instant generatedAt
) {}
