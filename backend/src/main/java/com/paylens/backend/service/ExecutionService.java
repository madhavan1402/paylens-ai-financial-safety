package com.paylens.backend.service;

import com.paylens.backend.dto.*;
import com.paylens.backend.model.*;
import com.paylens.backend.repository.DecisionRepository;
import com.paylens.backend.repository.ExecutionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

@Service
public class ExecutionService {

    private final DecisionRepository decisionRepository;
    private final ExecutionRepository executionRepository;
    private final PaymentExecutionProvider paymentExecutionProvider;
    private final AuditService auditService;

    public ExecutionService(DecisionRepository decisionRepository,
                            ExecutionRepository executionRepository,
                            PaymentExecutionProvider paymentExecutionProvider,
                            AuditService auditService) {
        this.decisionRepository = decisionRepository;
        this.executionRepository = executionRepository;
        this.paymentExecutionProvider = paymentExecutionProvider;
        this.auditService = auditService;
    }

    @Transactional
    public ExecutionResponse execute(ExecutionApiRequest request) {
        String decisionId = request.decisionId();
        String idempotencyKey = request.idempotencyKey();

        // 1. Idempotency check at DB query level
        var existingKeyRecord = executionRepository.findByIdempotencyKey(idempotencyKey);
        if (existingKeyRecord.isPresent()) {
            var record = existingKeyRecord.get();
            auditService.record(decisionId, AuditEventType.EXECUTION_DUPLICATE, ActorType.SYSTEM, "paylens",
                    "Duplicate execution request received for idempotency key: " + idempotencyKey);
            return toResponse(record);
        }

        // 2. Load authoritative DecisionRecord from persistence
        DecisionRecord decision = decisionRepository.findByDecisionId(decisionId)
                .orElseThrow(() -> new NoSuchElementException("Decision not found: " + decisionId));

        // 3. Check if decision has already reached a successful terminal execution state
        var existingDecisionExecs = executionRepository.findByDecisionIdAndStatusIn(
                decisionId, List.of(ExecutionStatus.SUCCEEDED, ExecutionStatus.PROCESSING));
        if (!existingDecisionExecs.isEmpty()) {
            var record = existingDecisionExecs.get(0);
            auditService.record(decisionId, AuditEventType.EXECUTION_DUPLICATE, ActorType.SYSTEM, "paylens",
                    "Decision has already been executed. Terminal state reached.");
            return toResponse(record);
        }

        String executionId = "exec_" + UUID.randomUUID().toString();
        ExecutionProvider provider = paymentExecutionProvider.getProviderType();
        String actionType = decision.getActionType();
        BigDecimal amount = decision.getAmount();
        String currency = decision.getCurrency();
        String target = decision.getTarget();

        // Audit request
        auditService.record(decisionId, AuditEventType.EXECUTION_REQUESTED, ActorType.HUMAN, "operator",
                "Execution requested with idempotency key: " + idempotencyKey);

        // 4. Server-Side Governance & Eligibility Validation (Never trust client input)
        String eligibilityError = validateEligibility(decision);
        if (eligibilityError != null) {
            ExecutionRecord record = new ExecutionRecord(
                    executionId, decisionId, idempotencyKey, provider,
                    actionType, amount, currency, target, ExecutionStatus.ELIGIBILITY_REJECTED
            );
            record.setFailureCode("ELIGIBILITY_DENIED");
            record.setFailureMessage(eligibilityError);

            record = executionRepository.save(record);
            auditService.record(decisionId, AuditEventType.EXECUTION_ELIGIBILITY_REJECTED, ActorType.SYSTEM, "paylens",
                    "Execution eligibility denied: " + eligibilityError);
            return toResponse(record);
        }

        // 5. Initial Execution Record Creation (PROCESSING state)
        ExecutionRecord record = new ExecutionRecord(
                executionId, decisionId, idempotencyKey, provider,
                actionType, amount, currency, target, ExecutionStatus.PROCESSING
        );
        record = executionRepository.save(record);
        auditService.record(decisionId, AuditEventType.EXECUTION_STARTED, ActorType.SYSTEM, "paylens",
                "Execution started via provider " + provider);

        // 6. Invoke Payment Execution Provider
        ExecutionCommand command = new ExecutionCommand(
                executionId, decisionId, actionType, amount, currency, target, decision.getOriginalMessage()
        );
        ExecutionProviderResult result = paymentExecutionProvider.execute(command);

        // 7. Persist Provider Result & Log Audit Event
        record.setStatus(result.status());
        record.setProviderReference(result.providerReference());
        record.setFailureCode(result.failureCode());
        record.setFailureMessage(result.failureMessage());
        record = executionRepository.save(record);

        switch (result.status()) {
            case SUCCEEDED -> auditService.record(decisionId, AuditEventType.EXECUTION_SUCCEEDED, ActorType.SYSTEM, "paylens",
                    "Execution succeeded. Provider ref: " + result.providerReference());
            case FAILED -> auditService.record(decisionId, AuditEventType.EXECUTION_FAILED, ActorType.SYSTEM, "paylens",
                    "Execution failed. Code: " + result.failureCode() + ", Message: " + result.failureMessage());
            case UNKNOWN -> auditService.record(decisionId, AuditEventType.EXECUTION_UNKNOWN, ActorType.SYSTEM, "paylens",
                    "Execution outcome unknown. Provider communication timed out.");
            case UNSUPPORTED_EXECUTION -> auditService.record(decisionId, AuditEventType.EXECUTION_UNSUPPORTED, ActorType.SYSTEM, "paylens",
                    "Execution unsupported: " + result.failureMessage());
            default -> {}
        }

        return toResponse(record);
    }

    private String validateEligibility(DecisionRecord decision) {
        GovernanceStatus govStatus = decision.getStatus();
        if (govStatus == GovernanceStatus.BLOCKED) {
            return "BLOCKED decisions can never reach payment infrastructure.";
        }
        if (govStatus == GovernanceStatus.PENDING_REVIEW) {
            return "PENDING_REVIEW decisions require human governance approval before execution.";
        }
        if (govStatus == GovernanceStatus.REJECTED) {
            return "REJECTED decisions cannot be executed.";
        }
        if (govStatus != GovernanceStatus.SAFE && govStatus != GovernanceStatus.APPROVED) {
            return "Governance status '" + govStatus + "' does not permit execution.";
        }
        if (decision.getAmount() == null || decision.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            return "Invalid execution amount: must be positive.";
        }
        return null;
    }

    @Transactional(readOnly = true)
    public ExecutionResponse getExecution(String executionId) {
        ExecutionRecord record = executionRepository.findByExecutionId(executionId)
                .orElseThrow(() -> new NoSuchElementException("Execution record not found: " + executionId));
        return toResponse(record);
    }

    @Transactional(readOnly = true)
    public ExecutionResponse getExecutionByIdempotencyKey(String idempotencyKey) {
        ExecutionRecord record = executionRepository.findByIdempotencyKey(idempotencyKey)
                .orElseThrow(() -> new NoSuchElementException("Execution record not found for idempotency key: " + idempotencyKey));
        return toResponse(record);
    }

    @Transactional(readOnly = true)
    public ExecutionResponse getExecutionByDecisionId(String decisionId) {
        ExecutionRecord record = executionRepository.findByDecisionId(decisionId)
                .orElseThrow(() -> new NoSuchElementException("No execution record found for decision: " + decisionId));
        return toResponse(record);
    }

    @Transactional(readOnly = true)
    public List<ExecutionSummaryResponse> listExecutions(String status) {
        List<ExecutionRecord> records;
        if (status != null && !status.isBlank()) {
            try {
                ExecutionStatus execStatus = ExecutionStatus.valueOf(status.toUpperCase());
                records = executionRepository.findByStatusOrderByCreatedAtDesc(execStatus);
            } catch (IllegalArgumentException e) {
                records = List.of();
            }
        } else {
            records = executionRepository.findAllByOrderByCreatedAtDesc();
        }
        return records.stream().map(this::toSummaryResponse).toList();
    }

    private ExecutionResponse toResponse(ExecutionRecord r) {
        return new ExecutionResponse(
                r.getExecutionId(),
                r.getDecisionId(),
                r.getIdempotencyKey(),
                r.getProvider(),
                r.getProviderReference(),
                r.getActionType(),
                r.getAmount(),
                r.getCurrency(),
                r.getTarget(),
                r.getStatus(),
                r.getFailureCode(),
                r.getFailureMessage(),
                r.getCreatedAt(),
                r.getUpdatedAt()
        );
    }

    private ExecutionSummaryResponse toSummaryResponse(ExecutionRecord r) {
        return new ExecutionSummaryResponse(
                r.getExecutionId(),
                r.getDecisionId(),
                r.getActionType(),
                r.getAmount(),
                r.getCurrency(),
                r.getProvider(),
                r.getProviderReference(),
                r.getStatus(),
                r.getCreatedAt()
        );
    }
}
