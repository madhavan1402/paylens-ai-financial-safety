package com.paylens.backend.service;

import com.paylens.backend.dto.AgentAnalysisResponse;
import com.paylens.backend.dto.AgentAnalyzeRequest;
import com.paylens.backend.dto.AgentFinancialIntent;
import com.paylens.backend.dto.IntentAgentResponse;
import com.paylens.backend.dto.SimulationRequest;
import com.paylens.backend.exception.IntentAgentUnavailableException;
import com.paylens.backend.model.SimulationActionType;
import jakarta.validation.Validator;
import java.util.List;
import org.springframework.stereotype.Service;

/** Validates untrusted agent output before using existing simulation and policy services. */
@Service
public class AgentAnalysisService {
    private static final String VALID = "VALID";
    private static final String NEEDS_CLARIFICATION = "NEEDS_CLARIFICATION";
    private static final String INVALID = "INVALID";

    private final IntentAgentClient intentAgentClient;
    private final SimulationService simulationService;
    private final PolicyService policyService;
    private final Validator validator;

    public AgentAnalysisService(IntentAgentClient intentAgentClient, SimulationService simulationService,
            PolicyService policyService, Validator validator) {
        this.intentAgentClient = intentAgentClient;
        this.simulationService = simulationService;
        this.policyService = policyService;
        this.validator = validator;
    }

    public AgentAnalysisResponse analyze(AgentAnalyzeRequest request) {
        var agentResponse = intentAgentClient.parse(request);
        if (NEEDS_CLARIFICATION.equals(agentResponse.status())) {
            return response(request.message(), agentResponse, null, null);
        }
        if (!VALID.equals(agentResponse.status())) {
            return response(request.message(), invalidResponse(agentResponse), null, null);
        }

        SimulationRequest action = validatedAction(agentResponse.intent());
        var simulation = simulationService.simulate(action);
        var policy = policyService.evaluate(simulation);
        return response(request.message(), agentResponse, simulation, policy);
    }

    private SimulationRequest validatedAction(AgentFinancialIntent intent) {
        if (intent == null || !"INR".equals(intent.currency()) || intent.description() == null || intent.description().isBlank()) {
            throw new IllegalArgumentException("Intent agent returned an invalid financial intent");
        }
        try {
            var action = new SimulationRequest(SimulationActionType.valueOf(intent.actionType()), intent.amount(), intent.description());
            if (!validator.validate(action).isEmpty()) {
                throw new IllegalArgumentException("Intent agent returned an invalid financial intent");
            }
            return action;
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Intent agent returned an invalid financial intent");
        }
    }

    private IntentAgentResponse invalidResponse(IntentAgentResponse response) {
        return new IntentAgentResponse(INVALID, null, null, List.of(),
                response.message() == null ? "The requested financial action could not be understood." : response.message(),
                response.providerMode());
    }

    private AgentAnalysisResponse response(String message, IntentAgentResponse intentResponse,
            com.paylens.backend.dto.SimulationResult simulation,
            com.paylens.backend.dto.PolicyEvaluationResult policy) {
        return new AgentAnalysisResponse(message, intentResponse.status(), intentResponse.intent(),
                intentResponse.missingFields() == null ? List.of() : intentResponse.missingFields(),
                intentResponse.message(), simulation, policy);
    }
}
