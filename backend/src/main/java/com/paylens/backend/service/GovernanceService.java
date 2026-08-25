package com.paylens.backend.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.paylens.backend.dto.*;
import com.paylens.backend.exception.GovernanceConflictException;
import com.paylens.backend.model.*;
import com.paylens.backend.repository.DecisionRepository;
import jakarta.transaction.Transactional;
import java.util.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class GovernanceService {
    private final DecisionRepository decisions;
    private final AuditService audit;
    private final ObjectMapper json;

    @Autowired
    public GovernanceService(DecisionRepository decisions, AuditService audit, @Autowired(required = false) ObjectMapper json) {
        this.decisions = decisions;
        this.audit = audit;
        this.json = json != null ? json : new ObjectMapper();
    }

    @Transactional
    public GovernanceResponse record(String message, AgentFinancialIntent intent, SimulationResult simulation, PolicyEvaluationResult policy, ExplanationResponse explanation) {
        GovernanceStatus status = switch (policy.decision()) {
            case SAFE -> GovernanceStatus.SAFE;
            case REVIEW -> GovernanceStatus.PENDING_REVIEW;
            case BLOCK -> GovernanceStatus.BLOCKED;
        };
        String id = "dec_" + UUID.randomUUID();
        var d = new DecisionRecord(id, message, intent.actionType(), intent.amount(), intent.currency(), intent.target(),
                policy.decision(), status, policy.reason(), policy.recommendation(),
                explanation != null ? explanation.explanation() : null,
                explanation != null ? explanation.providerMode() : null,
                encode(intent), encode(simulation), encode(policy), explanation != null ? encode(explanation) : null);
        decisions.save(d);

        audit.record(id, AuditEventType.ACTION_ANALYZED, ActorType.SYSTEM, "paylens", "Financial action analyzed");
        audit.record(id, AuditEventType.INTENT_PARSED, ActorType.AI_AGENT, "intent-agent", "Structured intent validated");
        audit.record(id, AuditEventType.SIMULATION_COMPLETED, ActorType.SYSTEM, "paylens", "Financial impact simulated");
        audit.record(id, AuditEventType.POLICY_EVALUATED, ActorType.SYSTEM, "paylens", "Policy decision: " + policy.decision());
        if (explanation != null) {
            audit.record(id, AuditEventType.EXPLANATION_GENERATED, ActorType.AI_AGENT, "explanation-agent", "Explanation generated");
        }
        if (status == GovernanceStatus.PENDING_REVIEW) {
            audit.record(id, AuditEventType.REVIEW_REQUESTED, ActorType.SYSTEM, "paylens", "Human review requested");
        }
        if (status == GovernanceStatus.BLOCKED) {
            audit.record(id, AuditEventType.ACTION_BLOCKED, ActorType.SYSTEM, "paylens", "Action blocked by policy");
        }
        return new GovernanceResponse(id, status);
    }

    public List<DecisionSummaryResponse> list(GovernanceStatus status, int limit) {
        var list = status == null ? decisions.findAllByOrderByCreatedAtDesc() : decisions.findByStatusOrderByCreatedAtDesc(status);
        return list.stream().limit(limit).map(this::summary).toList();
    }

    public DecisionDetailResponse detail(String id) {
        var d = find(id);
        return new DecisionDetailResponse(d.getDecisionId(), d.getOriginalMessage(),
                decode(d.getIntentJson(), AgentFinancialIntent.class),
                decode(d.getSimulationJson(), SimulationResult.class),
                decode(d.getPolicyJson(), PolicyEvaluationResult.class),
                d.getExplanationJson() != null ? decode(d.getExplanationJson(), ExplanationResponse.class) : null,
                d.getStatus(), d.getCreatedAt(), d.getUpdatedAt());
    }

    @Transactional
    public GovernanceResponse review(String id, ReviewRequest req, boolean approve) {
        var d = find(id);
        if (d.getStatus() != GovernanceStatus.PENDING_REVIEW) {
            throw new GovernanceConflictException("Only pending review decisions can be " + (approve ? "approved" : "rejected"));
        }
        d.setStatus(approve ? GovernanceStatus.APPROVED : GovernanceStatus.REJECTED);
        audit.record(id, approve ? AuditEventType.REVIEW_APPROVED : AuditEventType.REVIEW_REJECTED, ActorType.HUMAN, req.actorId(), req.comment());
        return new GovernanceResponse(id, d.getStatus());
    }

    private DecisionRecord find(String id) {
        return decisions.findByDecisionId(id).orElseThrow(() -> new NoSuchElementException("Decision not found"));
    }

    private DecisionSummaryResponse summary(DecisionRecord d) {
        return new DecisionSummaryResponse(d.getDecisionId(), d.getActionType(), d.getAmount(), d.getCurrency(), d.getTarget(), d.getDecision(), d.getStatus(), d.getCreatedAt());
    }

    private String encode(Object v) {
        if (v == null) return null;
        try {
            return json.writeValueAsString(v);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Could not persist governance decision", e);
        }
    }

    private <T> T decode(String value, Class<T> type) {
        if (value == null) return null;
        try {
            return json.readValue(value, type);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Could not read governance decision", e);
        }
    }
}
