package com.openrecords.api.exception;

/**
 * Thrown when a user with valid credentials hasn't verified their email.
 * Returns 403 with an actionable message so the frontend can prompt
 * the user to check their inbox.
 */
public class EmailNotVerifiedException extends RuntimeException {
    public EmailNotVerifiedException(String message) {
        super(message);
    }
}