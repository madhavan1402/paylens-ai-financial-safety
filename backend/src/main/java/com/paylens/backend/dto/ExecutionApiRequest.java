package com.paylens.backend.dto;

import jakarta.validation.constraints.NotBlank;

public record ExecutionApiRequest(
        @NotBlank String decisionId,
        @NotBlank String idempotencyKey
) {}
