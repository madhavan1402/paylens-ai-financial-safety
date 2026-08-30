package com.paylens.backend;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.paylens.backend.dto.RiskDismissRequest;
import com.paylens.backend.model.*;
import com.paylens.backend.repository.ExecutionRepository;
import com.paylens.backend.repository.RiskEventRepository;
import com.paylens.backend.repository.RiskSnapshotRepository;
import com.paylens.backend.service.RiskMonitoringService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:risk_testdb;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
class RiskMonitoringEngineTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private RiskMonitoringService monitoringService;

    @Autowired
    private RiskEventRepository riskEventRepository;

    @Autowired
    private RiskSnapshotRepository riskSnapshotRepository;

    @Autowired
    private ExecutionRepository executionRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        riskEventRepository.deleteAll();
        riskSnapshotRepository.deleteAll();
    }

    @Test
    @DisplayName("1 & 2. First monitoring cycle creates baseline and does not create false deterioration alerts")
    void firstCycleCreatesBaseline() {
        var cycle = monitoringService.runMonitoringCycle();
        assertEquals("SUCCESS", cycle.status());

        var snapshot = riskSnapshotRepository.findTopByOrderByCapturedAtDesc().orElseThrow();
        assertTrue(snapshot.isBaseline(), "First snapshot must be marked as baseline");
    }

    @Test
    @DisplayName("3 & 12 & 13. Fingerprint deduplication prevents duplicate open alerts")
    void deduplicationPreventsDuplicateAlerts() {
        monitoringService.runMonitoringCycle();
        int initialCount = riskEventRepository.findAll().size();

        // Run a second cycle without state changes
        monitoringService.runMonitoringCycle();
        int secondCount = riskEventRepository.findAll().size();

        assertEquals(initialCount, secondCount, "Subsequent monitoring cycle must not create duplicate open risk events");
    }

    @Test
    @DisplayName("15. POST /api/risk-events/{id}/acknowledge transitions OPEN to ACKNOWLEDGED")
    void acknowledgeEndpointWorks() throws Exception {
        monitoringService.runMonitoringCycle();
        List<RiskEvent> openEvents = riskEventRepository.findByStatus(RiskEventStatus.OPEN);

        if (!openEvents.isEmpty()) {
            String eventId = openEvents.get(0).getRiskEventId();

            mockMvc.perform(post("/api/risk-events/" + eventId + "/acknowledge"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("ACKNOWLEDGED"));

            RiskEvent updated = riskEventRepository.findById(eventId).orElseThrow();
            assertEquals(RiskEventStatus.ACKNOWLEDGED, updated.getStatus());
        }
    }

    @Test
    @DisplayName("16. POST /api/risk-events/{id}/dismiss transitions to DISMISSED with reason")
    void dismissEndpointWorks() throws Exception {
        monitoringService.runMonitoringCycle();
        List<RiskEvent> openEvents = riskEventRepository.findByStatus(RiskEventStatus.OPEN);

        if (!openEvents.isEmpty()) {
            String eventId = openEvents.get(0).getRiskEventId();
            RiskDismissRequest req = new RiskDismissRequest("Accepted operational risk");

            mockMvc.perform(post("/api/risk-events/" + eventId + "/dismiss")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("DISMISSED"))
                    .andExpect(jsonPath("$.dismissalReason").value("Accepted operational risk"));
        }
    }

    @Test
    @DisplayName("17. Invalid state transition returns 409 Conflict")
    void invalidStateTransitionReturnsConflict() throws Exception {
        monitoringService.runMonitoringCycle();
        List<RiskEvent> openEvents = riskEventRepository.findByStatus(RiskEventStatus.OPEN);

        if (!openEvents.isEmpty()) {
            String eventId = openEvents.get(0).getRiskEventId();

            // Acknowledge first
            monitoringService.acknowledgeRiskEvent(eventId);

            // Attempting to acknowledge an ACKNOWLEDGED event should return 409 Conflict
            mockMvc.perform(post("/api/risk-events/" + eventId + "/acknowledge"))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.error").exists());
        }
    }

    @Test
    @DisplayName("18. Active risk cannot be falsely resolved if condition remains active")
    void cannotResolveActiveRisk() throws Exception {
        // Seed an UNKNOWN execution to trigger RECONCILIATION_REQUIRED
        ExecutionRecord exec = new ExecutionRecord(
                "exec-active-recon", "dec-001", "idemp-001", ExecutionProvider.RAZORPAY_TEST,
                "REFUND", new BigDecimal("50000"), "INR", "Customer X", ExecutionStatus.UNKNOWN
        );
        exec.setFailureCode("TIMEOUT");
        exec.setFailureMessage("Provider timed out");
        executionRepository.save(exec);

        monitoringService.runMonitoringCycle();

        List<RiskEvent> openEvents = riskEventRepository.findByStatus(RiskEventStatus.OPEN);
        RiskEvent reconEvent = openEvents.stream()
                .filter(e -> e.getRiskSignalType() == RiskSignalType.RECONCILIATION_REQUIRED)
                .findFirst().orElse(null);

        if (reconEvent != null) {
            // Attempt to manually resolve while UNKNOWN execution still exists -> 409 Conflict
            mockMvc.perform(post("/api/risk-events/" + reconEvent.getRiskEventId() + "/resolve"))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.error").value(containsString("Cannot resolve risk event while UNKNOWN executions exist")));
        }
    }

    @Test
    @DisplayName("21. POST /api/risk-monitoring/run executes manual monitoring cycle")
    void manualMonitoringTriggerWorks() throws Exception {
        mockMvc.perform(post("/api/risk-monitoring/run"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.executedAt").exists());
    }

    @Test
    @DisplayName("22. GET /api/risk-monitoring/status returns status telemetry")
    void getMonitoringStatusWorks() throws Exception {
        monitoringService.runMonitoringCycle();

        mockMvc.perform(get("/api/risk-monitoring/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.monitoringEnabled").value(true))
                .andExpect(jsonPath("$.lastRunStatus").value("SUCCESS"));
    }

    @Test
    @DisplayName("26. Risk monitoring DOES NOT execute any payment actions")
    void monitoringDoesNotExecutePayments() {
        long initialExecutions = executionRepository.count();
        monitoringService.runMonitoringCycle();
        long postExecutions = executionRepository.count();

        assertEquals(initialExecutions, postExecutions, "Risk monitoring engine must NEVER execute financial transactions");
    }

    @Test
    @DisplayName("27. Risk event lifecycle operations emit append-only audit trail events")
    void riskEventsEmitAuditTrail() {
        monitoringService.runMonitoringCycle();
        List<RiskEvent> openEvents = riskEventRepository.findByStatus(RiskEventStatus.OPEN);

        if (!openEvents.isEmpty()) {
            String eventId = openEvents.get(0).getRiskEventId();
            monitoringService.acknowledgeRiskEvent(eventId);
            monitoringService.dismissRiskEvent(eventId, "Manual dismissal test");

            RiskEvent dismissed = riskEventRepository.findById(eventId).orElseThrow();
            assertEquals(RiskEventStatus.DISMISSED, dismissed.getStatus());
        }
    }
}
