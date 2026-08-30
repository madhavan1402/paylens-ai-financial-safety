package com.paylens.backend.repository;

import com.paylens.backend.model.AuditEvent;
import com.paylens.backend.model.AuditEventType;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface AuditEventRepository extends JpaRepository<AuditEvent, Long> {
    List<AuditEvent> findByDecisionIdOrderByCreatedAtDesc(String decisionId);
    List<AuditEvent> findByEventTypeOrderByCreatedAtDesc(AuditEventType type);
    List<AuditEvent> findAllByOrderByCreatedAtDesc();
}
