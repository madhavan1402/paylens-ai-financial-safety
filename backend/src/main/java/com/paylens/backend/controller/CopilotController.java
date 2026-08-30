package com.paylens.backend.controller;

import com.paylens.backend.dto.CopilotRequest;
import com.paylens.backend.dto.CopilotResponse;
import com.paylens.backend.service.CopilotService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Phase 13 — AI Fintech Copilot REST endpoint.
 *
 * <p>Protected by JWT auth (authenticated() rule in SecurityConfig).
 * Available to any authenticated user regardless of role.
 *
 * <p>IMPORTANT: The Copilot NEVER executes or authorises financial transactions.
 * The deterministic Policy Engine remains the authoritative decision maker.
 */
@RestController
@RequestMapping("/api/copilot")
public class CopilotController {

    private final CopilotService copilotService;

    public CopilotController(CopilotService copilotService) {
        this.copilotService = copilotService;
    }

    /**
     * Accept a merchant natural-language query and return a structured Copilot response.
     *
     * <p>Security: requires valid JWT. Any authenticated role may query the Copilot.
     * Rate limiting is inherited from the global security filter chain.
     */
    @PostMapping("/query")
    public CopilotResponse query(@Valid @RequestBody CopilotRequest request) {
        return copilotService.query(request);
    }
}
