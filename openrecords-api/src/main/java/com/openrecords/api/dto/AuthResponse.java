package com.openrecords.api.dto;

/**
 * Response body for /auth/login and /auth/refresh.
 * Contains tokens and user identity for the frontend to store.
 */
public record AuthResponse(
    String accessToken,
    String refreshToken,
    UserSummaryDto user,
    String role
) {}