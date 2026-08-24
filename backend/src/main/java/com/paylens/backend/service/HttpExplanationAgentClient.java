package com.paylens.backend.service;

import com.paylens.backend.dto.ExplanationAgentRequest;
import com.paylens.backend.dto.ExplanationResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class HttpExplanationAgentClient implements ExplanationAgentClient {
    private final RestClient restClient;

    public HttpExplanationAgentClient(@Value("${paylens.intent-agent.base-url:http://localhost:8000}") String baseUrl) {
        this.restClient = RestClient.builder().baseUrl(baseUrl).build();
    }

    @Override
    public ExplanationResponse explain(ExplanationAgentRequest request) {
        var response = restClient.post().uri("/api/explain").body(request).retrieve().body(ExplanationResponse.class);
        if (response == null) throw new IllegalStateException("Empty explanation response");
        return response;
    }
}
