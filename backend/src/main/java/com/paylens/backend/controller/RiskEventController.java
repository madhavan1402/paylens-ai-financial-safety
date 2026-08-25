package com.paylens.backend.controller;

import com.paylens.backend.dto.RiskDismissRequest;
import com.paylens.backend.dto.RiskEventResponse;
import com.paylens.backend.model.RiskEventStatus;
import com.paylens.backend.model.RiskSeverity;
import com.paylens.backend.model.RiskSignalType;
import com.paylens.backend.service.RiskMonitoringService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/risk-events")
public class RiskEventController {

    private final RiskMonitoringService monitoringService;

    public RiskEventController(RiskMonitoringService monitoringService) {
        this.monitoringService = monitoringService;
    }

    @GetMapping
    public ResponseEntity<List<RiskEventResponse>> getRiskEvents(
            @RequestParam(required = false) RiskEventStatus status,
            @RequestParam(required = false) RiskSeverity severity,
            @RequestParam(required = false) RiskSignalType type
    ) {
        return ResponseEntity.ok(monitoringService.getFilteredRiskEvents(status, severity, type));
    }

    @GetMapping("/{riskEventId}")
    public ResponseEntity<RiskEventResponse> getRiskEventDetail(@PathVariable String riskEventId) {
        try {
            return ResponseEntity.ok(monitoringService.getRiskEventDetail(riskEventId));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/{riskEventId}/acknowledge")
    public ResponseEntity<?> acknowledgeRiskEvent(@PathVariable String riskEventId) {
        try {
            return ResponseEntity.ok(monitoringService.acknowledgeRiskEvent(riskEventId));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/{riskEventId}/dismiss")
    public ResponseEntity<?> dismissRiskEvent(
            @PathVariable String riskEventId,
            @RequestBody(required = false) RiskDismissRequest request
    ) {
        try {
            String reason = request != null ? request.reason() : "Dismissed by user.";
            return ResponseEntity.ok(monitoringService.dismissRiskEvent(riskEventId, reason));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/{riskEventId}/resolve")
    public ResponseEntity<?> resolveRiskEvent(
            @PathVariable String riskEventId,
            @RequestBody(required = false) RiskDismissRequest request
    ) {
        try {
            String reason = request != null ? request.reason() : "Manually resolved by user.";
            return ResponseEntity.ok(monitoringService.resolveRiskEvent(riskEventId, reason));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", e.getMessage()));
        }
    }
}
