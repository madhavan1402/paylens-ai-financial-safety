package com.paylens.backend.controller;

import com.paylens.backend.dto.*;
import com.paylens.backend.service.ReconciliationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class ReconciliationController {

    private final ReconciliationService reconciliationService;

    public ReconciliationController(ReconciliationService reconciliationService) {
        this.reconciliationService = reconciliationService;
    }

    @PostMapping("/executions/{executionId}/reconcile")
    public ResponseEntity<ReconciliationResponse> reconcile(@PathVariable String executionId) {
        ReconciliationResponse response = reconciliationService.reconcile(executionId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/executions/{executionId}/reconciliation")
    public ResponseEntity<ReconciliationResponse> getLatestReconciliation(@PathVariable String executionId) {
        ReconciliationResponse response = reconciliationService.getLatestReconciliation(executionId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/reconciliations")
    public ResponseEntity<Map<String, List<ReconciliationSummaryResponse>>> listReconciliations(
            @RequestParam(required = false) String executionId,
            @RequestParam(required = false) String status) {
        List<ReconciliationSummaryResponse> reconciliations = reconciliationService.listReconciliations(executionId, status);
        return ResponseEntity.ok(Map.of("reconciliations", reconciliations));
    }

    @GetMapping("/reconciliations/metrics")
    public ResponseEntity<ReliabilityMetricsResponse> getReliabilityMetrics() {
        ReliabilityMetricsResponse metrics = reconciliationService.getReliabilityMetrics();
        return ResponseEntity.ok(metrics);
    }

    @GetMapping("/reconciliations/{reconciliationId}")
    public ResponseEntity<ReconciliationResponse> getReconciliation(@PathVariable String reconciliationId) {
        ReconciliationResponse response = reconciliationService.getReconciliation(reconciliationId);
        return ResponseEntity.ok(response);
    }
}
