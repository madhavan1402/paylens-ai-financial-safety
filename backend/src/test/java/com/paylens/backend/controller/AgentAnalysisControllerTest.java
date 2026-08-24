package com.paylens.backend.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.paylens.backend.dto.AgentAnalyzeRequest;
import com.paylens.backend.dto.AgentFinancialIntent;
import com.paylens.backend.dto.IntentAgentResponse;
import com.paylens.backend.repository.InMemoryFinancialStateRepository;
import com.paylens.backend.service.AgentAnalysisService;
import com.paylens.backend.service.DeterministicExplanationService;
import com.paylens.backend.service.ExplanationAgentClient;
import com.paylens.backend.service.FinancialStateService;
import com.paylens.backend.service.IntentAgentClient;
import com.paylens.backend.service.PolicyService;
import com.paylens.backend.service.PolicyThresholds;
import com.paylens.backend.service.SimulationService;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AgentAnalysisController.class)
@Import({
        AgentAnalysisService.class, SimulationService.class, PolicyService.class, PolicyThresholds.class,
        FinancialStateService.class, InMemoryFinancialStateRepository.class, DeterministicExplanationService.class,
        AgentAnalysisControllerTest.AgentClientConfig.class
})
class AgentAnalysisControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @Test
    void connectsValidIntentToSimulationAndPolicy() throws Exception {
        assertDecision("safe", "VALID", "SAFE");
        assertDecision("review", "VALID", "REVIEW");
        assertDecision("block", "VALID", "BLOCK");
    }

    @Test
    void returnsClarificationAndInvalidResponsesWithoutSimulation() throws Exception {
        mockMvc.perform(request("clarify"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("NEEDS_CLARIFICATION"))
                .andExpect(jsonPath("$.missingFields[0]").value("amount"))
                .andExpect(jsonPath("$.simulation").doesNotExist());
        mockMvc.perform(request("invalid"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("INVALID"))
                .andExpect(jsonPath("$.simulation").doesNotExist());
    }

    private void assertDecision(String message, String intentStatus, String decision) throws Exception {
        mockMvc.perform(request(message))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(intentStatus))
                .andExpect(jsonPath("$.policy.decision").value(decision))
                .andExpect(jsonPath("$.explanation.decision").value(decision));
    }

    private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder request(String message) {
        return post("/api/agent/analyze").contentType(MediaType.APPLICATION_JSON)
                .content("{\"message\":\"" + message + "\"}");
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class AgentClientConfig {
        @Bean
        IntentAgentClient intentAgentClient() {
            return request -> switch (request.message()) {
                case "clarify" -> new IntentAgentResponse("NEEDS_CLARIFICATION", null, BigDecimal.ZERO,
                        List.of("amount"), "Please provide the refund amount.", "deterministic");
                case "invalid" -> new IntentAgentResponse("INVALID", null, BigDecimal.ZERO,
                        List.of(), "The requested financial action could not be understood.", "deterministic");
                case "safe" -> validIntent("20000");
                case "review" -> validIntent("50000");
                default -> validIntent("250000");
            };
        }

        @Bean
        ExplanationAgentClient explanationAgentClient() {
            return request -> { throw new IllegalStateException("agent unavailable"); };
        }

        private IntentAgentResponse validIntent(String amount) {
            return new IntentAgentResponse("VALID", new AgentFinancialIntent("REFUND", new BigDecimal(amount),
                    "INR", "Rahul", "Refund to Rahul"), BigDecimal.ONE, List.of(), null, "deterministic");
        }
    }
}
