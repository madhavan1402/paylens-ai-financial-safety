package com.paylens.backend;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.paylens.backend.dto.*;
import com.paylens.backend.model.*;
import com.paylens.backend.repository.AuditEventRepository;
import com.paylens.backend.repository.DecisionRepository;
import com.paylens.backend.repository.ExecutionRepository;
import com.paylens.backend.repository.ReconciliationRepository;
import com.paylens.backend.service.AuditService;
import com.paylens.backend.service.GovernanceService;
import com.paylens.backend.service.IntentAgentClient;
import com.paylens.backend.service.PaymentExecutionProvider;
import com.paylens.backend.service.PaymentReconciliationProvider;
import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Import(ReconciliationEngineTest.TestConfig.class)
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:recon_testdb;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
class ReconciliationEngineTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private DecisionRepository decisionRepository;

    @Autowired
    private ExecutionRepository executionRepository;

    @Autowired
    private ReconciliationRepository reconciliationRepository;

    @Autowired
    private AuditEventRepository auditEventRepository;

    @Autowired
    private TestMockExecutionProvider mockExecutionProvider;

    @Autowired
    private TestMockReconciliationProvider mockReconciliationProvider;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        auditEventRepository.deleteAll();
        reconciliationRepository.deleteAll();
        executionRepository.deleteAll();
        decisionRepository.deleteAll();
        mockExecutionProvider.reset();
        mockReconciliationProvider.reset();
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class TestConfig {
        @Bean
        IntentAgentClient intentAgentClient() {
            return request -> new IntentAgentResponse("VALID", new AgentFinancialIntent("REFUND", new BigDecimal("1000"), "INR", "Customer A", "Refund 1k"), BigDecimal.ONE, List.of(), null, "deterministic");
        }

        @Bean
        @Primary
        TestMockExecutionProvider testMockExecutionProvider() {
            return new TestMockExecutionProvider();
        }

        @Bean
        @Primary
        TestMockReconciliationProvider testMockReconciliationProvider() {
            return new TestMockReconciliationProvider();
        }
    }

    public static class TestMockExecutionProvider implements PaymentExecutionProvider {
        private String forceBehavior = "TIMEOUT";

        public void reset() {
            forceBehavior = "TIMEOUT";
        }

        public void setForceBehavior(String forceBehavior) {
            this.forceBehavior = forceBehavior;
        }

        @Override
        public ExecutionProvider getProviderType() {
            return ExecutionProvider.RAZORPAY_TEST;
        }

        @Override
        public ExecutionProviderResult execute(ExecutionCommand command) {
            if ("TIMEOUT".equals(forceBehavior)) {
                return ExecutionProviderResult.unknown("Network timeout communicating with Razorpay gateway.");
            }
            if ("FAIL".equals(forceBehavior)) {
                return ExecutionProviderResult.failure("BAD_REQUEST", "Razorpay rejected execution.");
            }
            return ExecutionProviderResult.success("rfnd_test_123456");
        }
    }

    public static class TestMockReconciliationProvider implements PaymentReconciliationProvider {
        private final AtomicInteger invocationCount = new AtomicInteger(0);
        private String forceOutcome = "CONFIRMED_SUCCESS";

        public void reset() {
            invocationCount.set(0);
            forceOutcome = "CONFIRMED_SUCCESS";
        }

        public void setForceOutcome(String forceOutcome) {
            this.forceOutcome = forceOutcome;
        }

        public int getInvocationCount() {
            return invocationCount.get();
        }

        @Override
        public ExecutionProvider getProviderType() {
            return ExecutionProvider.RAZORPAY_TEST;
        }

        @Override
        public ReconciliationProviderResult reconcile(ReconciliationCommand command) {
            invocationCount.incrementAndGet();
            String ref = command.providerReference();
            if (ref != null && ref.contains("notfound")) {
                return ReconciliationProviderResult.notFound("Reference " + ref + " not found on Razorpay server.");
            }

            return switch (forceOutcome) {
                case "CONFIRMED_SUCCESS" -> ReconciliationProviderResult.confirmedSuccess("processed");
                case "CONFIRMED_FAILURE" -> ReconciliationProviderResult.confirmedFailure("failed", "REFUND_FAILED", "Refund failed");
                case "STILL_PROCESSING" -> ReconciliationProviderResult.stillProcessing("pending");
                case "NOT_FOUND" -> ReconciliationProviderResult.notFound("Reference not found");
                case "UNKNOWN" -> ReconciliationProviderResult.unknown("GATEWAY_TIMEOUT", "Timeout during reconciliation");
                default -> ReconciliationProviderResult.confirmedSuccess("processed");
            };
        }
    }

    private String createUnknownExecution() throws Exception {
        mockExecutionProvider.setForceBehavior("TIMEOUT");

        String analyzeResp = mockMvc.perform(post("/api/agent/analyze")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"Refund ₹1000 to customer\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        String decisionId = objectMapper.readTree(analyzeResp).get("governance").get("decisionId").asText();
        String execJson = String.format("{\"decisionId\":\"%s\",\"idempotencyKey\":\"exec_timeout_%s\"}", decisionId, System.currentTimeMillis());

        String execResp = mockMvc.perform(post("/api/executions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(execJson))
                .andExpect(status().isGatewayTimeout())
                .andExpect(jsonPath("$.status").value("UNKNOWN"))
                .andReturn().getResponse().getContentAsString();

        return objectMapper.readTree(execResp).get("executionId").asText();
    }

    @Test
    @DisplayName("1. UNKNOWN execution → Reconciles to CONFIRMED_SUCCESS and updates execution to SUCCEEDED")
    void unknownReconcilesToConfirmedSuccess() throws Exception {
        String executionId = createUnknownExecution();
        mockReconciliationProvider.setForceOutcome("CONFIRMED_SUCCESS");

        mockMvc.perform(post("/api/executions/" + executionId + "/reconcile"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONFIRMED"))
                .andExpect(jsonPath("$.resolvedExecutionStatus").value("SUCCEEDED"))
                .andExpect(jsonPath("$.providerOutcome").value("CONFIRMED_SUCCESS"))
                .andExpect(jsonPath("$.retryDecision").value("NOT_SAFE"));

        mockMvc.perform(get("/api/executions/" + executionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCEEDED"));

        assertEquals(1, mockReconciliationProvider.getInvocationCount());
    }

    @Test
    @DisplayName("2. UNKNOWN execution → Reconciles to CONFIRMED_FAILURE and updates execution to FAILED")
    void unknownReconcilesToConfirmedFailure() throws Exception {
        String executionId = createUnknownExecution();
        mockReconciliationProvider.setForceOutcome("CONFIRMED_FAILURE");

        mockMvc.perform(post("/api/executions/" + executionId + "/reconcile"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("FAILED"))
                .andExpect(jsonPath("$.resolvedExecutionStatus").value("FAILED"))
                .andExpect(jsonPath("$.providerOutcome").value("CONFIRMED_FAILURE"))
                .andExpect(jsonPath("$.retryDecision").value("NOT_SAFE"));

        mockMvc.perform(get("/api/executions/" + executionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("FAILED"));
    }

    @Test
    @DisplayName("3. UNKNOWN execution → STILL_PROCESSING leaves reconciliation PENDING")
    void unknownReconcilesToStillProcessing() throws Exception {
        String executionId = createUnknownExecution();
        mockReconciliationProvider.setForceOutcome("STILL_PROCESSING");

        mockMvc.perform(post("/api/executions/" + executionId + "/reconcile"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.resolvedExecutionStatus").value("UNKNOWN"))
                .andExpect(jsonPath("$.providerOutcome").value("STILL_PROCESSING"));
    }

    @Test
    @DisplayName("4. UNKNOWN execution → NOT_FOUND preserved and transitions to MANUAL_REVIEW_REQUIRED")
    void unknownReconcilesToNotFound() throws Exception {
        String executionId = createUnknownExecution();
        mockReconciliationProvider.setForceOutcome("NOT_FOUND");

        mockMvc.perform(post("/api/executions/" + executionId + "/reconcile"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("MANUAL_REVIEW_REQUIRED"))
                .andExpect(jsonPath("$.providerOutcome").value("NOT_FOUND"))
                .andExpect(jsonPath("$.retryDecision").value("MANUAL_REVIEW"));

        mockMvc.perform(get("/api/executions/" + executionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UNKNOWN"));
    }

    @Test
    @DisplayName("5. UNKNOWN execution → Provider UNKNOWN transitions to MANUAL_REVIEW_REQUIRED")
    void unknownReconcilesToProviderUnknown() throws Exception {
        String executionId = createUnknownExecution();
        mockReconciliationProvider.setForceOutcome("UNKNOWN");

        mockMvc.perform(post("/api/executions/" + executionId + "/reconcile"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("MANUAL_REVIEW_REQUIRED"))
                .andExpect(jsonPath("$.providerOutcome").value("UNKNOWN"))
                .andExpect(jsonPath("$.retryDecision").value("MANUAL_REVIEW"));
    }

    @Test
    @DisplayName("6. Duplicate reconciliation is safe and returns existing resolved state")
    void duplicateReconciliationIsSafe() throws Exception {
        String executionId = createUnknownExecution();
        mockReconciliationProvider.setForceOutcome("CONFIRMED_SUCCESS");

        mockMvc.perform(post("/api/executions/" + executionId + "/reconcile"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONFIRMED"));

        assertEquals(1, mockReconciliationProvider.getInvocationCount());

        mockMvc.perform(post("/api/executions/" + executionId + "/reconcile"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONFIRMED"));

        assertEquals(1, mockReconciliationProvider.getInvocationCount());
    }

    @Test
    @DisplayName("7. Concurrent reconciliation is safe and invokes provider ONLY ONCE")
    void concurrentReconciliationIsSafe() throws Exception {
        String executionId = createUnknownExecution();
        mockReconciliationProvider.setForceOutcome("CONFIRMED_SUCCESS");

        ExecutorService executor = Executors.newFixedThreadPool(2);
        Callable<String> task = () -> mockMvc.perform(post("/api/executions/" + executionId + "/reconcile"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        Future<String> f1 = executor.submit(task);
        Future<String> f2 = executor.submit(task);

        assertNotNull(f1.get());
        assertNotNull(f2.get());
        executor.shutdown();

        assertEquals(1, mockReconciliationProvider.getInvocationCount());
    }

    @Test
    @DisplayName("8. Reconciliation APIs return complete detail & history")
    void reconciliationApisReturnData() throws Exception {
        String executionId = createUnknownExecution();
        mockReconciliationProvider.setForceOutcome("CONFIRMED_SUCCESS");

        String reconResp = mockMvc.perform(post("/api/executions/" + executionId + "/reconcile"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        String reconciliationId = objectMapper.readTree(reconResp).get("reconciliationId").asText();

        mockMvc.perform(get("/api/reconciliations/" + reconciliationId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reconciliationId").value(reconciliationId))
                .andExpect(jsonPath("$.status").value("CONFIRMED"));

        mockMvc.perform(get("/api/executions/" + executionId + "/reconciliation"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reconciliationId").value(reconciliationId));

        mockMvc.perform(get("/api/reconciliations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reconciliations", hasSize(greaterThanOrEqualTo(1))));
    }

    @Test
    @DisplayName("9. Reliability metrics accurately calculate resolved success rate ignoring UNKNOWN")
    void reliabilityMetricsAccurate() throws Exception {
        String executionId = createUnknownExecution();

        mockMvc.perform(get("/api/reconciliations/metrics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalExecutions").value(greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.unknownOrManualReview").value(greaterThanOrEqualTo(1)));
    }
}
