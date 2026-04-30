package com.openrecords.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * New-user registration payload.
 *
 * Password rules:
 * - Min 8 chars, max 100 chars
 * - At least one uppercase, one lowercase, one digit
 *
 * These are reasonable defaults. Real federal apps may have stricter rules
 * (NIST 800-63B compliance, password reuse prevention, etc).
 */
public record RegisterRequest(
    @NotBlank @Email
    @Size(max = 254)
    String email,

    @NotBlank
    @Size(min = 8, max = 100, message = "Password must be 8-100 characters")
    @Pattern(
        regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).+$",
        message = "Password must contain at least one uppercase letter, one lowercase letter, and one digit"
    )
    String password,

    @NotBlank
    @Size(min = 1, max = 200)
    String fullName
) {}