package com.paylens.backend.service;

import com.paylens.backend.dto.AgentAnalyzeRequest;
import com.paylens.backend.dto.IntentAgentResponse;
import com.paylens.backend.exception.IntentAgentUnavailableException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class HttpIntentAgentClient implements IntentAgentClient {
    private final RestClient restClient;

    public HttpIntentAgentClient(
            @Value("${paylens.intent-agent.base-url:http://localhost:8000}") String intentAgentBaseUrl) {
        this.restClient = RestClient.builder().baseUrl(intentAgentBaseUrl).build();
    }

    @Override
    public IntentAgentResponse parse(AgentAnalyzeRequest request) {
        try {
            var response = restClient.post().uri("/api/intent").body(request).retrieve()
                    .body(IntentAgentResponse.class);
            if (response == null) {
                throw new IntentAgentUnavailableException(new IllegalStateException("Empty agent response"));
            }
            return response;
        } catch (IntentAgentUnavailableException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw new IntentAgentUnavailableException(exception);
        }
    }
}
