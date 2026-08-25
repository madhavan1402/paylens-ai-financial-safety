package com.paylens.backend;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.paylens.backend.dto.*;
import com.paylens.backend.model.*;
import com.paylens.backend.repository.AuditEventRepository;
import com.paylens.backend.repository.DecisionRepository;
import com.paylens.backend.repository.ExecutionRepository;
import com.paylens.backend.service.AuditService;
import com.paylens.backend.service.GovernanceService;
import com.paylens.backend.service.IntentAgentClient;
import com.paylens.backend.service.PaymentExecutionProvider;
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
@Import(ExecutionGatewayTest.TestConfig.class)
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:exec_testdb;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
class ExecutionGatewayTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private DecisionRepository decisionRepository;

    @Autowired
    private ExecutionRepository executionRepository;

    @Autowired
    private AuditEventRepository auditEventRepository;

    @Autowired
    private GovernanceService governanceService;

    @Autowired
    private AuditService auditService;

    @Autowired
    private TestMockExecutionProvider mockExecutionProvider;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        auditEventRepository.deleteAll();
        executionRepository.deleteAll();
        decisionRepository.deleteAll();
        mockExecutionProvider.reset();
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class TestConfig {
        @Bean
        IntentAgentClient intentAgentClient() {
            return request -> {
                String msg = request.message();
                if (msg.contains("80,000") || msg.contains("80000")) {
                    return new IntentAgentResponse("VALID", new AgentFinancialIntent("REFUND", new BigDecimal("80000"), "INR", "Customer A", "Refund for Order 80k"), BigDecimal.ONE, List.of(), null, "deterministic");
                }
                if (msg.contains("Vendor") || msg.contains("vendor")) {
                    return new IntentAgentResponse("VALID", new AgentFinancialIntent("VENDOR_PAYMENT", new BigDecimal("10000"), "INR", "ABC Suppliers", "Vendor payment"), BigDecimal.ONE, List.of(), null, "deterministic");
                }
                if (msg.contains("Payroll") || msg.contains("payroll")) {
                    return new IntentAgentResponse("VALID", new AgentFinancialIntent("PAYROLL", new BigDecimal("5000"), "INR", "Employees", "Monthly payroll"), BigDecimal.ONE, List.of(), null, "deterministic");
                }
                if (msg.contains("2,50,000") || msg.contains("250000")) {
                    return new IntentAgentResponse("VALID", new AgentFinancialIntent("REFUND", new BigDecimal("250000"), "INR", "Rahul", "Refund to Rahul"), BigDecimal.ONE, List.of(), null, "deterministic");
                }
                return new IntentAgentResponse("VALID", new AgentFinancialIntent("REFUND", new BigDecimal("1000"), "INR", "Customer B", "Refund for Order 1k"), BigDecimal.ONE, List.of(), null, "deterministic");
            };
        }

        @Bean
        @Primary
        TestMockExecutionProvider testMockExecutionProvider() {
            return new TestMockExecutionProvider();
        }
    }

    public static class TestMockExecutionProvider implements PaymentExecutionProvider {
        private final AtomicInteger invocationCount = new AtomicInteger(0);
        private String forceBehavior = "SUCCESS";

        public void reset() {
            invocationCount.set(0);
            forceBehavior = "SUCCESS";
        }

        public void setForceBehavior(String forceBehavior) {
            this.forceBehavior = forceBehavior;
        }

        public int getInvocationCount() {
            return invocationCount.get();
        }

        @Override
        public ExecutionProvider getProviderType() {
            return ExecutionProvider.RAZORPAY_TEST;
        }

        @Override
        public ExecutionProviderResult execute(ExecutionCommand command) {
            invocationCount.incrementAndGet();
            String action = command.actionType() == null ? "" : command.actionType().toUpperCase();

            if (!"REFUND".equals(action) && !"CUSTOMER_REFUND".equals(action)) {
                return ExecutionProviderResult.unsupported("Standard Razorpay TEST Payment API only supports customer refunds (REFUND). Outbound " + action + " requires RazorpayX Payouts.");
            }

            if ("TIMEOUT".equals(forceBehavior)) {
                return ExecutionProviderResult.unknown("Network timeout communicating with Razorpay gateway.");
            }
            if ("FAIL".equals(forceBehavior)) {
                return ExecutionProviderResult.failure("BAD_REQUEST", "Razorpay rejected payment request.");
            }

            return ExecutionProviderResult.success("rfnd_test_123456789");
        }
    }

    @Test
    @DisplayName("1. SAFE decision (REFUND) can be executed through provider after explicit authorization")
    void safeDecisionCanBeExecuted() throws Exception {
        String analyzeResp = mockMvc.perform(post("/api/agent/analyze")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"Refund ₹1000 to customer\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.governance.status").value("SAFE"))
                .andReturn().getResponse().getContentAsString();

        String decisionId = objectMapper.readTree(analyzeResp).get("governance").get("decisionId").asText();
        String execJson = String.format("{\"decisionId\":\"%s\",\"idempotencyKey\":\"exec_safe_1\"}", decisionId);

        mockMvc.perform(post("/api/executions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(execJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCEEDED"))
                .andExpect(jsonPath("$.provider").value("RAZORPAY_TEST"))
                .andExpect(jsonPath("$.providerReference").value("rfnd_test_123456789"))
                .andExpect(jsonPath("$.amount").value(1000));

        assertEquals(1, mockExecutionProvider.getInvocationCount());

        var events = auditEventRepository.findByDecisionIdOrderByCreatedAtDesc(decisionId);
        assertTrue(events.stream().anyMatch(e -> e.getEventType() == AuditEventType.EXECUTION_SUCCEEDED));
    }

    @Test
    @DisplayName("2. APPROVED decision (REFUND) can be executed through provider")
    void approvedDecisionCanBeExecuted() throws Exception {
        String analyzeResp = mockMvc.perform(post("/api/agent/analyze")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"Refund ₹80,000 to customer\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.governance.status").value("PENDING_REVIEW"))
                .andReturn().getResponse().getContentAsString();

        String decisionId = objectMapper.readTree(analyzeResp).get("governance").get("decisionId").asText();

        // Human Approve
        mockMvc.perform(post("/api/decisions/" + decisionId + "/approve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"actorId\":\"admin\",\"comment\":\"Approved by CFO\"}"))
                .andExpect(status().isOk());

        String execJson = String.format("{\"decisionId\":\"%s\",\"idempotencyKey\":\"exec_appr_1\"}", decisionId);

        mockMvc.perform(post("/api/executions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(execJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCEEDED"))
                .andExpect(jsonPath("$.providerReference").value("rfnd_test_123456789"));

        assertEquals(1, mockExecutionProvider.getInvocationCount());
    }

    @Test
    @DisplayName("3. PENDING_REVIEW decision execution is DENIED and NEVER calls provider")
    void pendingReviewExecutionDenied() throws Exception {
        String analyzeResp = mockMvc.perform(post("/api/agent/analyze")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"Refund ₹80,000 to customer\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.governance.status").value("PENDING_REVIEW"))
                .andReturn().getResponse().getContentAsString();

        String decisionId = objectMapper.readTree(analyzeResp).get("governance").get("decisionId").asText();
        String execJson = String.format("{\"decisionId\":\"%s\",\"idempotencyKey\":\"exec_pend_1\"}", decisionId);

        mockMvc.perform(post("/api/executions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(execJson))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.status").value("ELIGIBILITY_REJECTED"))
                .andExpect(jsonPath("$.failureMessage").value(containsString("PENDING_REVIEW decisions require human governance approval")));

        assertEquals(0, mockExecutionProvider.getInvocationCount());

        var events = auditEventRepository.findByDecisionIdOrderByCreatedAtDesc(decisionId);
        assertTrue(events.stream().anyMatch(e -> e.getEventType() == AuditEventType.EXECUTION_ELIGIBILITY_REJECTED));
    }

    @Test
    @DisplayName("4. REJECTED decision execution is DENIED and NEVER calls provider")
    void rejectedDecisionExecutionDenied() throws Exception {
        String analyzeResp = mockMvc.perform(post("/api/agent/analyze")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"Refund ₹80,000 to customer\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        String decisionId = objectMapper.readTree(analyzeResp).get("governance").get("decisionId").asText();

        // Reject
        mockMvc.perform(post("/api/decisions/" + decisionId + "/reject")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"actorId\":\"admin\",\"comment\":\"Over budget\"}"))
                .andExpect(status().isOk());

        String execJson = String.format("{\"decisionId\":\"%s\",\"idempotencyKey\":\"exec_rej_1\"}", decisionId);

        mockMvc.perform(post("/api/executions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(execJson))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.status").value("ELIGIBILITY_REJECTED"));

        assertEquals(0, mockExecutionProvider.getInvocationCount());
    }

    @Test
    @DisplayName("5. BLOCKED decision execution is DENIED and NEVER calls provider")
    void blockedDecisionExecutionDenied() throws Exception {
        String analyzeResp = mockMvc.perform(post("/api/agent/analyze")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"Refund ₹2,50,000 to Rahul\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.governance.status").value("BLOCKED"))
                .andReturn().getResponse().getContentAsString();

        String decisionId = objectMapper.readTree(analyzeResp).get("governance").get("decisionId").asText();
        String execJson = String.format("{\"decisionId\":\"%s\",\"idempotencyKey\":\"exec_blk_1\"}", decisionId);

        mockMvc.perform(post("/api/executions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(execJson))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.status").value("ELIGIBILITY_REJECTED"))
                .andExpect(jsonPath("$.failureMessage").value(containsString("BLOCKED decisions can never reach payment infrastructure")));

        assertEquals(0, mockExecutionProvider.getInvocationCount());
    }

    @Test
    @DisplayName("6. Duplicate idempotency key calls provider ONLY ONCE and returns existing execution")
    void duplicateIdempotencyKeyCallsProviderOnlyOnce() throws Exception {
        String analyzeResp = mockMvc.perform(post("/api/agent/analyze")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"Refund ₹1000 to customer\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        String decisionId = objectMapper.readTree(analyzeResp).get("governance").get("decisionId").asText();
        String execJson = String.format("{\"decisionId\":\"%s\",\"idempotencyKey\":\"exec_dup_key_123\"}", decisionId);

        mockMvc.perform(post("/api/executions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(execJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCEEDED"));

        assertEquals(1, mockExecutionProvider.getInvocationCount());

        mockMvc.perform(post("/api/executions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(execJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCEEDED"));

        assertEquals(1, mockExecutionProvider.getInvocationCount());
    }

    @Test
    @DisplayName("7. Concurrent duplicate requests invoke provider ONLY ONCE")
    void concurrentDuplicateRequestsCallProviderOnlyOnce() throws Exception {
        String analyzeResp = mockMvc.perform(post("/api/agent/analyze")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"Refund ₹1000 to customer\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        String decisionId = objectMapper.readTree(analyzeResp).get("governance").get("decisionId").asText();
        String execJson = String.format("{\"decisionId\":\"%s\",\"idempotencyKey\":\"exec_concurrent_key\"}", decisionId);

        ExecutorService executor = Executors.newFixedThreadPool(2);
        Callable<String> task = () -> mockMvc.perform(post("/api/executions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(execJson))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        Future<String> f1 = executor.submit(task);
        Future<String> f2 = executor.submit(task);

        assertNotNull(f1.get());
        assertNotNull(f2.get());
        executor.shutdown();

        assertEquals(1, mockExecutionProvider.getInvocationCount());
    }

    @Test
    @DisplayName("8. Outbound VENDOR_PAYMENT returns UNSUPPORTED_EXECUTION")
    void vendorPaymentReturnsUnsupportedExecution() throws Exception {
        String analyzeResp = mockMvc.perform(post("/api/agent/analyze")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"Pay ₹10,000 to Vendor ABC\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.governance.status").value("SAFE"))
                .andReturn().getResponse().getContentAsString();

        String decisionId = objectMapper.readTree(analyzeResp).get("governance").get("decisionId").asText();
        String execJson = String.format("{\"decisionId\":\"%s\",\"idempotencyKey\":\"exec_vend_1\"}", decisionId);

        mockMvc.perform(post("/api/executions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(execJson))
                .andExpect(status().isNotImplemented())
                .andExpect(jsonPath("$.status").value("UNSUPPORTED_EXECUTION"))
                .andExpect(jsonPath("$.failureMessage").value(containsStringIgnoringCase("outbound")));

        assertEquals(1, mockExecutionProvider.getInvocationCount());
    }

    @Test
    @DisplayName("9. Outbound PAYROLL returns UNSUPPORTED_EXECUTION")
    void payrollReturnsUnsupportedExecution() throws Exception {
        String analyzeResp = mockMvc.perform(post("/api/agent/analyze")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"Process Payroll of ₹5000 for Employees\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.governance.status").value("SAFE"))
                .andReturn().getResponse().getContentAsString();

        String decisionId = objectMapper.readTree(analyzeResp).get("governance").get("decisionId").asText();
        String execJson = String.format("{\"decisionId\":\"%s\",\"idempotencyKey\":\"exec_payr_1\"}", decisionId);

        mockMvc.perform(post("/api/executions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(execJson))
                .andExpect(status().isNotImplemented())
                .andExpect(jsonPath("$.status").value("UNSUPPORTED_EXECUTION"))
                .andExpect(jsonPath("$.failureMessage").value(containsString("RazorpayX Payouts")));

        assertEquals(1, mockExecutionProvider.getInvocationCount());
    }

    @Test
    @DisplayName("10. Provider failure creates FAILED execution record")
    void providerFailureCreatesFailedRecord() throws Exception {
        mockExecutionProvider.setForceBehavior("FAIL");

        String analyzeResp = mockMvc.perform(post("/api/agent/analyze")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"Refund ₹1000 to customer\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        String decisionId = objectMapper.readTree(analyzeResp).get("governance").get("decisionId").asText();
        String execJson = String.format("{\"decisionId\":\"%s\",\"idempotencyKey\":\"exec_fail_1\"}", decisionId);

        mockMvc.perform(post("/api/executions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(execJson))
                .andExpect(status().isPaymentRequired())
                .andExpect(jsonPath("$.status").value("FAILED"))
                .andExpect(jsonPath("$.failureCode").value("BAD_REQUEST"));
    }

    @Test
    @DisplayName("11. Provider timeout yields UNKNOWN status without unsafe auto retry")
    void providerTimeoutYieldsUnknownStatus() throws Exception {
        mockExecutionProvider.setForceBehavior("TIMEOUT");

        String analyzeResp = mockMvc.perform(post("/api/agent/analyze")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"Refund ₹1000 to customer\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        String decisionId = objectMapper.readTree(analyzeResp).get("governance").get("decisionId").asText();
        String execJson = String.format("{\"decisionId\":\"%s\",\"idempotencyKey\":\"exec_timeout_1\"}", decisionId);

        mockMvc.perform(post("/api/executions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(execJson))
                .andExpect(status().isGatewayTimeout())
                .andExpect(jsonPath("$.status").value("UNKNOWN"))
                .andExpect(jsonPath("$.failureCode").value("TIMEOUT_UNKNOWN"));

        var events = auditEventRepository.findByDecisionIdOrderByCreatedAtDesc(decisionId);
        assertTrue(events.stream().anyMatch(e -> e.getEventType() == AuditEventType.EXECUTION_UNKNOWN));
    }

    @Test
    @DisplayName("12. GET /api/executions and GET /api/executions/{id} return complete detail")
    void getExecutionsAndDetailWork() throws Exception {
        String analyzeResp = mockMvc.perform(post("/api/agent/analyze")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"Refund ₹1000 to customer\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        String decisionId = objectMapper.readTree(analyzeResp).get("governance").get("decisionId").asText();
        String execJson = String.format("{\"decisionId\":\"%s\",\"idempotencyKey\":\"exec_detail_1\"}", decisionId);

        String execResp = mockMvc.perform(post("/api/executions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(execJson))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        String executionId = objectMapper.readTree(execResp).get("executionId").asText();

        mockMvc.perform(get("/api/executions/" + executionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.executionId").value(executionId))
                .andExpect(jsonPath("$.decisionId").value(decisionId))
                .andExpect(jsonPath("$.provider").value("RAZORPAY_TEST"))
                .andExpect(jsonPath("$.status").value("SUCCEEDED"));

        mockMvc.perform(get("/api/decisions/" + decisionId + "/execution"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.executionId").value(executionId));

        mockMvc.perform(get("/api/executions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.executions", hasSize(greaterThanOrEqualTo(1))));
    }

    @Test
    @DisplayName("13. Invalid decision ID returns 404 Not Found")
    void invalidDecisionReturns404() throws Exception {
        String execJson = "{\"decisionId\":\"dec_invalid_999\",\"idempotencyKey\":\"exec_inv_1\"}";

        mockMvc.perform(post("/api/executions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(execJson))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value(containsString("Decision not found")));
    }
}
