package com.openrecords.api.security;

import com.openrecords.api.domain.User;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.stereotype.Component;

/**
 * Request-scoped holder for the authenticated user.
 *
 * Populated by {@link CurrentUserFilter} based on the X-User-Email header.
 * Injected wherever the "current user" matters (services, controllers).
 *
 * NOTE: This is mock authentication. Phase 6 replaces the X-User-Email
 * header lookup with proper JWT validation; this class will then be
 * populated from a verified token claim instead.
 */
@Component
@Scope(value = "request", proxyMode = ScopedProxyMode.TARGET_CLASS)
public class CurrentUser {

    private User user;

    public User get() {
        return user;
    }

    public void set(User user) {
        this.user = user;
    }

    public boolean isAuthenticated() {
        return user != null;
    }

    public boolean hasRole(User.Role role) {
        return user != null && user.getRole() == role;
    }

    public boolean isStaffOrAdmin() {
        return hasRole(User.Role.STAFF) || hasRole(User.Role.ADMIN);
    }
}