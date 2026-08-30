package com.paylens.backend.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.paylens.backend.repository.InMemoryFinancialStateRepository;
import com.paylens.backend.service.FinancialStateService;
import com.paylens.backend.service.PolicyService;
import com.paylens.backend.service.PolicyThresholds;
import com.paylens.backend.service.SimulationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.paylens.backend.security.JwtAuthenticationFilter;
import com.paylens.backend.security.SecurityConfig;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;

@WebMvcTest(controllers = PolicyController.class, excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {SecurityConfig.class, JwtAuthenticationFilter.class}))
@AutoConfigureMockMvc(addFilters = false)
@Import({
        PolicyService.class, PolicyThresholds.class, SimulationService.class,
        FinancialStateService.class, InMemoryFinancialStateRepository.class
})
class PolicyControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @Test
    void returnsSafeReviewAndBlockDecisions() throws Exception {
        assertDecision("20000", "SAFE");
        assertDecision("50000", "REVIEW");
        assertDecision("250000", "BLOCK");
    }

    @Test
    void rejectsInvalidAmountsAndActionTypes() throws Exception {
        mockMvc.perform(post("/api/policy/evaluate").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"actionType\":\"REFUND\",\"amount\":-1}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("amount must be greater than zero"));

        mockMvc.perform(post("/api/policy/evaluate").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"actionType\":\"INVALID\",\"amount\":100}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Invalid JSON request"));
    }

    private void assertDecision(String amount, String decision) throws Exception {
        mockMvc.perform(post("/api/policy/evaluate").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"actionType\":\"REFUND\",\"amount\":" + amount + "}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.decision").value(decision));
    }
}
