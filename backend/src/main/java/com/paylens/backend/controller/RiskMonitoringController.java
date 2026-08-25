package com.paylens.backend.controller;

import com.paylens.backend.dto.MonitoringCycleResponse;
import com.paylens.backend.dto.MonitoringStatusResponse;
import com.paylens.backend.service.RiskMonitoringService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/risk-monitoring")
public class RiskMonitoringController {

    private final RiskMonitoringService monitoringService;

    public RiskMonitoringController(RiskMonitoringService monitoringService) {
        this.monitoringService = monitoringService;
    }

    @PostMapping("/run")
    public ResponseEntity<MonitoringCycleResponse> runMonitoringCycle() {
        return ResponseEntity.ok(monitoringService.runMonitoringCycle());
    }

    @GetMapping("/status")
    public ResponseEntity<MonitoringStatusResponse> getMonitoringStatus() {
        return ResponseEntity.ok(monitoringService.getMonitoringStatus());
    }
}
