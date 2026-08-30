package com.paylens.backend.security;

import com.paylens.backend.model.UserRole;
import com.paylens.backend.model.UserStatus;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

public class UserPrincipal implements UserDetails {

    private final String userId;
    private final String merchantId;
    private final String email;
    private final String passwordHash;
    private final UserRole role;
    private final UserStatus status;

    public UserPrincipal(String userId, String merchantId, String email, String passwordHash, UserRole role, UserStatus status) {
        this.userId = userId;
        this.merchantId = merchantId;
        this.email = email;
        this.passwordHash = passwordHash;
        this.role = role;
        this.status = status;
    }

    public String getUserId() { return userId; }
    public String getMerchantId() { return merchantId; }
    public String getEmail() { return email; }
    public UserRole getRole() { return role; }
    public UserStatus getStatus() { return status; }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override
    public String getPassword() { return passwordHash; }

    @Override
    public String getUsername() { return email; }

    @Override
    public boolean isAccountNonExpired() { return true; }

    @Override
    public boolean isAccountNonLocked() { return status != UserStatus.LOCKED; }

    @Override
    public boolean isCredentialsNonExpired() { return true; }

    @Override
    public boolean isEnabled() { return status == UserStatus.ACTIVE; }
}
