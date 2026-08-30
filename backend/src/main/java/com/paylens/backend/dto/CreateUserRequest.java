package com.paylens.backend.dto;

import com.paylens.backend.model.UserRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateUserRequest(
        @NotBlank @Email String email,
        @NotBlank @Size(min = 8, message = "Password must be at least 8 characters long") String password,
        @NotBlank String displayName,
        @NotNull UserRole role
) {}
