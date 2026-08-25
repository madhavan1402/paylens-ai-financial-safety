package com.paylens.backend;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.paylens.backend.dto.AgentFinancialIntent;
import com.paylens.backend.dto.IntentAgentResponse;
import com.paylens.backend.dto.ReviewRequest;
import com.paylens.backend.model.ActorType;
import com.paylens.backend.model.AuditEventType;
import com.paylens.backend.model.GovernanceStatus;
import com.paylens.backend.model.PolicyDecision;
import com.paylens.backend.repository.AuditEventRepository;
import com.paylens.backend.repository.DecisionRepository;
import com.paylens.backend.service.IntentAgentClient;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Import(GovernanceIntegrationTest.TestAgentConfig.class)
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
class GovernanceIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private DecisionRepository decisionRepository;

    @Autowired
    private AuditEventRepository auditEventRepository;

    @BeforeEach
    void setUp() {
        auditEventRepository.deleteAll();
        decisionRepository.deleteAll();
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class TestAgentConfig {
        @Bean
        IntentAgentClient intentAgentClient() {
            return request -> {
                String msg = request.message();
                if (msg.contains("80,000") || msg.contains("80000")) {
                    return new IntentAgentResponse("VALID", new AgentFinancialIntent("VENDOR_PAYMENT", new BigDecimal("80000"), "INR", "ABC Suppliers", "Pay ABC Suppliers"), BigDecimal.ONE, List.of(), null, "deterministic");
                }
                if (msg.contains("2,50,000") || msg.contains("250000")) {
                    return new IntentAgentResponse("VALID", new AgentFinancialIntent("REFUND", new BigDecimal("250000"), "INR", "Rahul", "Refund to Rahul"), BigDecimal.ONE, List.of(), null, "deterministic");
                }
                if (msg.contains("500")) {
                    return new IntentAgentResponse("VALID", new AgentFinancialIntent("VENDOR_PAYMENT", new BigDecimal("500"), "INR", "Coffee Shop", "Coffee"), BigDecimal.ONE, List.of(), null, "deterministic");
                }
                return new IntentAgentResponse("VALID", new AgentFinancialIntent("VENDOR_PAYMENT", new BigDecimal("1000"), "INR", "Vendor", "Office supplies"), BigDecimal.ONE, List.of(), null, "deterministic");
            };
        }
    }

    @Test
    @DisplayName("1. SAFE decision creates SAFE governance status and records audit events")
    void safeCreatesSafeDecision() throws Exception {
        String json = """
                {
                  "message": "Pay ₹1000 to vendor for supplies"
                }
                """;

        String response = mockMvc.perform(post("/api/agent/analyze")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.governance.status").value("SAFE"))
                .andExpect(jsonPath("$.governance.decisionId").exists())
                .andReturn().getResponse().getContentAsString();

        var root = objectMapper.readTree(response);
        String decisionId = root.get("governance").get("decisionId").asText();

        var record = decisionRepository.findByDecisionId(decisionId).orElseThrow();
        assertEquals(GovernanceStatus.SAFE, record.getStatus());
        assertEquals(PolicyDecision.SAFE, record.getDecision());

        var events = auditEventRepository.findByDecisionIdOrderByCreatedAtDesc(decisionId);
        assertFalse(events.isEmpty());
        assertTrue(events.stream().anyMatch(e -> e.getEventType() == AuditEventType.ACTION_ANALYZED));
    }

    @Test
    @DisplayName("2 & 6 & 14. REVIEW creates PENDING_REVIEW and can be approved producing REVIEW_APPROVED event")
    void reviewCreatesPendingReviewAndCanBeApproved() throws Exception {
        String json = """
                {
                  "message": "Pay ₹80,000 to ABC Suppliers"
                }
                """;

        String response = mockMvc.perform(post("/api/agent/analyze")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.governance.status").value("PENDING_REVIEW"))
                .andReturn().getResponse().getContentAsString();

        String decisionId = objectMapper.readTree(response).get("governance").get("decisionId").asText();

        // Check pending status in GET /api/decisions/{id}
        mockMvc.perform(get("/api/decisions/" + decisionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDING_REVIEW"));

        // Approve PENDING_REVIEW
        ReviewRequest approveReq = new ReviewRequest("demo-user", "Approved after review.");
        mockMvc.perform(post("/api/decisions/" + decisionId + "/approve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(approveReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"));

        // Verify updated status
        var record = decisionRepository.findByDecisionId(decisionId).orElseThrow();
        assertEquals(GovernanceStatus.APPROVED, record.getStatus());

        // Verify REVIEW_APPROVED audit event created
        var events = auditEventRepository.findByDecisionIdOrderByCreatedAtDesc(decisionId);
        assertTrue(events.stream().anyMatch(e -> e.getEventType() == AuditEventType.REVIEW_APPROVED
                && "demo-user".equals(e.getActorId())
                && e.getActorType() == ActorType.HUMAN));
    }

    @Test
    @DisplayName("7 & 15. REVIEW can be rejected with mandatory comment producing REVIEW_REJECTED event")
    void reviewCanBeRejected() throws Exception {
        String json = """
                {
                  "message": "Pay ₹80,000 to ABC Suppliers"
                }
                """;

        String response = mockMvc.perform(post("/api/agent/analyze")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        String decisionId = objectMapper.readTree(response).get("governance").get("decisionId").asText();

        // Reject PENDING_REVIEW
        ReviewRequest rejectReq = new ReviewRequest("demo-user", "Rejected to preserve liquidity.");
        mockMvc.perform(post("/api/decisions/" + decisionId + "/reject")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(rejectReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REJECTED"));

        var record = decisionRepository.findByDecisionId(decisionId).orElseThrow();
        assertEquals(GovernanceStatus.REJECTED, record.getStatus());

        var events = auditEventRepository.findByDecisionIdOrderByCreatedAtDesc(decisionId);
        assertTrue(events.stream().anyMatch(e -> e.getEventType() == AuditEventType.REVIEW_REJECTED
                && "demo-user".equals(e.getActorId())
                && e.getActorType() == ActorType.HUMAN));
    }

    @Test
    @DisplayName("3 & 8 & 9 & 16. BLOCK creates BLOCKED and ACTION_BLOCKED audit event; BLOCK cannot be approved or rejected")
    void blockCreatesBlockedAndCannotBeApprovedOrRejected() throws Exception {
        String json = """
                {
                  "message": "Refund ₹2,50,000 to Rahul"
                }
                """;

        String response = mockMvc.perform(post("/api/agent/analyze")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.governance.status").value("BLOCKED"))
                .andReturn().getResponse().getContentAsString();

        String decisionId = objectMapper.readTree(response).get("governance").get("decisionId").asText();

        // Check ACTION_BLOCKED event present
        var events = auditEventRepository.findByDecisionIdOrderByCreatedAtDesc(decisionId);
        assertTrue(events.stream().anyMatch(e -> e.getEventType() == AuditEventType.ACTION_BLOCKED));

        ReviewRequest request = new ReviewRequest("demo-user", "Bypass attempt");

        // BLOCK cannot be approved (409 Conflict)
        mockMvc.perform(post("/api/decisions/" + decisionId + "/approve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());

        // BLOCK cannot be rejected (409 Conflict)
        mockMvc.perform(post("/api/decisions/" + decisionId + "/reject")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("10 & 11. SAFE cannot be approved or rejected")
    void safeCannotBeApprovedOrRejected() throws Exception {
        String json = """
                {
                  "message": "Pay ₹500 for coffee"
                }
                """;

        String response = mockMvc.perform(post("/api/agent/analyze")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.governance.status").value("SAFE"))
                .andReturn().getResponse().getContentAsString();

        String decisionId = objectMapper.readTree(response).get("governance").get("decisionId").asText();
        ReviewRequest req = new ReviewRequest("demo-user", "Approve safe");

        mockMvc.perform(post("/api/decisions/" + decisionId + "/approve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isConflict());

        mockMvc.perform(post("/api/decisions/" + decisionId + "/reject")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("12 & 13. Terminal states APPROVED and REJECTED cannot transition")
    void terminalStatesCannotTransition() throws Exception {
        String json = """
                {
                  "message": "Pay ₹80,000 to ABC Suppliers"
                }
                """;

        String response = mockMvc.perform(post("/api/agent/analyze")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        String decisionId = objectMapper.readTree(response).get("governance").get("decisionId").asText();

        // Approve
        ReviewRequest req = new ReviewRequest("demo-user", "First approval");
        mockMvc.perform(post("/api/decisions/" + decisionId + "/approve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk());

        // Attempting APPROVED -> APPROVED fails (409)
        mockMvc.perform(post("/api/decisions/" + decisionId + "/approve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isConflict());

        // Attempting APPROVED -> REJECTED fails (409)
        mockMvc.perform(post("/api/decisions/" + decisionId + "/reject")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("4 & 5. GET /api/decisions and GET /api/decisions/{id} return expected data")
    void decisionsQueryAndDetailWork() throws Exception {
        // Create two decisions
        mockMvc.perform(post("/api/agent/analyze")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"Pay ₹1000 for office supplies\"}"))
                .andExpect(status().isOk());

        String resp2 = mockMvc.perform(post("/api/agent/analyze")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"Refund ₹2,50,000 to Rahul\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        String dec2Id = objectMapper.readTree(resp2).get("governance").get("decisionId").asText();

        // GET /api/decisions
        mockMvc.perform(get("/api/decisions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.decisions", hasSize(2)));

        // GET /api/decisions?status=BLOCKED
        mockMvc.perform(get("/api/decisions").param("status", "BLOCKED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.decisions", hasSize(1)))
                .andExpect(jsonPath("$.decisions[0].decisionId").value(dec2Id));

        // GET /api/decisions/{id}
        mockMvc.perform(get("/api/decisions/" + dec2Id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.decisionId").value(dec2Id))
                .andExpect(jsonPath("$.status").value("BLOCKED"))
                .andExpect(jsonPath("$.intent.actionType").value("REFUND"))
                .andExpect(jsonPath("$.intent.amount").value(250000))
                .andExpect(jsonPath("$.simulation").exists())
                .andExpect(jsonPath("$.policy").exists());
    }

    @Test
    @DisplayName("17 & 18 & 19. Audit events are persisted and append-only (no modification endpoints exist)")
    void auditEventsPersistedAndAppendOnly() throws Exception {
        String resp = mockMvc.perform(post("/api/agent/analyze")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"Pay ₹500 for coffee\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        String decisionId = objectMapper.readTree(resp).get("governance").get("decisionId").asText();

        // GET /api/audit
        mockMvc.perform(get("/api/audit"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.events", hasSize(greaterThanOrEqualTo(5))));

        // GET /api/audit?decisionId=...
        mockMvc.perform(get("/api/audit").param("decisionId", decisionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.events", hasSize(5)));

        // Ensure PUT/PATCH/DELETE on audit return 4xx Client Error (405 Method Not Allowed or 404 Not Found)
        mockMvc.perform(put("/api/audit"))
                .andExpect(status().is4xxClientError());

        mockMvc.perform(patch("/api/audit"))
                .andExpect(status().is4xxClientError());

        mockMvc.perform(delete("/api/audit"))
                .andExpect(status().is4xxClientError());
    }

    @Test
    @DisplayName("20. Invalid decision ID returns 404 Not Found")
    void invalidDecisionIdReturns404() throws Exception {
        mockMvc.perform(get("/api/decisions/dec_non_existent_123"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Decision not found"));
    }
}
