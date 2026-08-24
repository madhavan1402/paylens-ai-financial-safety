package com.paylens.backend.service;

import com.paylens.backend.dto.ExplanationAgentRequest;
import com.paylens.backend.dto.ExplanationResponse;

public interface ExplanationAgentClient {
    ExplanationResponse explain(ExplanationAgentRequest request);
}
