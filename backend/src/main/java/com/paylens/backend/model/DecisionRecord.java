package com.paylens.backend.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "governance_decisions")
public class DecisionRecord {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(unique = true, nullable = false, updatable = false) private String decisionId;
    @Column(nullable = false, length = 2000) private String originalMessage;
    private String actionType; private BigDecimal amount; private String currency; private String target;
    @Enumerated(EnumType.STRING) private PolicyDecision decision;
    @Enumerated(EnumType.STRING) private GovernanceStatus status;
    @Column(length = 2000) private String policyReason; @Column(length = 2000) private String recommendation;
    @Column(length = 4000) private String explanation; private String providerMode;
    @Lob private String intentJson; @Lob private String simulationJson; @Lob private String policyJson; @Lob private String explanationJson;
    private Instant createdAt; private Instant updatedAt;
    protected DecisionRecord() {}
    public DecisionRecord(String decisionId, String message, String actionType, BigDecimal amount, String currency, String target, PolicyDecision decision, GovernanceStatus status, String reason, String recommendation, String explanation, String providerMode, String intentJson, String simulationJson, String policyJson, String explanationJson) { this.decisionId=decisionId; this.originalMessage=message; this.actionType=actionType; this.amount=amount; this.currency=currency; this.target=target; this.decision=decision; this.status=status; this.policyReason=reason; this.recommendation=recommendation; this.explanation=explanation; this.providerMode=providerMode; this.intentJson=intentJson; this.simulationJson=simulationJson; this.policyJson=policyJson; this.explanationJson=explanationJson; this.createdAt=Instant.now(); this.updatedAt=this.createdAt; }
    @PreUpdate void touch(){updatedAt=Instant.now();}
    public String getDecisionId(){return decisionId;} public String getOriginalMessage(){return originalMessage;} public String getActionType(){return actionType;} public BigDecimal getAmount(){return amount;} public String getCurrency(){return currency;} public String getTarget(){return target;} public PolicyDecision getDecision(){return decision;} public GovernanceStatus getStatus(){return status;} public String getPolicyReason(){return policyReason;} public String getRecommendation(){return recommendation;} public String getExplanation(){return explanation;} public String getProviderMode(){return providerMode;} public String getIntentJson(){return intentJson;} public String getSimulationJson(){return simulationJson;} public String getPolicyJson(){return policyJson;} public String getExplanationJson(){return explanationJson;} public Instant getCreatedAt(){return createdAt;} public Instant getUpdatedAt(){return updatedAt;} public void setStatus(GovernanceStatus value){status=value;}
}
