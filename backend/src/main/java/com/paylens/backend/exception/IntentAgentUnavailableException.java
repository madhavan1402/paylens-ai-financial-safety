package com.paylens.backend.exception;

public class IntentAgentUnavailableException extends RuntimeException {
    public IntentAgentUnavailableException(Throwable cause) {
        super("Intent agent is unavailable", cause);
    }
}
