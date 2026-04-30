package com.openrecords.api.exception;

/**
 * Thrown when an email verification token is invalid:
 * not found, expired, or already used.
 *
 * Always returns the same generic message to avoid revealing token state.
 */
public class InvalidVerificationTokenException extends RuntimeException {
    public InvalidVerificationTokenException(String internalReason) {
        super(internalReason);  // Logged server-side, not exposed
    }
}