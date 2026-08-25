package com.paylens.backend.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "audit_events")
public class AuditEvent {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(unique = true, nullable = false, updatable = false) private String eventId;
    @Column(nullable = false, updatable = false) private String decisionId;
    @Enumerated(EnumType.STRING) private AuditEventType eventType;
    @Enumerated(EnumType.STRING) private ActorType actorType;
    private String actorId; @Column(length = 2000) private String description; @Lob private String metadata; private Instant createdAt;
    protected AuditEvent() {} public AuditEvent(String eventId,String decisionId,AuditEventType type,ActorType actorType,String actorId,String description,String metadata){this.eventId=eventId;this.decisionId=decisionId;this.eventType=type;this.actorType=actorType;this.actorId=actorId;this.description=description;this.metadata=metadata;this.createdAt=Instant.now();}
    public String getEventId(){return eventId;} public String getDecisionId(){return decisionId;} public AuditEventType getEventType(){return eventType;} public ActorType getActorType(){return actorType;} public String getActorId(){return actorId;} public String getDescription(){return description;} public Instant getCreatedAt(){return createdAt;}
}
