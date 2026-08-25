package com.paylens.backend.controller;

import com.paylens.backend.dto.*;
import com.paylens.backend.service.FinancialIntelligenceService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/intelligence")
public class IntelligenceController {

    private final FinancialIntelligenceService intelligenceService;

    public IntelligenceController(FinancialIntelligenceService intelligenceService) {
        this.intelligenceService = intelligenceService;
    }

    @GetMapping("/financial-health")
    public ResponseEntity<FinancialHealthResponse> getFinancialHealth() {
        return ResponseEntity.ok(intelligenceService.getFinancialHealth());
    }

    @GetMapping("/cash-flow")
    public ResponseEntity<CashFlowResponse> getCashFlow(@RequestParam(required = false) String period) {
        return ResponseEntity.ok(intelligenceService.getCashFlow(period));
    }

    @GetMapping("/obligations")
    public ResponseEntity<ObligationsRiskResponse> getObligations() {
        return ResponseEntity.ok(intelligenceService.getObligations());
    }

    @GetMapping("/forecast")
    public ResponseEntity<LiquidityForecastResponse> getForecast(@RequestParam(defaultValue = "7") int days) {
        return ResponseEntity.ok(intelligenceService.getForecast(days));
    }

    @PostMapping("/forecast/scenario")
    public ResponseEntity<ForecastScenarioResponse> simulateScenario(@RequestBody ForecastScenarioRequest request) {
        return ResponseEntity.ok(intelligenceService.simulateScenario(request));
    }

    @GetMapping("/risk-signals")
    public ResponseEntity<RiskSignalsResponse> getRiskSignals() {
        return ResponseEntity.ok(intelligenceService.getRiskSignals());
    }

    @GetMapping("/revenue-at-risk")
    public ResponseEntity<RevenueAtRiskResponse> getRevenueAtRisk() {
        return ResponseEntity.ok(intelligenceService.getRevenueAtRisk());
    }

    @GetMapping("/summary")
    public ResponseEntity<IntelligenceSummaryResponse> getSummary() {
        return ResponseEntity.ok(intelligenceService.getIntelligenceSummary());
    }
}
