package com.openrecords.api.service;

import com.openrecords.api.domain.User;
import com.openrecords.api.dto.AuthResponse;
import com.openrecords.api.dto.LoginRequest;
import com.openrecords.api.dto.UserSummaryDto;
import com.openrecords.api.exception.EmailNotVerifiedException;
import com.openrecords.api.exception.InvalidCredentialsException;
import com.openrecords.api.repository.UserRepository;
import com.openrecords.api.security.JwtService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(
        UserRepository userRepository,
        PasswordEncoder passwordEncoder,
        JwtService jwtService
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    /**
     * Authenticate a user and issue tokens.
     */
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.email().trim().toLowerCase())
            .orElseThrow(() -> {
                log.info("Login failed: email not found ({})", request.email());
                return new InvalidCredentialsException("email not found");
            });

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            log.info("Login failed: bad password for {}", request.email());
            throw new InvalidCredentialsException("password mismatch");
        }

        // Phase 6-4 will require email verification.
        // For now, we'll add the check but skip it for users who don't have the field yet.
        if (!user.isEmailVerified()) {
            log.info("Login failed: email not verified for {}", request.email());
            throw new EmailNotVerifiedException(
                "Please verify your email before logging in. Check your inbox."
            );
        }

        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);

        log.info("Login success: {} ({})", user.getEmail(), user.getRole());

        return new AuthResponse(
            accessToken,
            refreshToken,
            new UserSummaryDto(user.getId(), user.getEmail(), user.getFullName()),
            user.getRole().name()
        );
    }
}