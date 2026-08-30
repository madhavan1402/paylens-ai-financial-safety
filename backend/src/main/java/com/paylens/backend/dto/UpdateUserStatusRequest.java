package com.paylens.backend.dto;

import com.paylens.backend.model.UserStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateUserStatusRequest(
        @NotNull UserStatus status
) {}
