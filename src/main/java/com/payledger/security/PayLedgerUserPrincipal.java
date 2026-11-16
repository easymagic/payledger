package com.payledger.security;

import com.payledger.domain.AppUser;
import com.payledger.domain.UserRole;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public class PayLedgerUserPrincipal implements UserDetails {

    private final UUID userId;
    private final String username;
    private final String passwordHash;
    private final UserRole role;
    private final UUID merchantId;
    private final boolean enabled;

    public PayLedgerUserPrincipal(AppUser user) {
        this(
                user.getId(),
                user.getUsername(),
                user.getPasswordHash(),
                user.getRole(),
                user.getMerchant() != null ? user.getMerchant().getId() : null,
                user.isEnabled());
    }

    public PayLedgerUserPrincipal(
            UUID userId,
            String username,
            String passwordHash,
            UserRole role,
            UUID merchantId,
            boolean enabled) {
        this.userId = userId;
        this.username = username;
        this.passwordHash = passwordHash;
        this.role = role;
        this.merchantId = merchantId;
        this.enabled = enabled;
    }

    public UUID getUserId() {
        return userId;
    }

    public UserRole getRole() {
        return role;
    }

    public UUID getMerchantId() {
        return merchantId;
    }

    public boolean isAdmin() {
        return role == UserRole.ADMIN;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }
}
