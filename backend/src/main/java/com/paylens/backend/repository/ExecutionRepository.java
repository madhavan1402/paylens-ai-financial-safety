package com.paylens.backend.repository;

import com.paylens.backend.model.ExecutionRecord;
import com.paylens.backend.model.ExecutionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface ExecutionRepository extends JpaRepository<ExecutionRecord, Long> {
    Optional<ExecutionRecord> findByExecutionId(String executionId);
    Optional<ExecutionRecord> findByIdempotencyKey(String idempotencyKey);
    Optional<ExecutionRecord> findByDecisionId(String decisionId);
    List<ExecutionRecord> findByDecisionIdAndStatusIn(String decisionId, List<ExecutionStatus> statuses);
    List<ExecutionRecord> findByStatusOrderByCreatedAtDesc(ExecutionStatus status);
    List<ExecutionRecord> findAllByOrderByCreatedAtDesc();
}
