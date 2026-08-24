package com.paylens.backend.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.paylens.backend.repository.InMemoryFinancialStateRepository;
import com.paylens.backend.service.FinancialStateService;
import com.paylens.backend.service.SimulationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(SimulationController.class)
@Import({SimulationService.class, FinancialStateService.class, InMemoryFinancialStateRepository.class})
class SimulationControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @Test
    void returnsTheHypotheticalSimulation() throws Exception {
        mockMvc.perform(post("/api/simulations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"actionType\":\"REFUND\",\"amount\":250000,\"description\":\"Customer refund\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.before.currentBalance").value(840000))
                .andExpect(jsonPath("$.after.currentBalance").value(590000))
                .andExpect(jsonPath("$.impact.reserveBreached").value(true))
                .andExpect(jsonPath("$.consequence").value("OBLIGATION_SHORTFALL"));
    }

    @Test
    void rejectsZeroAndInvalidActionAmounts() throws Exception {
        mockMvc.perform(post("/api/simulations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"actionType\":\"REFUND\",\"amount\":0}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("amount must be greater than zero"));

        mockMvc.perform(post("/api/simulations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"actionType\":\"UNKNOWN\",\"amount\":100}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Invalid JSON request"));
    }
}
