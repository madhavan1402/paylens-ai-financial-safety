package com.paylens.backend.dto;
import jakarta.validation.constraints.NotBlank;
public record ReviewRequest(@NotBlank String actorId, @NotBlank String comment) {}
