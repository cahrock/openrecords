package com.openrecords.api.controller;

import com.openrecords.api.domain.User;
import com.openrecords.api.repository.UserRepository;
import com.openrecords.api.security.JwtService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.NoSuchElementException;

/**
 * TEMPORARY endpoint for testing JWT generation in Phase 6-1.
 * REMOVE in 6-2 once /auth/login replaces it.
 */
@RestController
@RequestMapping("/api/v1/dev/jwt")
public class JwtTestController {

    private final JwtService jwtService;
    private final UserRepository userRepository;

    public JwtTestController(JwtService jwtService, UserRepository userRepository) {
        this.jwtService = jwtService;
        this.userRepository = userRepository;
    }

    @GetMapping("/{userId}")
    public TestTokenResponse generateForUser(@PathVariable Long userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new NoSuchElementException("User not found: " + userId));

        return new TestTokenResponse(
            jwtService.generateAccessToken(user),
            jwtService.generateRefreshToken(user),
            user.getEmail(),
            user.getRole().name()
        );
    }

    public record TestTokenResponse(
        String accessToken,
        String refreshToken,
        String email,
        String role
    ) {}
}