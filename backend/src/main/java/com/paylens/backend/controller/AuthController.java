package com.paylens.backend.controller;

import com.paylens.backend.dto.*;
import com.paylens.backend.security.UserPrincipal;
import com.paylens.backend.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest req, HttpServletRequest request) {
        try {
            String clientIp = getClientIp(request);
            return ResponseEntity.ok(authService.login(req, clientIp));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(401).body(Map.of("error", e.getMessage()));
        } catch (IllegalStateException e) {
            if (e.getMessage() != null && e.getMessage().contains("Too many")) {
                return ResponseEntity.status(429).body(Map.of("error", e.getMessage()));
            }
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest req, HttpServletRequest request) {
        try {
            String clientIp = getClientIp(request);
            return ResponseEntity.ok(authService.register(req, clientIp));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(429).body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refreshToken(@Valid @RequestBody RefreshTokenRequest req) {
        try {
            return ResponseEntity.ok(authService.refreshToken(req));
        } catch (Exception e) {
            return ResponseEntity.status(401).body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody(required = false) RefreshTokenRequest req
    ) {
        String userId = principal != null ? principal.getUserId() : "unknown";
        String token = req != null ? req.refreshToken() : null;
        authService.logout(userId, token);
        return ResponseEntity.ok(Map.of("message", "Logged out successfully."));
    }

    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(@AuthenticationPrincipal UserPrincipal principal) {
        if (principal == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Not authenticated"));
        }
        return ResponseEntity.ok(authService.getUserProfile(principal.getUserId()));
    }

    @PostMapping("/change-password")
    public ResponseEntity<?> changePassword(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody ChangePasswordRequest req
    ) {
        if (principal == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Not authenticated"));
        }
        try {
            authService.changePassword(principal.getUserId(), req);
            return ResponseEntity.ok(Map.of("message", "Password changed successfully. Active sessions revoked."));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    private String getClientIp(HttpServletRequest request) {
        String xf = request.getHeader("X-Forwarded-For");
        if (xf != null && !xf.isBlank()) {
            return xf.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
