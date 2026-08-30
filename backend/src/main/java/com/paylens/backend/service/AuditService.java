package com.paylens.backend.service;

import com.paylens.backend.dto.AuditEventResponse;
import com.paylens.backend.model.*;
import com.paylens.backend.repository.AuditEventRepository;
import com.paylens.backend.security.UserPrincipal;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class AuditService {

    private final AuditEventRepository repository;

    public AuditService(AuditEventRepository repository) {
        this.repository = repository;
    }

    public void record(String decisionId, AuditEventType type, ActorType actorType, String actorId, String description) {
        ActorType resolvedActorType = actorType;
        String resolvedActorId = actorId;

        // If authenticated user context exists and not explicitly SYSTEM/AI_AGENT, derive identity from Spring Security
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof UserPrincipal principal) {
            if (actorType != ActorType.SYSTEM && actorType != ActorType.AI_AGENT) {
                resolvedActorType = ActorType.HUMAN;
                resolvedActorId = principal.getUserId();
            }
        }

        repository.save(new AuditEvent(
                "evt_" + UUID.randomUUID(), decisionId, type,
                resolvedActorType != null ? resolvedActorType : ActorType.SYSTEM,
                resolvedActorId != null ? resolvedActorId : "system",
                description, "{}"
        ));
    }

    public List<AuditEventResponse> list(String decisionId) {
        var events = decisionId == null ? repository.findAllByOrderByCreatedAtDesc() : repository.findByDecisionIdOrderByCreatedAtDesc(decisionId);
        return events.stream()
                .map(e -> new AuditEventResponse(
                        e.getEventId(), e.getDecisionId(), e.getEventType(),
                        e.getActorType(), e.getActorId(), e.getDescription(), e.getCreatedAt()
                ))
                .toList();
    }
}
