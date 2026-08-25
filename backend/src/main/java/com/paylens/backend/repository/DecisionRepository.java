package com.paylens.backend.repository;
import com.paylens.backend.model.*; import java.util.*; import org.springframework.data.jpa.repository.JpaRepository;
public interface DecisionRepository extends JpaRepository<DecisionRecord, Long> { Optional<DecisionRecord> findByDecisionId(String decisionId); List<DecisionRecord> findByStatusOrderByCreatedAtDesc(GovernanceStatus status); List<DecisionRecord> findAllByOrderByCreatedAtDesc(); }
