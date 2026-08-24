package com.paylens.backend.controller;

import com.paylens.backend.dto.PolicyEvaluationResult;
import com.paylens.backend.dto.SimulationRequest;
import com.paylens.backend.service.PolicyService;
import com.paylens.backend.service.SimulationService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/policy")
public class PolicyController {
    private final SimulationService simulationService;
    private final PolicyService policyService;

    public PolicyController(SimulationService simulationService, PolicyService policyService) {
        this.simulationService = simulationService;
        this.policyService = policyService;
    }

    @PostMapping("/evaluate")
    public PolicyEvaluationResult evaluate(@Valid @RequestBody SimulationRequest request) {
        return policyService.evaluate(simulationService.simulate(request));
    }
}
