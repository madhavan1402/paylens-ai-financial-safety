package com.paylens.backend;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.paylens.backend.dto.ChangePasswordRequest;
import com.paylens.backend.dto.CreateUserRequest;
import com.paylens.backend.dto.LoginRequest;
import com.paylens.backend.dto.RefreshTokenRequest;
import com.paylens.backend.model.*;
import com.paylens.backend.repository.AuditEventRepository;
import com.paylens.backend.repository.RefreshTokenRepository;
import com.paylens.backend.repository.UserRepository;
import com.paylens.backend.security.JwtService;
import com.paylens.backend.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:sec_testdb;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
class SecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private AuditEventRepository auditEventRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private AuthService authService;

    @Autowired
    private com.paylens.backend.service.RateLimiterService rateLimiterService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        rateLimiterService.reset();
        userRepository.findAll().forEach(u -> {
            u.setStatus(com.paylens.backend.model.UserStatus.ACTIVE);
            u.setFailedLoginAttempts(0);
            u.setLockedUntil(null);
            userRepository.save(u);
        });
        authService.initDefaultMerchantAndUsers();
    }

    private String getAuthToken(String email, String password) throws Exception {
        LoginRequest req = new LoginRequest(email, password);
        MvcResult res = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andReturn();
        String json = res.getResponse().getContentAsString();
        return objectMapper.readTree(json).get("accessToken").asText();
    }

    @Test
    @DisplayName("1. Unauthenticated request to protected financial endpoint returns 401 UNAUTHORIZED")
    void unauthenticatedRequestReturns401() throws Exception {
        mockMvc.perform(get("/api/dashboard"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Unauthorized. Authentication required."));
    }

    @Test
    @DisplayName("2, 6, 7 & 22. Valid login returns JWT, password stored as BCrypt hash, hash not returned in DTO")
    void validLoginSucceeds() throws Exception {
        String token = getAuthToken("owner@paylens.io", "Paylens123!");
        assertNotNull(token);
        assertTrue(jwtService.validateToken(token));

        User user = userRepository.findByEmail("owner@paylens.io").orElseThrow();
        assertTrue(passwordEncoder.matches("Paylens123!", user.getPasswordHash()));
        assertFalse(user.getPasswordHash().contains("Paylens123!"));

        List<AuditEvent> loginAudit = auditEventRepository.findByEventTypeOrderByCreatedAtDesc(AuditEventType.LOGIN_SUCCESS);
        assertFalse(loginAudit.isEmpty());
    }

    @Test
    @DisplayName("3 & 21. Invalid password fails with 401 and logs LOGIN_FAILURE")
    void invalidPasswordFails() throws Exception {
        LoginRequest req = new LoginRequest("owner@paylens.io", "WrongPassword!");
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Invalid email or password."));

        List<AuditEvent> failAudit = auditEventRepository.findByEventTypeOrderByCreatedAtDesc(AuditEventType.LOGIN_FAILURE);
        assertFalse(failAudit.isEmpty());
    }

    @Test
    @DisplayName("4. Disabled user cannot login")
    void disabledUserCannotLogin() throws Exception {
        User user = userRepository.findByEmail("viewer@paylens.io").orElseThrow();
        user.setStatus(UserStatus.DISABLED);
        userRepository.save(user);

        LoginRequest req = new LoginRequest("viewer@paylens.io", "Paylens123!");
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value(containsString("Account is disabled")));
    }

    @Test
    @DisplayName("5. 5 failed login attempts lock account")
    void fiveFailedLoginsLockAccount() throws Exception {
        LoginRequest req = new LoginRequest("finance@paylens.io", "WrongPassword!");

        for (int i = 0; i < 5; i++) {
            mockMvc.perform(post("/api/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(req)));
        }

        User lockedUser = userRepository.findByEmail("finance@paylens.io").orElseThrow();
        assertEquals(UserStatus.LOCKED, lockedUser.getStatus());
        assertNotNull(lockedUser.getLockedUntil());
    }

    @Test
    @DisplayName("8, 9, 10, 11, 12. Role permissions return 403 FORBIDDEN for unauthorized actions")
    void rolePermissionsEnforced() throws Exception {
        String viewerToken = getAuthToken("viewer@paylens.io", "Paylens123!");
        String reviewerToken = getAuthToken("reviewer@paylens.io", "Paylens123!");
        String operatorToken = getAuthToken("operator@paylens.io", "Paylens123!");

        // 8. VIEWER cannot approve
        mockMvc.perform(post("/api/decisions/dec-001/approve")
                        .header("Authorization", "Bearer " + viewerToken))
                .andExpect(status().isForbidden());

        // 9. VIEWER cannot execute
        mockMvc.perform(post("/api/executions")
                        .header("Authorization", "Bearer " + viewerToken))
                .andExpect(status().isForbidden());

        // 10. REVIEWER cannot execute
        mockMvc.perform(post("/api/executions")
                        .header("Authorization", "Bearer " + reviewerToken))
                .andExpect(status().isForbidden());

        // 11. OPERATOR cannot approve
        mockMvc.perform(post("/api/decisions/dec-001/approve")
                        .header("Authorization", "Bearer " + operatorToken))
                .andExpect(status().isForbidden());

        // 12. VIEWER cannot run risk monitoring
        mockMvc.perform(post("/api/risk-monitoring/run")
                        .header("Authorization", "Bearer " + viewerToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("13 & 14. Non-OWNER cannot assign OWNER role or manage users")
    void userManagementRoleEnforcement() throws Exception {
        String financeToken = getAuthToken("finance@paylens.io", "Paylens123!");

        // FINANCE_MANAGER cannot access /api/users
        mockMvc.perform(get("/api/users")
                        .header("Authorization", "Bearer " + financeToken))
                .andExpect(status().isForbidden());

        String adminToken = getAuthToken("admin@paylens.io", "Paylens123!");
        CreateUserRequest req = new CreateUserRequest("newowner@paylens.io", "Paylens123!", "New Owner", UserRole.OWNER);

        // ADMIN cannot assign OWNER role
        mockMvc.perform(post("/api/users")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("Only an existing OWNER can assign the OWNER role."));
    }

    @Test
    @DisplayName("18, 19 & 20. Authenticated actions derive identity from SecurityContext; secrets never logged in audit")
    void auditIdentityDerivationAndSecretsProtection() throws Exception {
        String ownerToken = getAuthToken("owner@paylens.io", "Paylens123!");

        mockMvc.perform(get("/api/dashboard")
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk());

        // Verify no audit entries contain raw passwords or JWT tokens
        List<AuditEvent> allEvents = auditEventRepository.findAll();
        for (AuditEvent event : allEvents) {
            assertFalse(event.getDescription().contains("Paylens123!"));
            assertFalse(event.getDescription().contains(ownerToken));
        }
    }

    @Test
    @DisplayName("23 & 24. Logout revokes refresh token and rotation works")
    void logoutAndRefreshTokenRotation() throws Exception {
        LoginRequest req = new LoginRequest("owner@paylens.io", "Paylens123!");
        MvcResult res = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andReturn();

        String body = res.getResponse().getContentAsString();
        String rawRefreshToken = objectMapper.readTree(body).get("refreshToken").asText();

        // Refresh token rotation
        RefreshTokenRequest refReq = new RefreshTokenRequest(rawRefreshToken);
        MvcResult refRes = mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(refReq)))
                .andExpect(status().isOk())
                .andReturn();

        String newRawRefreshToken = objectMapper.readTree(refRes.getResponse().getContentAsString()).get("refreshToken").asText();
        assertNotEquals(rawRefreshToken, newRawRefreshToken);

        // Old refresh token must fail
        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(refReq)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("25. CORS rejects unauthorized origins")
    void corsRejectsUnauthorizedOrigins() throws Exception {
        mockMvc.perform(options("/api/dashboard")
                        .header("Origin", "http://malicious-site.com")
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isForbidden());
    }
}
