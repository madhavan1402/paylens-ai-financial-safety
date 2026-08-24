package com.paylens.backend.dto;

import jakarta.validation.constraints.NotBlank;

public record AgentAnalyzeRequest(@NotBlank(message = "message is required") String message) {
}
