package com.paylens.backend.security;

import com.paylens.backend.model.UserStatus;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

    public JwtAuthenticationFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            if (jwtService.validateToken(token)) {
                try {
                    String userId = jwtService.getUserIdFromToken(token);
                    String merchantId = jwtService.getMerchantIdFromToken(token);
                    var role = jwtService.getRoleFromToken(token);

                    UserPrincipal principal = new UserPrincipal(userId, merchantId, userId, "", role, UserStatus.ACTIVE);
                    UsernamePasswordAuthenticationToken auth =
                            new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());

                    SecurityContextHolder.getContext().setAuthentication(auth);
                } catch (Exception ignored) {}
            }
        }

        filterChain.doFilter(request, response);
    }
}
