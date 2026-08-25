package com.paylens.backend.service;

import com.paylens.backend.model.RiskSignalType;
import org.springframework.stereotype.Service;

@Service
public class RiskRecommendationService {

    public String getRecommendation(RiskSignalType type) {
        if (type == null) {
            return "Review current financial state and liquidity ratios.";
        }

        return switch (type) {
            case SAFETY_BUFFER_PRESSURE ->
                    "Review upcoming discretionary vendor payments before approving outgoing funds.";
            case LOW_LIQUIDITY, LIQUIDITY_CRITICAL ->
                    "CRITICAL: Liquidity is below reserve bounds. Halt non-essential disbursements immediately.";
            case UPCOMING_OBLIGATION, HIGH_OBLIGATION_CONCENTRATION ->
                    "Ensure available liquidity covers upcoming obligations before initiating new transfers.";
            case FORECAST_BREACH ->
                    "Projected 7-day safety buffer breaches required safety reserve. Postpone uncommitted payouts.";
            case RECONCILIATION_REQUIRED, UNKNOWN_EXECUTION ->
                    "Reconcile provider state for unconfirmed executions before retrying any payment operations.";
            case EXECUTION_FAILURE, EXECUTION_FAILURE_SPIKE ->
                    "Inspect provider failure details and verify destination details before re-initiating.";
            case REVENUE_AT_RISK ->
                    "Investigate unconfirmed settlement records to recover at-risk revenue.";
        };
    }
}
