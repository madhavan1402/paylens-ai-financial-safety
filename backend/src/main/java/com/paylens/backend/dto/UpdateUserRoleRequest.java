package com.paylens.backend.dto;

import com.paylens.backend.model.UserRole;
import jakarta.validation.constraints.NotNull;

public record UpdateUserRoleRequest(
        @NotNull UserRole role
) {}
