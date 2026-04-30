package com.openrecords.api.security;

import com.openrecords.api.domain.User;
import com.openrecords.api.repository.UserRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
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
 * Reads the Authorization header, validates the JWT access token,
 * and populates {@link CurrentUser} with the authenticated user.
 *
 * Replaces the old CurrentUserFilter (which trusted X-User-Email).
 *
 * If the header is missing, malformed, or the token invalid/expired,
 * CurrentUser is left empty. Endpoints that require authentication
 * should check {@code currentUser.isAuthenticated()}.
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);
    private static final String AUTH_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final CurrentUser currentUser;

    public JwtAuthenticationFilter(
        JwtService jwtService,
        UserRepository userRepository,
        CurrentUser currentUser
    ) {
        this.jwtService = jwtService;
        this.userRepository = userRepository;
        this.currentUser = currentUser;
    }

    @Override
    protected void doFilterInternal(
        HttpServletRequest request,
        HttpServletResponse response,
        FilterChain chain
    ) throws ServletException, IOException {

        String token = extractToken(request);

        if (token != null) {
            try {
                Claims claims = jwtService.parseClaims(token);
                if (!jwtService.isAccessToken(claims)) {
                    log.debug("JWT auth: token is not an access token");
                } else {
                    Long userId = Long.valueOf(claims.getSubject());
                    Optional<User> user = userRepository.findById(userId);
                    user.ifPresentOrElse(
                        u -> {
                            currentUser.set(u);
                            log.debug("JWT auth: identified as {} ({})", u.getEmail(), u.getRole());
                        },
                        () -> log.warn("JWT auth: token sub claim references unknown user ID {}", userId)
                    );
                }
            } catch (JwtException e) {
                log.debug("JWT auth: token validation failed: {}", e.getMessage());
                // Token invalid/expired — leave CurrentUser empty
            } catch (NumberFormatException e) {
                log.warn("JWT auth: token sub claim is not a valid Long: {}", e.getMessage());
            }
        }

        chain.doFilter(request, response);
    }

    private String extractToken(HttpServletRequest request) {
        String header = request.getHeader(AUTH_HEADER);
        if (header != null && header.startsWith(BEARER_PREFIX)) {
            return header.substring(BEARER_PREFIX.length()).trim();
        }
        return null;
    }
}