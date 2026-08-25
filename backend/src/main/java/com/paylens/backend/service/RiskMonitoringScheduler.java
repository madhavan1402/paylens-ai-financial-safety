package com.paylens.backend.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class RiskMonitoringScheduler {

    private static final Logger log = LoggerFactory.getLogger(RiskMonitoringScheduler.class);

    private final RiskMonitoringService monitoringService;

    public RiskMonitoringScheduler(RiskMonitoringService monitoringService) {
        this.monitoringService = monitoringService;
    }

    @Scheduled(fixedDelayString = "${paylens.monitoring.interval-ms:300000}")
    public void runScheduledMonitoring() {
        log.info("Executing scheduled PayLens risk monitoring cycle...");
        try {
            var cycle = monitoringService.runMonitoringCycle();
            log.info("Risk monitoring cycle completed: {} in {} ms. Detected: {}, Updated: {}, Resolved: {}",
                    cycle.status(), cycle.durationMs(), cycle.eventsDetected(), cycle.eventsUpdated(), cycle.eventsResolved());
        } catch (Exception e) {
            log.error("Error during scheduled risk monitoring cycle: {}", e.getMessage(), e);
        }
    }
}
