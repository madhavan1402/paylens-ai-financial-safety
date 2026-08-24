package com.paylens.backend.service;

import com.paylens.backend.dto.AgentAnalyzeRequest;
import com.paylens.backend.dto.IntentAgentResponse;

public interface IntentAgentClient {
    IntentAgentResponse parse(AgentAnalyzeRequest request);
}
