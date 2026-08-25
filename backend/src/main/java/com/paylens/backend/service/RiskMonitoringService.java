package com.paylens.backend.service;

import com.paylens.backend.dto.*;
import com.paylens.backend.model.*;
import com.paylens.backend.repository.ExecutionRepository;
import com.paylens.backend.repository.ReconciliationRepository;
import com.paylens.backend.repository.RiskEventRepository;
import com.paylens.backend.repository.RiskSnapshotRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

@Service
public class RiskMonitoringService {

    private final FinancialIntelligenceService intelligenceService;
    private final RiskEventRepository riskEventRepository;
    private final RiskSnapshotRepository riskSnapshotRepository;
    private final RiskRecommendationService recommendationService;
    private final AuditService auditService;
    private final ExecutionRepository executionRepository;
    private final ReconciliationRepository reconciliationRepository;

    private Instant lastRunAt;
    private Instant nextRunAt;
    private long lastRunDurationMs;
    private String lastRunStatus = "NOT_STARTED";
    private int lastEventsDetected;
    private int lastEventsUpdated;
    private int lastEventsResolved;

    public RiskMonitoringService(FinancialIntelligenceService intelligenceService,
                                 RiskEventRepository riskEventRepository,
                                 RiskSnapshotRepository riskSnapshotRepository,
                                 RiskRecommendationService recommendationService,
                                 AuditService auditService,
                                 ExecutionRepository executionRepository,
                                 ReconciliationRepository reconciliationRepository) {
        this.intelligenceService = intelligenceService;
        this.riskEventRepository = riskEventRepository;
        this.riskSnapshotRepository = riskSnapshotRepository;
        this.recommendationService = recommendationService;
        this.auditService = auditService;
        this.executionRepository = executionRepository;
        this.reconciliationRepository = reconciliationRepository;
    }

    @Transactional
    public synchronized MonitoringCycleResponse runMonitoringCycle() {
        long start = System.currentTimeMillis();
        Instant now = Instant.now();
        String cycleId = "cycle-" + UUID.randomUUID().toString().substring(0, 8);

        int detectedCount = 0;
        int updatedCount = 0;
        int resolvedCount = 0;

        try {
            var health = intelligenceService.getFinancialHealth();
            var forecast = intelligenceService.getForecast(7);
            var obligations = intelligenceService.getObligations();
            var signals = intelligenceService.getRiskSignals();
            var revenueAtRisk = intelligenceService.getRevenueAtRisk();

            Optional<RiskSnapshot> prevSnapshotOpt = riskSnapshotRepository.findTopByOrderByCapturedAtDesc();
            boolean isBaseline = prevSnapshotOpt.isEmpty();

            // Save new snapshot for change detection
            BigDecimal highRiskObligationAmount = obligations.obligations().stream()
                    .filter(o -> o.riskLevel() == ObligationRiskLevel.CRITICAL || o.riskLevel() == ObligationRiskLevel.HIGH)
                    .map(ObligationRiskItem::amount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            long unknownExecCount = executionRepository.countByStatus(ExecutionStatus.UNKNOWN);

            RiskSnapshot currentSnapshot = new RiskSnapshot(
                    "snap-" + UUID.randomUUID().toString().substring(0, 8),
                    now, health.healthStatus(), health.healthScore(),
                    health.availableLiquidity(), health.safetyBuffer(),
                    revenueAtRisk.totalAmount(), highRiskObligationAmount,
                    unknownExecCount, 0, isBaseline
            );
            riskSnapshotRepository.save(currentSnapshot);

            Set<String> activeFingerprints = new HashSet<>();

            // Rule 1: SAFETY_BUFFER_DETERIORATION
            if (!isBaseline && prevSnapshotOpt.isPresent()) {
                BigDecimal prevBuffer = prevSnapshotOpt.get().getSafetyBuffer();
                BigDecimal currentBuffer = health.safetyBuffer();
                if (currentBuffer.compareTo(prevBuffer.subtract(new BigDecimal("50000"))) < 0) {
                    BigDecimal drop = prevBuffer.subtract(currentBuffer);
                    String fp = "SAFETY_BUFFER_PRESSURE:account:account-merchant-primary";
                    activeFingerprints.add(fp);
                    int res = processRiskDetection(
                            fp, RiskSignalType.SAFETY_BUFFER_PRESSURE, RiskSeverity.HIGH, RiskPriority.HIGH,
                            "Safety Buffer Material Decrease",
                            "Safety buffer dropped by " + drop + " INR from " + prevBuffer + " INR to " + currentBuffer + " INR.",
                            RiskEventSource.FINANCIAL_STATE, "account", "account-merchant-primary", drop, now
                    );
                    if (res == 1) detectedCount++; else if (res == 2) updatedCount++;
                }
            }

            // Rule 2: HEALTH_STATUS_DETERIORATION
            if (!isBaseline && prevSnapshotOpt.isPresent()) {
                FinancialHealthStatus prevStatus = prevSnapshotOpt.get().getHealthStatus();
                FinancialHealthStatus currentStatus = health.healthStatus();
                if (isHealthDeteriorated(prevStatus, currentStatus)) {
                    String fp = "HEALTH_DETERIORATION:account:account-merchant-primary";
                    activeFingerprints.add(fp);
                    int res = processRiskDetection(
                            fp, RiskSignalType.SAFETY_BUFFER_PRESSURE, RiskSeverity.HIGH, RiskPriority.HIGH,
                            "Financial Health Status Degraded",
                            "Merchant financial health status degraded from " + prevStatus + " to " + currentStatus + ".",
                            RiskEventSource.FINANCIAL_STATE, "account", "account-merchant-primary", health.safetyBuffer().abs(), now
                    );
                    if (res == 1) detectedCount++; else if (res == 2) updatedCount++;
                }
            }

            // Rule 3: FORECAST_BREACH
            if (forecast.projectedSafetyBuffer().compareTo(BigDecimal.ZERO) < 0 || forecast.projectedEndingBalance().compareTo(health.safetyReserve()) < 0) {
                String fp = "FORECAST_BREACH:account:account-merchant-primary";
                activeFingerprints.add(fp);
                int res = processRiskDetection(
                        fp, RiskSignalType.FORECAST_BREACH, RiskSeverity.HIGH, RiskPriority.HIGH,
                        "7-Day Liquidity Forecast Reserve Breach",
                        "7-day projected safety buffer (" + forecast.projectedSafetyBuffer() + " INR) breaches safety reserve margin.",
                        RiskEventSource.FORECAST, "account", "account-merchant-primary", forecast.projectedSafetyBuffer().abs(), now
                );
                if (res == 1) detectedCount++; else if (res == 2) updatedCount++;
            }

            // Rule 4: UPCOMING_HIGH_RISK_OBLIGATION
            for (var obl : obligations.obligations()) {
                if (obl.daysUntilDue() <= 3 && (obl.riskLevel() == ObligationRiskLevel.CRITICAL || obl.riskLevel() == ObligationRiskLevel.HIGH)) {
                    String fp = "UPCOMING_OBLIGATION:obligation:" + obl.id();
                    activeFingerprints.add(fp);
                    int res = processRiskDetection(
                            fp, RiskSignalType.UPCOMING_OBLIGATION, RiskSeverity.HIGH, RiskPriority.HIGH,
                            "Approaching High-Risk Obligation: " + obl.description(),
                            "Obligation " + obl.description() + " of " + obl.amount() + " INR is due in " + obl.daysUntilDue() + " day(s).",
                            RiskEventSource.OBLIGATION, "obligation", obl.id(), obl.amount(), now
                    );
                    if (res == 1) detectedCount++; else if (res == 2) updatedCount++;
                }
            }

            // Rule 5: RECONCILIATION_REQUIRED
            List<ExecutionRecord> unknownExecs = executionRepository.findByStatusOrderByCreatedAtDesc(ExecutionStatus.UNKNOWN);
            for (var exec : unknownExecs) {
                String fp = "RECONCILIATION_REQUIRED:execution:" + exec.getExecutionId();
                activeFingerprints.add(fp);
                int res = processRiskDetection(
                        fp, RiskSignalType.RECONCILIATION_REQUIRED, RiskSeverity.HIGH, RiskPriority.HIGH,
                        "Provider Reconciliation Required for Payment " + exec.getExecutionId(),
                        "Execution " + exec.getExecutionId() + " for " + exec.getAmount() + " INR timed out. Provider state unknown.",
                        RiskEventSource.RECONCILIATION, "execution", exec.getExecutionId(), exec.getAmount(), now
                );
                if (res == 1) detectedCount++; else if (res == 2) updatedCount++;
            }

            // Rule 6: EXECUTION_FAILURE_SPIKE
            List<ExecutionRecord> failedExecs = executionRepository.findByStatusOrderByCreatedAtDesc(ExecutionStatus.FAILED);
            for (var exec : failedExecs) {
                String fp = "EXECUTION_FAILURE:execution:" + exec.getExecutionId();
                activeFingerprints.add(fp);
                int res = processRiskDetection(
                        fp, RiskSignalType.EXECUTION_FAILURE, RiskSeverity.MEDIUM, RiskPriority.MEDIUM,
                        "Payment Execution Failed: " + exec.getExecutionId(),
                        "Execution " + exec.getExecutionId() + " failed: " + (exec.getFailureMessage() != null ? exec.getFailureMessage() : "Provider error"),
                        RiskEventSource.EXECUTION, "execution", exec.getExecutionId(), exec.getAmount(), now
                );
                if (res == 1) detectedCount++; else if (res == 2) updatedCount++;
            }

            // Rule 7: REVENUE_AT_RISK_INCREASE
            if (!isBaseline && prevSnapshotOpt.isPresent()) {
                BigDecimal prevRevRisk = prevSnapshotOpt.get().getRevenueAtRisk();
                BigDecimal currentRevRisk = revenueAtRisk.totalAmount();
                if (currentRevRisk.compareTo(prevRevRisk.add(new BigDecimal("20000"))) > 0) {
                    String fp = "REVENUE_AT_RISK:account:account-merchant-primary";
                    activeFingerprints.add(fp);
                    int res = processRiskDetection(
                            fp, RiskSignalType.REVENUE_AT_RISK, RiskSeverity.HIGH, RiskPriority.HIGH,
                            "Revenue-at-Risk Surge Detected",
                            "Revenue at risk increased to " + currentRevRisk + " INR due to unconfirmed/failed payment settlements.",
                            RiskEventSource.REVENUE, "account", "account-merchant-primary", currentRevRisk, now
                    );
                    if (res == 1) detectedCount++; else if (res == 2) updatedCount++;
                }
            }

            // Rule 8: LIQUIDITY_CRITICAL
            if (health.healthStatus() == FinancialHealthStatus.CRITICAL) {
                String fp = "LIQUIDITY_CRITICAL:account:account-merchant-primary";
                activeFingerprints.add(fp);
                int res = processRiskDetection(
                        fp, RiskSignalType.LIQUIDITY_CRITICAL, RiskSeverity.CRITICAL, RiskPriority.CRITICAL,
                        "CRITICAL: Merchant Liquidity Depleted",
                        "Available liquidity is below zero. Merchant cannot cover operating obligations.",
                        RiskEventSource.FINANCIAL_STATE, "account", "account-merchant-primary", health.availableLiquidity().abs(), now
                );
                if (res == 1) detectedCount++; else if (res == 2) updatedCount++;
            }

            // Auto-resolution check
            List<RiskEvent> openEvents = riskEventRepository.findByStatusInOrderByLastDetectedAtDesc(List.of(RiskEventStatus.OPEN, RiskEventStatus.ACKNOWLEDGED));
            for (RiskEvent event : openEvents) {
                if (!activeFingerprints.contains(event.getFingerprint())) {
                    event.setStatus(RiskEventStatus.RESOLVED);
                    event.setResolvedAt(now);
                    event.setResolutionReason("Underlying risk condition resolved automatically by monitoring engine.");
                    event.setUpdatedAt(now);
                    riskEventRepository.save(event);
                    resolvedCount++;

                    auditService.record(
                            event.getRiskEventId(), AuditEventType.RISK_RESOLVED,
                            ActorType.SYSTEM, "risk-monitoring-engine",
                            "Risk event " + event.getRiskEventId() + " automatically resolved: condition cleared."
                    );
                }
            }

            long duration = System.currentTimeMillis() - start;
            this.lastRunAt = now;
            this.nextRunAt = now.plusSeconds(300);
            this.lastRunDurationMs = duration;
            this.lastRunStatus = "SUCCESS";
            this.lastEventsDetected = detectedCount;
            this.lastEventsUpdated = updatedCount;
            this.lastEventsResolved = resolvedCount;

            return new MonitoringCycleResponse(cycleId, now, duration, detectedCount, updatedCount, resolvedCount, "SUCCESS");

        } catch (Exception e) {
            long duration = System.currentTimeMillis() - start;
            this.lastRunAt = now;
            this.lastRunDurationMs = duration;
            this.lastRunStatus = "FAILED";
            return new MonitoringCycleResponse(cycleId, now, duration, 0, 0, 0, "FAILED: " + e.getMessage());
        }
    }

    private int processRiskDetection(String fingerprint, RiskSignalType type, RiskSeverity severity,
                                      RiskPriority priority, String title, String description,
                                      RiskEventSource source, String entityType, String entityId,
                                      BigDecimal impact, Instant now) {
        List<RiskEvent> existing = riskEventRepository.findByFingerprintAndStatusIn(
                fingerprint, List.of(RiskEventStatus.OPEN, RiskEventStatus.ACKNOWLEDGED)
        );

        if (!existing.isEmpty()) {
            RiskEvent event = existing.get(0);
            event.setLastDetectedAt(now);
            event.setOccurrenceCount(event.getOccurrenceCount() + 1);
            event.setFinancialImpact(impact);
            event.setSeverity(severity);
            event.setPriority(priority);
            event.setUpdatedAt(now);
            riskEventRepository.save(event);

            auditService.record(
                    event.getRiskEventId(), AuditEventType.RISK_UPDATED,
                    ActorType.SYSTEM, "risk-monitoring-engine",
                    "Updated risk event " + event.getRiskEventId() + " (Occurrence " + event.getOccurrenceCount() + ")"
            );
            return 2; // Updated
        } else {
            String eventId = "risk-" + UUID.randomUUID().toString().substring(0, 8);
            String rec = recommendationService.getRecommendation(type);

            RiskEvent event = new RiskEvent(
                    eventId, fingerprint, type, severity, priority, title, description,
                    RiskEventStatus.OPEN, source, now, now, now, null, 1,
                    entityType, entityId, rec, impact, null, null, now, now
            );
            riskEventRepository.save(event);

            auditService.record(
                    eventId, AuditEventType.RISK_DETECTED,
                    ActorType.SYSTEM, "risk-monitoring-engine",
                    "Detected new risk event " + eventId + ": " + title
            );
            return 1; // Created
        }
    }

    private boolean isHealthDeteriorated(FinancialHealthStatus prev, FinancialHealthStatus current) {
        int prevRank = getStatusRank(prev);
        int currentRank = getStatusRank(current);
        return currentRank > prevRank;
    }

    private int getStatusRank(FinancialHealthStatus status) {
        return switch (status) {
            case HEALTHY -> 1;
            case CAUTION -> 2;
            case AT_RISK -> 3;
            case CRITICAL -> 4;
        };
    }

    @Transactional
    public RiskEventResponse acknowledgeRiskEvent(String riskEventId) {
        RiskEvent event = riskEventRepository.findById(riskEventId)
                .orElseThrow(() -> new IllegalArgumentException("Risk event not found: " + riskEventId));

        if (event.getStatus() != RiskEventStatus.OPEN) {
            throw new IllegalStateException("Only OPEN risk events can be acknowledged. Current status: " + event.getStatus());
        }

        Instant now = Instant.now();
        event.setStatus(RiskEventStatus.ACKNOWLEDGED);
        event.setUpdatedAt(now);
        riskEventRepository.save(event);

        auditService.record(
                riskEventId, AuditEventType.RISK_ACKNOWLEDGED,
                ActorType.HUMAN, "user-operator",
                "Acknowledged risk event " + riskEventId
        );

        return RiskEventResponse.from(event);
    }

    @Transactional
    public RiskEventResponse dismissRiskEvent(String riskEventId, String reason) {
        RiskEvent event = riskEventRepository.findById(riskEventId)
                .orElseThrow(() -> new IllegalArgumentException("Risk event not found: " + riskEventId));

        if (event.getStatus() == RiskEventStatus.RESOLVED || event.getStatus() == RiskEventStatus.DISMISSED) {
            throw new IllegalStateException("Risk event is already terminal. Current status: " + event.getStatus());
        }

        Instant now = Instant.now();
        event.setStatus(RiskEventStatus.DISMISSED);
        event.setDismissalReason(reason != null && !reason.isBlank() ? reason : "Dismissed by user.");
        event.setUpdatedAt(now);
        riskEventRepository.save(event);

        auditService.record(
                riskEventId, AuditEventType.RISK_DISMISSED,
                ActorType.HUMAN, "user-operator",
                "Dismissed risk event " + riskEventId + ": " + event.getDismissalReason()
        );

        return RiskEventResponse.from(event);
    }

    @Transactional
    public RiskEventResponse resolveRiskEvent(String riskEventId, String reason) {
        RiskEvent event = riskEventRepository.findById(riskEventId)
                .orElseThrow(() -> new IllegalArgumentException("Risk event not found: " + riskEventId));

        if (event.getStatus() == RiskEventStatus.RESOLVED) {
            return RiskEventResponse.from(event);
        }

        // Validate if underlying condition is still active
        var health = intelligenceService.getFinancialHealth();
        if (event.getRiskSignalType() == RiskSignalType.RECONCILIATION_REQUIRED && executionRepository.countByStatus(ExecutionStatus.UNKNOWN) > 0) {
            throw new IllegalStateException("Cannot resolve risk event while UNKNOWN executions exist.");
        }

        Instant now = Instant.now();
        event.setStatus(RiskEventStatus.RESOLVED);
        event.setResolvedAt(now);
        event.setResolutionReason(reason != null && !reason.isBlank() ? reason : "Resolved by user.");
        event.setUpdatedAt(now);
        riskEventRepository.save(event);

        auditService.record(
                riskEventId, AuditEventType.RISK_RESOLVED,
                ActorType.HUMAN, "user-operator",
                "Manually resolved risk event " + riskEventId
        );

        return RiskEventResponse.from(event);
    }

    public List<RiskEventResponse> getFilteredRiskEvents(RiskEventStatus status, RiskSeverity severity, RiskSignalType type) {
        return riskEventRepository.findFiltered(status, severity, type).stream()
                .map(RiskEventResponse::from)
                .toList();
    }

    public RiskEventResponse getRiskEventDetail(String riskEventId) {
        RiskEvent event = riskEventRepository.findById(riskEventId)
                .orElseThrow(() -> new IllegalArgumentException("Risk event not found: " + riskEventId));
        return RiskEventResponse.from(event);
    }

    public MonitoringStatusResponse getMonitoringStatus() {
        long open = riskEventRepository.countByStatus(RiskEventStatus.OPEN);
        long ack = riskEventRepository.countByStatus(RiskEventStatus.ACKNOWLEDGED);
        long resolved = riskEventRepository.countByStatus(RiskEventStatus.RESOLVED);
        long dismissed = riskEventRepository.countByStatus(RiskEventStatus.DISMISSED);

        return new MonitoringStatusResponse(
                lastRunAt, nextRunAt, true, lastRunDurationMs,
                lastRunStatus, lastEventsDetected, lastEventsUpdated, lastEventsResolved,
                open, ack, resolved, dismissed
        );
    }
}
