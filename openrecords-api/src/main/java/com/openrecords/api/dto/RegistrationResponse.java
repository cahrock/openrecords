package com.openrecords.api.dto;

/**
 * Response body for /auth/register.
 * Note: does NOT include tokens — user must verify email before logging in.
 */
public record RegistrationResponse(
    Long id,
    String email,
    String fullName,
    String message
) {}