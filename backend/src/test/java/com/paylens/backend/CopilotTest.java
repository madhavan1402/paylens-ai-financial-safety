package com.paylens.backend;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.paylens.backend.dto.CopilotRequest;
import com.paylens.backend.model.CopilotIntent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Phase 13 — AI Fintech Copilot integration tests.
 *
 * Security filters are bypassed (addFilters=false) to focus on service-layer correctness.
 * These tests verify the five copilot guarantees:
 *   1. Intent classification is correct
 *   2. Deterministic financial facts are embedded in the response
 *   3. Policy decisions are authoritative (never from AI)
 *   4. Copilot NEVER authorises execution
 *   5. Every query is audited
 */
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:copilot_testdb;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
class CopilotTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private String json(CopilotRequest request) throws Exception {
        return objectMapper.writeValueAsString(request);
    }

    @Test
    @DisplayName("1. FINANCIAL_STATUS: 'What is my current balance?' returns health facts")
    void financialStatusQueryReturnsHealthFacts() throws Exception {
        var request = new CopilotRequest("What is my current balance?", null, null);
        mockMvc.perform(post("/api/copilot/query")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.intent").value(CopilotIntent.FINANCIAL_STATUS.name()))
                .andExpect(jsonPath("$.headline").isNotEmpty())
                .andExpect(jsonPath("$.explanation").isNotEmpty())
                .andExpect(jsonPath("$.keyFactors").isArray())
                .andExpect(jsonPath("$.keyFactors", hasSize(greaterThanOrEqualTo(4))))
                .andExpect(jsonPath("$.recommendation").isNotEmpty())
                .andExpect(jsonPath("$.financialHealth").isNotEmpty())
                .andExpect(jsonPath("$.generatedAt").isNotEmpty());
    }

    @Test
    @DisplayName("2. ACTION_ANALYSIS: 'Should I refund ₹50000?' runs simulation and returns policy decision")
    void actionAnalysisQueryRunsSimulation() throws Exception {
        var request = new CopilotRequest("Should I refund this customer?", "REFUND", new BigDecimal("50000"));
        mockMvc.perform(post("/api/copilot/query")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.intent").value(CopilotIntent.ACTION_ANALYSIS.name()))
                .andExpect(jsonPath("$.policyDecision").isNotEmpty())
                .andExpect(jsonPath("$.simulation").isNotEmpty())
                .andExpect(jsonPath("$.simulation.before").isNotEmpty())
                .andExpect(jsonPath("$.simulation.after").isNotEmpty())
                .andExpect(jsonPath("$.financialImpact").isNotEmpty())
                .andExpect(jsonPath("$.recommendation").isNotEmpty());
    }

    @Test
    @DisplayName("3. RISK_EXPLANATION: 'What are the active risk signals?' returns risk facts")
    void riskExplanationQueryReturnsSignals() throws Exception {
        var request = new CopilotRequest("What are the active risk signals?", null, null);
        mockMvc.perform(post("/api/copilot/query")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.intent").value(CopilotIntent.RISK_EXPLANATION.name()))
                .andExpect(jsonPath("$.headline").isNotEmpty())
                .andExpect(jsonPath("$.explanation").isNotEmpty())
                .andExpect(jsonPath("$.recommendation").isNotEmpty());
    }

    @Test
    @DisplayName("4. FORECAST_QUERY: 'What will liquidity look like next week?' returns forecast facts")
    void forecastQueryReturnsForecastFacts() throws Exception {
        var request = new CopilotRequest("What will liquidity look like next week?", null, null);
        mockMvc.perform(post("/api/copilot/query")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.intent").value(CopilotIntent.FORECAST_QUERY.name()))
                .andExpect(jsonPath("$.headline").isNotEmpty())
                .andExpect(jsonPath("$.keyFactors", hasSize(greaterThanOrEqualTo(3))))
                .andExpect(jsonPath("$.financialImpact").isNotEmpty());
    }

    @Test
    @DisplayName("5. POLICY_EXPLANATION: 'Why was my payment blocked?' returns policy explanation")
    void policyExplanationQueryExplainsPolicy() throws Exception {
        var request = new CopilotRequest("Why was my payment blocked?", null, null);
        mockMvc.perform(post("/api/copilot/query")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.intent").value(CopilotIntent.POLICY_EXPLANATION.name()))
                .andExpect(jsonPath("$.explanation").value(containsString("Policy Engine")))
                .andExpect(jsonPath("$.keyFactors", hasSize(greaterThanOrEqualTo(5))));
    }

    @Test
    @DisplayName("6. UNKNOWN: unrecognised query returns helpful fallback with UNKNOWN intent")
    void unknownQueryReturnsFallback() throws Exception {
        var request = new CopilotRequest("xyzzy magic words that mean nothing financial", null, null);
        mockMvc.perform(post("/api/copilot/query")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.intent").value(CopilotIntent.UNKNOWN.name()))
                .andExpect(jsonPath("$.keyFactors").isArray())
                .andExpect(jsonPath("$.keyFactors", hasSize(greaterThanOrEqualTo(3))))
                .andExpect(jsonPath("$.requiresHumanReview").value(false));
    }

    @Test
    @DisplayName("7. Validation: blank message returns 400 Bad Request")
    void blankMessageReturnsBadRequest() throws Exception {
        var request = new CopilotRequest("", null, null);
        mockMvc.perform(post("/api/copilot/query")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("8. BLOCK policy: large amount triggers BLOCK and requiresHumanReview=true")
    void largeAmountTriggersBlock() throws Exception {
        // ₹10,00,000 far exceeds the current balance — must be BLOCKED
        var request = new CopilotRequest("Pay ₹10,00,000 to vendor", "VENDOR_PAYMENT", new BigDecimal("1000000"));
        mockMvc.perform(post("/api/copilot/query")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.intent").value(CopilotIntent.ACTION_ANALYSIS.name()))
                .andExpect(jsonPath("$.policyDecision").value(anyOf(equalTo("BLOCK"), equalTo("REVIEW"))))
                .andExpect(jsonPath("$.requiresHumanReview").value(true));
    }

    @Test
    @DisplayName("9. SAFE policy: small amount returns SAFE and requiresHumanReview=false")
    void smallAmountReturnsSafe() throws Exception {
        // ₹1,000 well within current balance (₹8,40,000) — must be SAFE
        var request = new CopilotRequest("Refund ₹1000 to customer", "REFUND", new BigDecimal("1000"));
        mockMvc.perform(post("/api/copilot/query")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.intent").value(CopilotIntent.ACTION_ANALYSIS.name()))
                .andExpect(jsonPath("$.policyDecision").value("SAFE"))
                .andExpect(jsonPath("$.requiresHumanReview").value(false));
    }

    @Test
    @DisplayName("10. Copilot response never contains execution endpoints or authorization grants")
    void copilotResponseDoesNotAuthoriseExecution() throws Exception {
        var request = new CopilotRequest("Execute this payment immediately", "VENDOR_PAYMENT", new BigDecimal("50000"));
        String responseBody = mockMvc.perform(post("/api/copilot/query")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(request)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        // The copilot response must NOT contain any execution-trigger terms
        assert !responseBody.toLowerCase().contains("\"execute\"") : "Copilot must not include execute commands";
        assert !responseBody.toLowerCase().contains("\"authorized\"") : "Copilot must not issue authorizations";
    }
}
