package com.paylens.backend;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.paylens.backend.dto.ForecastScenarioRequest;
import com.paylens.backend.model.FinancialHealthStatus;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:intel_testdb;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
class FinancialIntelligenceTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DisplayName("1. GET /api/intelligence/financial-health returns authoritative health score & status")
    void getFinancialHealthReturnsData() throws Exception {
        mockMvc.perform(get("/api/intelligence/financial-health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentBalance").value(840000))
                .andExpect(jsonPath("$.safetyReserve").value(100000))
                .andExpect(jsonPath("$.safetyBuffer").value(120000))
                .andExpect(jsonPath("$.healthStatus").value("HEALTHY"))
                .andExpect(jsonPath("$.healthScore").value(greaterThanOrEqualTo(80)));
    }

    @Test
    @DisplayName("2. GET /api/intelligence/cash-flow returns points calculated from persisted transactions")
    void getCashFlowReturnsPoints() throws Exception {
        mockMvc.perform(get("/api/intelligence/cash-flow"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.points", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$.dataQualityMessage").value(containsString("persisted operational transaction records")));
    }

    @Test
    @DisplayName("3. GET /api/intelligence/obligations returns obligation risk classification")
    void getObligationsReturnsRiskLevels() throws Exception {
        mockMvc.perform(get("/api/intelligence/obligations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.obligations", hasSize(4)))
                .andExpect(jsonPath("$.totalUpcomingAmount").value(620000));
    }

    @Test
    @DisplayName("4. GET /api/intelligence/forecast returns 7-day projected liquidity & buffer")
    void getForecastReturnsLiquidityPoints() throws Exception {
        mockMvc.perform(get("/api/intelligence/forecast?days=7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.forecastDays").value(7))
                .andExpect(jsonPath("$.confidence").value("MEDIUM"))
                .andExpect(jsonPath("$.dataQuality").value("LIMITED_HISTORY"))
                .andExpect(jsonPath("$.forecastDaysList", hasSize(7)));
    }

    @Test
    @DisplayName("5. POST /api/intelligence/forecast/scenario uses SimulationService without state mutation")
    void simulateScenarioWorks() throws Exception {
        ForecastScenarioRequest req = new ForecastScenarioRequest("REFUND", new BigDecimal("50000"), "INR", "Customer X");

        mockMvc.perform(post("/api/intelligence/forecast/scenario")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.actionType").value("REFUND"))
                .andExpect(jsonPath("$.amount").value(50000))
                .andExpect(jsonPath("$.policyDecision").value("REVIEW"))
                .andExpect(jsonPath("$.safetyBufferImpact").value(-50000));

        // Verify financial state is unchanged after scenario simulation
        mockMvc.perform(get("/api/financial-state"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.account.currentBalance").value(840000));
    }

    @Test
    @DisplayName("6. GET /api/intelligence/risk-signals detects active risk signals")
    void getRiskSignalsReturnsSignals() throws Exception {
        mockMvc.perform(get("/api/intelligence/risk-signals"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.signals", hasSize(greaterThanOrEqualTo(1))));
    }

    @Test
    @DisplayName("7. GET /api/intelligence/revenue-at-risk returns revenue risk calculation")
    void getRevenueAtRiskReturnsData() throws Exception {
        mockMvc.perform(get("/api/intelligence/revenue-at-risk"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.calculatedAt").exists());
    }

    @Test
    @DisplayName("8. GET /api/intelligence/summary returns unified command center summary")
    void getSummaryReturnsCompleteData() throws Exception {
        mockMvc.perform(get("/api/intelligence/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.health.healthScore").exists())
                .andExpect(jsonPath("$.forecast.forecastDaysList", hasSize(7)))
                .andExpect(jsonPath("$.topObligations", hasSize(3)))
                .andExpect(jsonPath("$.activeSignals").exists())
                .andExpect(jsonPath("$.revenueAtRisk").exists());
    }
}
