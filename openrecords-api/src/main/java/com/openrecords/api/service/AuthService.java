package com.openrecords.api.service;

import com.openrecords.api.domain.User;
import com.openrecords.api.domain.VerificationToken;
import com.openrecords.api.dto.AuthResponse;
import com.openrecords.api.dto.LoginRequest;
import com.openrecords.api.dto.RegisterRequest;
import com.openrecords.api.dto.UserSummaryDto;
import com.openrecords.api.exception.EmailAlreadyExistsException;
import com.openrecords.api.exception.EmailNotVerifiedException;
import com.openrecords.api.exception.InvalidCredentialsException;
import com.openrecords.api.repository.UserRepository;
import com.openrecords.api.repository.VerificationTokenRepository;
import com.openrecords.api.security.JwtService;
import org.springframework.transaction.annotation.Transactional;
import com.openrecords.api.dto.RegistrationResponse;

import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.util.Base64;

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
    private final VerificationTokenRepository verificationTokenRepository;

    public AuthService(
        UserRepository userRepository,
        PasswordEncoder passwordEncoder,
        JwtService jwtService,
        VerificationTokenRepository verificationTokenRepository
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.verificationTokenRepository = verificationTokenRepository;
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

    /**
     * Register a new user. Creates the account in unverified state
     * and issues a verification token (logged to console for now).
     */
    @Transactional
    public RegistrationResponse register(RegisterRequest request) {
        String normalizedEmail = request.email().trim().toLowerCase();

        // Check for duplicate
        if (userRepository.findByEmail(normalizedEmail).isPresent()) {
            throw new EmailAlreadyExistsException(normalizedEmail);
        }

        // Create the user
        User user = new User(
            normalizedEmail,
            passwordEncoder.encode(request.password()),
            request.fullName().trim(),
            User.Role.REQUESTER
        );
        user.setEmailVerified(false);
        user = userRepository.save(user);

        // Generate verification token (32 random bytes, base64-url encoded)
        String tokenValue = generateRandomToken();
        OffsetDateTime expiresAt = OffsetDateTime.now().plusHours(24);
        VerificationToken token = new VerificationToken(user, tokenValue, expiresAt);
        verificationTokenRepository.save(token);

        // Log the verification link (Phase 7 will send a real email)
        String verificationLink = "http://localhost:4200/verify?token=" + tokenValue;
        log.info("=================================================");
        log.info("Verification link for {}: {}", normalizedEmail, verificationLink);
        log.info("=================================================");

        log.info("Registration success: {} ({})", user.getEmail(), user.getRole());

        return new RegistrationResponse(
            user.getId(),
            user.getEmail(),
            user.getFullName(),
            "Account created. Check your email to verify."
        );
    }

    private String generateRandomToken() {
        byte[] randomBytes = new byte[32];
        new SecureRandom().nextBytes(randomBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }
}