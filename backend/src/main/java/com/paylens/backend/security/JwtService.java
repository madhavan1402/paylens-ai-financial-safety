package com.paylens.backend.security;

import com.paylens.backend.model.UserRole;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Service
public class JwtService {

    private final SecretKey secretKey;
    private final long accessTokenExpirationMs;

    public JwtService(
            @Value("${paylens.jwt.secret:default-paylens-jwt-secret-key-must-be-at-least-256-bits-for-hmac-sha256-security}") String secret,
            @Value("${paylens.jwt.expiration-ms:900000}") long accessTokenExpirationMs
    ) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessTokenExpirationMs = accessTokenExpirationMs;
    }

    public String generateAccessToken(String userId, String merchantId, UserRole role) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + accessTokenExpirationMs);

        return Jwts.builder()
                .subject(userId)
                .claim("merchantId", merchantId)
                .claim("role", role.name())
                .issuedAt(now)
                .expiration(expiry)
                .signWith(secretKey)
                .compact();
    }

    public Claims parseClaims(String token) throws JwtException {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public boolean validateToken(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public String getUserIdFromToken(String token) {
        return parseClaims(token).getSubject();
    }

    public String getMerchantIdFromToken(String token) {
        return parseClaims(token).get("merchantId", String.class);
    }

    public UserRole getRoleFromToken(String token) {
        String roleStr = parseClaims(token).get("role", String.class);
        return UserRole.valueOf(roleStr);
    }

    public long getAccessTokenExpirationMs() {
        return accessTokenExpirationMs;
    }
}
