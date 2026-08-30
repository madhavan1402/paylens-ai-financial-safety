package com.paylens.backend.controller;

import com.paylens.backend.dto.CreateUserRequest;
import com.paylens.backend.dto.UpdateUserRoleRequest;
import com.paylens.backend.dto.UpdateUserStatusRequest;
import com.paylens.backend.dto.UserResponse;
import com.paylens.backend.security.UserPrincipal;
import com.paylens.backend.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public ResponseEntity<List<UserResponse>> getUsers(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(userService.getUsers(principal.getMerchantId()));
    }

    @PostMapping
    public ResponseEntity<?> createUser(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody CreateUserRequest req
    ) {
        try {
            return ResponseEntity.ok(userService.createUser(principal.getUserId(), principal.getMerchantId(), principal.getRole(), req));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(403).body(Map.of("error", e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/{userId}/role")
    public ResponseEntity<?> updateUserRole(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable String userId,
            @Valid @RequestBody UpdateUserRoleRequest req
    ) {
        try {
            return ResponseEntity.ok(userService.updateUserRole(principal.getUserId(), principal.getMerchantId(), principal.getRole(), userId, req));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(403).body(Map.of("error", e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/{userId}/status")
    public ResponseEntity<?> updateUserStatus(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable String userId,
            @Valid @RequestBody UpdateUserStatusRequest req
    ) {
        try {
            return ResponseEntity.ok(userService.updateUserStatus(principal.getUserId(), principal.getMerchantId(), principal.getRole(), userId, req));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(403).body(Map.of("error", e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
