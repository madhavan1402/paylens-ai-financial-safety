package com.paylens.backend.service;

import com.paylens.backend.dto.*;
import com.paylens.backend.model.*;
import com.paylens.backend.repository.MerchantRepository;
import com.paylens.backend.repository.RefreshTokenRepository;
import com.paylens.backend.repository.UserRepository;
import com.paylens.backend.security.JwtService;
import jakarta.annotation.PostConstruct;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;
import java.util.UUID;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final MerchantRepository merchantRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuditService auditService;
    private final RateLimiterService rateLimiterService;

    public static final String DEFAULT_MERCHANT_ID = "merchant-primary";

    public AuthService(UserRepository userRepository,
                       MerchantRepository merchantRepository,
                       RefreshTokenRepository refreshTokenRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService,
                       AuditService auditService,
                       RateLimiterService rateLimiterService) {
        this.userRepository = userRepository;
        this.merchantRepository = merchantRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.auditService = auditService;
        this.rateLimiterService = rateLimiterService;
    }

    @PostConstruct
    @Transactional
    public void initDefaultMerchantAndUsers() {
        Merchant merchant = merchantRepository.findByMerchantId(DEFAULT_MERCHANT_ID)
                .orElseGet(() -> merchantRepository.save(new Merchant(DEFAULT_MERCHANT_ID, "Acme Commerce Pvt Ltd")));

        String defaultPasswordHash = passwordEncoder.encode("Paylens123!");

        seedUserIfMissing("user-owner-001", merchant.getMerchantId(), "owner@paylens.io", defaultPasswordHash, "Alexis Vance (Owner)", UserRole.OWNER);
        seedUserIfMissing("user-admin-001", merchant.getMerchantId(), "admin@paylens.io", defaultPasswordHash, "Morgan Reid (Admin)", UserRole.ADMIN);
        seedUserIfMissing("user-finance-001", merchant.getMerchantId(), "finance@paylens.io", defaultPasswordHash, "Jordan Taylor (Finance Manager)", UserRole.FINANCE_MANAGER);
        seedUserIfMissing("user-reviewer-001", merchant.getMerchantId(), "reviewer@paylens.io", defaultPasswordHash, "Sam Mercer (Governance Reviewer)", UserRole.REVIEWER);
        seedUserIfMissing("user-operator-001", merchant.getMerchantId(), "operator@paylens.io", defaultPasswordHash, "Taylor Brooke (Payment Operator)", UserRole.OPERATOR);
        seedUserIfMissing("user-viewer-001", merchant.getMerchantId(), "viewer@paylens.io", defaultPasswordHash, "Casey Quinn (Auditor Viewer)", UserRole.VIEWER);
    }

    private void seedUserIfMissing(String userId, String merchantId, String email, String passwordHash, String name, UserRole role) {
        if (userRepository.findByEmail(email).isEmpty()) {
            userRepository.save(new User(userId, merchantId, email, passwordHash, name, role));
        }
    }

    @Transactional(noRollbackFor = {IllegalArgumentException.class, IllegalStateException.class})
    public AuthResponse login(LoginRequest req, String clientIp) {
        if (!rateLimiterService.tryAcquire(clientIp + ":" + req.email())) {
            throw new IllegalStateException("Too many login attempts. Please try again later.");
        }

        User user = userRepository.findByEmail(req.email())
                .orElseThrow(() -> {
                    auditService.record("system", AuditEventType.LOGIN_FAILURE, ActorType.HUMAN, req.email(), "Failed login: user not found.");
                    return new IllegalArgumentException("Invalid email or password.");
                });

        Instant now = Instant.now();

        // Check account lock status
        if (user.getStatus() == UserStatus.LOCKED) {
            if (user.getLockedUntil() != null && now.isAfter(user.getLockedUntil())) {
                user.setStatus(UserStatus.ACTIVE);
                user.setFailedLoginAttempts(0);
                user.setLockedUntil(null);
            } else {
                auditService.record(user.getUserId(), AuditEventType.LOGIN_FAILURE, ActorType.HUMAN, user.getUserId(), "Failed login attempt on locked account.");
                throw new IllegalStateException("Account is temporarily locked due to multiple failed login attempts. Try again in 15 minutes.");
            }
        }

        if (user.getStatus() == UserStatus.DISABLED) {
            auditService.record(user.getUserId(), AuditEventType.LOGIN_FAILURE, ActorType.HUMAN, user.getUserId(), "Failed login attempt on disabled account.");
            throw new IllegalStateException("Account is disabled. Please contact your system administrator.");
        }

        // Verify password
        if (!passwordEncoder.matches(req.password(), user.getPasswordHash())) {
            int attempts = user.getFailedLoginAttempts() + 1;
            user.setFailedLoginAttempts(attempts);
            if (attempts >= 5) {
                user.setStatus(UserStatus.LOCKED);
                user.setLockedUntil(now.plus(15, ChronoUnit.MINUTES));
            }
            userRepository.save(user);

            auditService.record(user.getUserId(), AuditEventType.LOGIN_FAILURE, ActorType.HUMAN, user.getUserId(), "Failed login attempt (" + attempts + "/5).");
            throw new IllegalArgumentException("Invalid email or password.");
        }

        // Reset failed login state
        user.setFailedLoginAttempts(0);
        user.setLockedUntil(null);
        user.setLastLoginAt(now);
        userRepository.save(user);

        // Generate Tokens
        String accessToken = jwtService.generateAccessToken(user.getUserId(), user.getMerchantId(), user.getRole());
        String rawRefreshToken = UUID.randomUUID().toString();
        String tokenHash = hashToken(rawRefreshToken);

        RefreshToken refreshToken = new RefreshToken(
                "token-" + UUID.randomUUID().toString().substring(0, 8),
                user.getUserId(), user.getMerchantId(), tokenHash,
                now.plus(7, ChronoUnit.DAYS)
        );
        refreshTokenRepository.save(refreshToken);

        Merchant merchant = merchantRepository.findByMerchantId(user.getMerchantId()).orElse(null);
        String merchantName = merchant != null ? merchant.getName() : "Acme Commerce Pvt Ltd";

        auditService.record(user.getUserId(), AuditEventType.LOGIN_SUCCESS, ActorType.HUMAN, user.getUserId(), "User logged in successfully.");

        return new AuthResponse(
                accessToken, rawRefreshToken, "Bearer",
                jwtService.getAccessTokenExpirationMs() / 1000,
                UserResponse.from(user), merchantName
        );
    }

    @Transactional
    public AuthResponse register(RegisterRequest req, String clientIp) {
        if (!rateLimiterService.tryAcquire(clientIp + ":register")) {
            throw new IllegalStateException("Rate limit exceeded.");
        }

        if (userRepository.findByEmail(req.email()).isPresent()) {
            throw new IllegalArgumentException("Email already registered: " + req.email());
        }

        String merchantId = DEFAULT_MERCHANT_ID;
        if (req.merchantName() != null && !req.merchantName().isBlank()) {
            merchantId = "mch-" + UUID.randomUUID().toString().substring(0, 8);
            merchantRepository.save(new Merchant(merchantId, req.merchantName()));
        }

        String userId = "user-" + UUID.randomUUID().toString().substring(0, 8);
        String passwordHash = passwordEncoder.encode(req.password());
        UserRole role = merchantId.equals(DEFAULT_MERCHANT_ID) ? UserRole.VIEWER : UserRole.OWNER;

        User user = new User(userId, merchantId, req.email(), passwordHash, req.displayName(), role);
        userRepository.save(user);

        auditService.record(userId, AuditEventType.USER_CREATED, ActorType.HUMAN, userId, "Registered new account: " + req.email());

        return login(new LoginRequest(req.email(), req.password()), clientIp);
    }

    @Transactional
    public AuthResponse refreshToken(RefreshTokenRequest req) {
        String tokenHash = hashToken(req.refreshToken());
        RefreshToken storedToken = refreshTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new IllegalArgumentException("Invalid refresh token."));

        if (storedToken.isRevoked() || storedToken.getExpiresAt().isBefore(Instant.now())) {
            throw new IllegalStateException("Refresh token is expired or revoked.");
        }

        User user = userRepository.findByUserId(storedToken.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("User not found for token."));

        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new IllegalStateException("User account is not active.");
        }

        // Revoke old refresh token (Rotation)
        storedToken.setRevoked(true);
        refreshTokenRepository.save(storedToken);

        // Issue new tokens
        String newAccessToken = jwtService.generateAccessToken(user.getUserId(), user.getMerchantId(), user.getRole());
        String newRawRefreshToken = UUID.randomUUID().toString();
        String newTokenHash = hashToken(newRawRefreshToken);

        RefreshToken newRefreshToken = new RefreshToken(
                "token-" + UUID.randomUUID().toString().substring(0, 8),
                user.getUserId(), user.getMerchantId(), newTokenHash,
                Instant.now().plus(7, ChronoUnit.DAYS)
        );
        refreshTokenRepository.save(newRefreshToken);

        Merchant merchant = merchantRepository.findByMerchantId(user.getMerchantId()).orElse(null);
        String merchantName = merchant != null ? merchant.getName() : "Acme Commerce Pvt Ltd";

        return new AuthResponse(
                newAccessToken, newRawRefreshToken, "Bearer",
                jwtService.getAccessTokenExpirationMs() / 1000,
                UserResponse.from(user), merchantName
        );
    }

    @Transactional
    public void logout(String userId, String rawRefreshToken) {
        if (rawRefreshToken != null && !rawRefreshToken.isBlank()) {
            String tokenHash = hashToken(rawRefreshToken);
            refreshTokenRepository.findByTokenHash(tokenHash).ifPresent(t -> {
                t.setRevoked(true);
                refreshTokenRepository.save(t);
            });
        }
        auditService.record(userId, AuditEventType.LOGOUT, ActorType.HUMAN, userId, "User logged out.");
    }

    @Transactional
    public void changePassword(String userId, ChangePasswordRequest req) {
        User user = userRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        if (!passwordEncoder.matches(req.currentPassword(), user.getPasswordHash())) {
            throw new IllegalArgumentException("Current password does not match.");
        }

        user.setPasswordHash(passwordEncoder.encode(req.newPassword()));
        userRepository.save(user);

        // Revoke all refresh tokens
        var tokens = refreshTokenRepository.findByUserIdAndRevokedFalse(userId);
        tokens.forEach(t -> t.setRevoked(true));
        refreshTokenRepository.saveAll(tokens);

        auditService.record(userId, AuditEventType.SESSION_REVOKED, ActorType.HUMAN, userId, "Password changed. Revoked active sessions.");
    }

    public UserResponse getUserProfile(String userId) {
        User user = userRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
        return UserResponse.from(user);
    }

    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm unavailable", e);
        }
    }
}
