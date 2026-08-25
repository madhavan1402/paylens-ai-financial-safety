package com.paylens.backend.dto;
import com.paylens.backend.model.*; import java.time.Instant;
public record AuditEventResponse(String eventId,String decisionId,AuditEventType eventType,ActorType actorType,String actorId,String description,Instant createdAt) {}
