package com.paylens.backend.service;

import com.paylens.backend.dto.CreateUserRequest;
import com.paylens.backend.dto.UpdateUserRoleRequest;
import com.paylens.backend.dto.UpdateUserStatusRequest;
import com.paylens.backend.dto.UserResponse;
import com.paylens.backend.model.*;
import com.paylens.backend.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder, AuditService auditService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.auditService = auditService;
    }

    public List<UserResponse> getUsers(String callerMerchantId) {
        return userRepository.findByMerchantId(callerMerchantId).stream()
                .map(UserResponse::from)
                .toList();
    }

    @Transactional
    public UserResponse createUser(String callerUserId, String callerMerchantId, UserRole callerRole, CreateUserRequest req) {
        if (callerRole != UserRole.OWNER && callerRole != UserRole.ADMIN) {
            throw new IllegalStateException("Only OWNER or ADMIN can create users.");
        }

        if (req.role() == UserRole.OWNER && callerRole != UserRole.OWNER) {
            throw new IllegalStateException("Only an existing OWNER can assign the OWNER role.");
        }

        if (userRepository.findByEmail(req.email()).isPresent()) {
            throw new IllegalArgumentException("Email already registered: " + req.email());
        }

        String userId = "user-" + UUID.randomUUID().toString().substring(0, 8);
        String passwordHash = passwordEncoder.encode(req.password());

        User user = new User(userId, callerMerchantId, req.email(), passwordHash, req.displayName(), req.role());
        userRepository.save(user);

        auditService.record(userId, AuditEventType.USER_CREATED, ActorType.HUMAN, callerUserId, "Created user " + req.email() + " with role " + req.role());

        return UserResponse.from(user);
    }

    @Transactional
    public UserResponse updateUserRole(String callerUserId, String callerMerchantId, UserRole callerRole, String targetUserId, UpdateUserRoleRequest req) {
        if (callerRole != UserRole.OWNER && callerRole != UserRole.ADMIN) {
            throw new IllegalStateException("Only OWNER or ADMIN can change user roles.");
        }

        User targetUser = userRepository.findByUserId(targetUserId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + targetUserId));

        if (!targetUser.getMerchantId().equals(callerMerchantId)) {
            throw new IllegalArgumentException("Target user belongs to another merchant.");
        }

        if (req.role() == UserRole.OWNER && callerRole != UserRole.OWNER) {
            throw new IllegalStateException("Only an existing OWNER can assign the OWNER role.");
        }

        if (targetUser.getRole() == UserRole.OWNER && callerRole != UserRole.OWNER) {
            throw new IllegalStateException("Only an existing OWNER can modify another OWNER user.");
        }

        UserRole oldRole = targetUser.getRole();
        targetUser.setRole(req.role());
        userRepository.save(targetUser);

        auditService.record(targetUserId, AuditEventType.ROLE_CHANGED, ActorType.HUMAN, callerUserId, "Changed role of " + targetUser.getEmail() + " from " + oldRole + " to " + req.role());

        return UserResponse.from(targetUser);
    }

    @Transactional
    public UserResponse updateUserStatus(String callerUserId, String callerMerchantId, UserRole callerRole, String targetUserId, UpdateUserStatusRequest req) {
        if (callerRole != UserRole.OWNER && callerRole != UserRole.ADMIN) {
            throw new IllegalStateException("Only OWNER or ADMIN can change user status.");
        }

        if (callerUserId.equals(targetUserId)) {
            throw new IllegalStateException("You cannot disable or lock your own account.");
        }

        User targetUser = userRepository.findByUserId(targetUserId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + targetUserId));

        if (!targetUser.getMerchantId().equals(callerMerchantId)) {
            throw new IllegalArgumentException("Target user belongs to another merchant.");
        }

        targetUser.setStatus(req.status());
        userRepository.save(targetUser);

        AuditEventType evt = req.status() == UserStatus.DISABLED ? AuditEventType.USER_DISABLED : AuditEventType.USER_ENABLED;
        auditService.record(targetUserId, evt, ActorType.HUMAN, callerUserId, "Updated user status of " + targetUser.getEmail() + " to " + req.status());

        return UserResponse.from(targetUser);
    }
}
