package com.paylens.backend.repository;
import com.paylens.backend.model.AuditEvent; import java.util.*; import org.springframework.data.jpa.repository.JpaRepository;
public interface AuditEventRepository extends JpaRepository<AuditEvent, Long> { List<AuditEvent> findByDecisionIdOrderByCreatedAtDesc(String decisionId); List<AuditEvent> findAllByOrderByCreatedAtDesc(); }
