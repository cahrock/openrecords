package com.openrecords.api.security;

import com.openrecords.api.domain.User;
import com.openrecords.api.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;

/**
 * Extracts the X-User-Email header and populates {@link CurrentUser}
 * for the duration of the request.
 *
 * If the header is missing, malformed, or doesn't match a known user,
 * CurrentUser is left empty. Endpoints that require authentication
 * should check {@code currentUser.isAuthenticated()}.
 *
 * NOTE: This is mock authentication and provides no real security.
 * Anyone can claim to be any user by setting the header. Phase 6 replaces
 * this with JWT validation.
 */
//@Component
public class CurrentUserFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(CurrentUserFilter.class);
    private static final String USER_EMAIL_HEADER = "X-User-Email";

    private final UserRepository userRepository;
    private final CurrentUser currentUser;

    public CurrentUserFilter(UserRepository userRepository, CurrentUser currentUser) {
        this.userRepository = userRepository;
        this.currentUser = currentUser;
    }

    @Override
    protected void doFilterInternal(
        HttpServletRequest request,
        HttpServletResponse response,
        FilterChain chain
    ) throws ServletException, IOException {

        String email = request.getHeader(USER_EMAIL_HEADER);

        if (email != null && !email.isBlank()) {
            Optional<User> user = userRepository.findByEmail(email.trim());
            user.ifPresentOrElse(
                u -> {
                    currentUser.set(u);
                    log.debug("Mock auth: identified as {} ({})", u.getEmail(), u.getRole());
                },
                () -> log.debug("Mock auth: header email '{}' not found in users table", email)
            );
        }

        chain.doFilter(request, response);
    }
}