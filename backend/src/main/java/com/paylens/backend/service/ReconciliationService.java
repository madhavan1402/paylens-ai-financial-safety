package com.paylens.backend.service;

import com.paylens.backend.dto.*;
import com.paylens.backend.model.*;
import com.paylens.backend.repository.DecisionRepository;
import com.paylens.backend.repository.ExecutionRepository;
import com.paylens.backend.repository.ReconciliationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

@Service
public class ReconciliationService {

    private final ExecutionRepository executionRepository;
    private final DecisionRepository decisionRepository;
    private final ReconciliationRepository reconciliationRepository;
    private final PaymentReconciliationProvider paymentReconciliationProvider;
    private final AuditService auditService;

    public ReconciliationService(ExecutionRepository executionRepository,
                                 DecisionRepository decisionRepository,
                                 ReconciliationRepository reconciliationRepository,
                                 PaymentReconciliationProvider paymentReconciliationProvider,
                                 AuditService auditService) {
        this.executionRepository = executionRepository;
        this.decisionRepository = decisionRepository;
        this.reconciliationRepository = reconciliationRepository;
        this.paymentReconciliationProvider = paymentReconciliationProvider;
        this.auditService = auditService;
    }

    @Transactional
    public ReconciliationResponse reconcile(String executionId) {
        // Lock on execution ID to prevent concurrent duplicate reconciliation calls
        synchronized (executionId.intern()) {
            ExecutionRecord execution = executionRepository.findByExecutionId(executionId)
                    .orElseThrow(() -> new NoSuchElementException("Execution record not found: " + executionId));

            String decisionId = execution.getDecisionId();
            String providerRef = execution.getProviderReference();
            ExecutionStatus currentExecStatus = execution.getStatus();

            // Check if authoritative reconciliation has already resolved this execution
            var existingOpt = reconciliationRepository.findFirstByExecutionIdOrderByCreatedAtDesc(executionId);
            if (existingOpt.isPresent()) {
                var existing = existingOpt.get();
                if (existing.getStatus() == ReconciliationStatus.CONFIRMED || existing.getStatus() == ReconciliationStatus.FAILED) {
                    auditService.record(decisionId, AuditEventType.RECONCILIATION_REQUESTED, ActorType.SYSTEM, "paylens",
                            "Reconciliation already resolved for execution: " + executionId);
                    return toResponse(existing);
                }
            }

            int attemptNumber = existingOpt.map(r -> r.getAttemptNumber() + 1).orElse(1);
            String reconciliationId = "recon_" + UUID.randomUUID().toString();

            // Record initial reconciliation request
            auditService.record(decisionId, AuditEventType.RECONCILIATION_REQUESTED, ActorType.HUMAN, "operator",
                    "Reconciliation requested for execution: " + executionId);

            ReconciliationRecord record = new ReconciliationRecord(
                    reconciliationId, executionId, decisionId, execution.getProvider(),
                    providerRef, currentExecStatus, attemptNumber
            );
            record = reconciliationRepository.save(record);
            auditService.record(decisionId, AuditEventType.RECONCILIATION_STARTED, ActorType.SYSTEM, "paylens",
                    "Querying provider state for reference: " + (providerRef != null ? providerRef : "NONE"));

            // 1. Invoke Reconciliation Provider
            ReconciliationCommand command = new ReconciliationCommand(
                    reconciliationId, executionId, decisionId, execution.getProvider(),
                    providerRef, execution.getActionType(), execution.getAmount(), execution.getCurrency()
            );
            ReconciliationProviderResult result = paymentReconciliationProvider.reconcile(command);

            // 2. Process Normalized Provider Result & Update States
            record.setProviderOutcome(result.outcome());
            record.setFailureCode(result.failureCode());
            record.setFailureMessage(result.failureMessage());

            switch (result.outcome()) {
                case CONFIRMED_SUCCESS -> {
                    execution.setStatus(ExecutionStatus.SUCCEEDED);
                    executionRepository.save(execution);

                    record.setResolvedExecutionStatus(ExecutionStatus.SUCCEEDED);
                    record.setStatus(ReconciliationStatus.CONFIRMED);
                    record.setRetryDecision(RetryDecision.NOT_SAFE);
                    record.setResolution("Provider confirmed payment execution succeeded.");
                    record.setResolvedAt(Instant.now());

                    auditService.record(decisionId, AuditEventType.RECONCILIATION_CONFIRMED, ActorType.SYSTEM, "paylens",
                            "Reconciliation confirmed success. Provider status: " + result.rawProviderStatus());
                }
                case CONFIRMED_FAILURE -> {
                    execution.setStatus(ExecutionStatus.FAILED);
                    executionRepository.save(execution);

                    record.setResolvedExecutionStatus(ExecutionStatus.FAILED);
                    record.setStatus(ReconciliationStatus.FAILED);
                    record.setRetryDecision(RetryDecision.NOT_SAFE);
                    record.setResolution("Provider confirmed payment execution failed.");
                    record.setResolvedAt(Instant.now());

                    auditService.record(decisionId, AuditEventType.RECONCILIATION_FAILED, ActorType.SYSTEM, "paylens",
                            "Reconciliation confirmed failure. Code: " + result.failureCode());
                }
                case STILL_PROCESSING -> {
                    record.setResolvedExecutionStatus(ExecutionStatus.UNKNOWN);
                    record.setStatus(ReconciliationStatus.PENDING);
                    record.setRetryDecision(RetryDecision.NOT_SAFE);
                    record.setResolution("Provider transaction is still processing.");

                    auditService.record(decisionId, AuditEventType.RECONCILIATION_PENDING, ActorType.SYSTEM, "paylens",
                            "Reconciliation pending. Provider status: " + result.rawProviderStatus());
                }
                case NOT_FOUND -> {
                    execution.setStatus(ExecutionStatus.UNKNOWN);
                    executionRepository.save(execution);

                    record.setResolvedExecutionStatus(ExecutionStatus.UNKNOWN);
                    record.setStatus(ReconciliationStatus.MANUAL_REVIEW_REQUIRED);
                    record.setRetryDecision(RetryDecision.MANUAL_REVIEW);
                    record.setResolution("Provider reference not found. Manual review required to verify if transaction reached provider.");

                    auditService.record(decisionId, AuditEventType.RECONCILIATION_NOT_FOUND, ActorType.SYSTEM, "paylens",
                            "Provider reference not found: " + providerRef);
                    auditService.record(decisionId, AuditEventType.RECONCILIATION_MANUAL_REVIEW, ActorType.SYSTEM, "paylens",
                            "Manual reconciliation required due to unconfirmed provider reference.");
                }
                case UNKNOWN -> {
                    execution.setStatus(ExecutionStatus.UNKNOWN);
                    executionRepository.save(execution);

                    record.setResolvedExecutionStatus(ExecutionStatus.UNKNOWN);
                    record.setStatus(ReconciliationStatus.MANUAL_REVIEW_REQUIRED);
                    record.setRetryDecision(RetryDecision.MANUAL_REVIEW);
                    record.setResolution("Provider reconciliation timed out or returned unknown state. Manual review required.");

                    auditService.record(decisionId, AuditEventType.RECONCILIATION_MANUAL_REVIEW, ActorType.SYSTEM, "paylens",
                            "Manual reconciliation required due to uncertain provider outcome.");
                }
            }

            record = reconciliationRepository.save(record);
            return toResponse(record);
        }
    }

    @Transactional(readOnly = true)
    public ReconciliationResponse getLatestReconciliation(String executionId) {
        ReconciliationRecord record = reconciliationRepository.findFirstByExecutionIdOrderByCreatedAtDesc(executionId)
                .orElseThrow(() -> new NoSuchElementException("No reconciliation record found for execution: " + executionId));
        return toResponse(record);
    }

    @Transactional(readOnly = true)
    public ReconciliationResponse getReconciliation(String reconciliationId) {
        ReconciliationRecord record = reconciliationRepository.findByReconciliationId(reconciliationId)
                .orElseThrow(() -> new NoSuchElementException("Reconciliation record not found: " + reconciliationId));
        return toResponse(record);
    }

    @Transactional(readOnly = true)
    public List<ReconciliationSummaryResponse> listReconciliations(String executionId, String status) {
        List<ReconciliationRecord> records;
        if (executionId != null && !executionId.isBlank()) {
            records = reconciliationRepository.findByExecutionIdOrderByCreatedAtDesc(executionId);
        } else if (status != null && !status.isBlank()) {
            try {
                ReconciliationStatus reconStatus = ReconciliationStatus.valueOf(status.toUpperCase());
                records = reconciliationRepository.findByStatusOrderByCreatedAtDesc(reconStatus);
            } catch (IllegalArgumentException e) {
                records = List.of();
            }
        } else {
            records = reconciliationRepository.findAllByOrderByCreatedAtDesc();
        }
        return records.stream().map(this::toSummaryResponse).toList();
    }

    @Transactional(readOnly = true)
    public ReliabilityMetricsResponse getReliabilityMetrics() {
        long totalExecutions = executionRepository.count();
        long confirmedSuccess = executionRepository.countByStatus(ExecutionStatus.SUCCEEDED);
        long confirmedFailure = executionRepository.countByStatus(ExecutionStatus.FAILED);
        long pending = executionRepository.countByStatus(ExecutionStatus.PROCESSING);
        long unknownOrManualReview = executionRepository.countByStatus(ExecutionStatus.UNKNOWN)
                + executionRepository.countByStatus(ExecutionStatus.ELIGIBILITY_REJECTED);

        long resolved = confirmedSuccess + confirmedFailure;
        double successRate = resolved == 0 ? 0.0 : (double) (confirmedSuccess * 100.0) / resolved;

        return new ReliabilityMetricsResponse(
                totalExecutions, confirmedSuccess, confirmedFailure, pending, unknownOrManualReview, successRate
        );
    }

    private ReconciliationResponse toResponse(ReconciliationRecord r) {
        return new ReconciliationResponse(
                r.getReconciliationId(),
                r.getExecutionId(),
                r.getDecisionId(),
                r.getProvider(),
                r.getProviderReference(),
                r.getPreviousExecutionStatus(),
                r.getResolvedExecutionStatus(),
                r.getStatus(),
                r.getProviderOutcome(),
                r.getRetryDecision(),
                r.getResolution(),
                r.getFailureCode(),
                r.getFailureMessage(),
                r.getAttemptNumber(),
                r.getCreatedAt(),
                r.getUpdatedAt(),
                r.getResolvedAt()
        );
    }

    private ReconciliationSummaryResponse toSummaryResponse(ReconciliationRecord r) {
        return new ReconciliationSummaryResponse(
                r.getReconciliationId(),
                r.getExecutionId(),
                r.getDecisionId(),
                r.getProvider(),
                r.getProviderReference(),
                r.getResolvedExecutionStatus(),
                r.getStatus(),
                r.getProviderOutcome(),
                r.getRetryDecision(),
                r.getCreatedAt()
        );
    }
}
