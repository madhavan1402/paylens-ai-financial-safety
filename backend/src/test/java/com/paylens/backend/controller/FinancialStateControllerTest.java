package com.paylens.backend.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import com.paylens.backend.repository.InMemoryFinancialStateRepository;
import com.paylens.backend.service.FinancialStateService;

@WebMvcTest(FinancialStateController.class)
@Import({FinancialStateService.class, InMemoryFinancialStateRepository.class})
class FinancialStateControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @Test
    void returnsTheDeterministicDashboardMetrics() throws Exception {
        mockMvc.perform(get("/api/dashboard"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currency").value("INR"))
                .andExpect(jsonPath("$.currentBalance").value(840000))
                .andExpect(jsonPath("$.upcomingObligations").value(620000))
                .andExpect(jsonPath("$.safetyBuffer").value(120000));
    }

    @Test
    void returnsFinancialStateAndHealthContracts() throws Exception {
        mockMvc.perform(get("/api/financial-state"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.account.currentBalance").value(840000))
                .andExpect(jsonPath("$.transactions.length()").value(4))
                .andExpect(jsonPath("$.obligations.length()").value(4));
    }
}
