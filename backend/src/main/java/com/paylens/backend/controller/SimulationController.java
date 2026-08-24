package com.paylens.backend.controller;

import com.paylens.backend.dto.SimulationRequest;
import com.paylens.backend.dto.SimulationResult;
import com.paylens.backend.service.SimulationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/simulations")
public class SimulationController {
    private final SimulationService simulationService;

    public SimulationController(SimulationService simulationService) {
        this.simulationService = simulationService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.OK)
    public SimulationResult simulate(@Valid @RequestBody SimulationRequest request) {
        return simulationService.simulate(request);
    }
}
