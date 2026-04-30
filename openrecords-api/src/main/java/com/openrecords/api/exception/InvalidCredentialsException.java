package com.openrecords.api.exception;

/**
 * Thrown for any login failure: unknown email, wrong password,
 * or unverified email. Always returns a generic message
 * to avoid revealing which check failed (security best practice).
 */
public class InvalidCredentialsException extends RuntimeException {
    public InvalidCredentialsException(String internalReason) {
        super(internalReason);  // Logged server-side, not exposed to user
    }
}