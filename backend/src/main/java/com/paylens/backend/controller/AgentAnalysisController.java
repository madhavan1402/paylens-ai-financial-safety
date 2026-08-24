package com.paylens.backend.controller;

import com.paylens.backend.dto.AgentAnalysisResponse;
import com.paylens.backend.dto.AgentAnalyzeRequest;
import com.paylens.backend.service.AgentAnalysisService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/agent")
public class AgentAnalysisController {
    private final AgentAnalysisService agentAnalysisService;

    public AgentAnalysisController(AgentAnalysisService agentAnalysisService) {
        this.agentAnalysisService = agentAnalysisService;
    }

    @PostMapping("/analyze")
    public AgentAnalysisResponse analyze(@Valid @RequestBody AgentAnalyzeRequest request) {
        return agentAnalysisService.analyze(request);
    }
}
