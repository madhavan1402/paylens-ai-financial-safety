package com.paylens.backend.dto;

import com.paylens.backend.model.User;
import com.paylens.backend.model.UserRole;
import com.paylens.backend.model.UserStatus;

import java.time.Instant;

public record UserResponse(
        String userId,
        String merchantId,
        String email,
        String displayName,
        UserRole role,
        UserStatus status,
        Instant lastLoginAt,
        Instant createdAt
) {
    public static UserResponse from(User user) {
        return new UserResponse(
                user.getUserId(),
                user.getMerchantId(),
                user.getEmail(),
                user.getDisplayName(),
                user.getRole(),
                user.getStatus(),
                user.getLastLoginAt(),
                user.getCreatedAt()
        );
    }
}
