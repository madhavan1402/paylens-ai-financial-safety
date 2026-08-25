package com.paylens.backend.repository;

import com.paylens.backend.model.ReconciliationRecord;
import com.paylens.backend.model.ReconciliationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface ReconciliationRepository extends JpaRepository<ReconciliationRecord, Long> {
    Optional<ReconciliationRecord> findByReconciliationId(String reconciliationId);
    Optional<ReconciliationRecord> findFirstByExecutionIdOrderByCreatedAtDesc(String executionId);
    List<ReconciliationRecord> findByExecutionIdOrderByCreatedAtDesc(String executionId);
    List<ReconciliationRecord> findByStatusOrderByCreatedAtDesc(ReconciliationStatus status);
    List<ReconciliationRecord> findAllByOrderByCreatedAtDesc();
    long countByStatus(ReconciliationStatus status);
}
