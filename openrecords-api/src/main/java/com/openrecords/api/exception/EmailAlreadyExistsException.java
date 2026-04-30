package com.openrecords.api.exception;

/**
 * Thrown when registration attempts to create a user with an email
 * that's already in the database. Returns HTTP 409 Conflict.
 */
public class EmailAlreadyExistsException extends RuntimeException {
    public EmailAlreadyExistsException(String email) {
        super("An account with email '" + email + "' already exists.");
    }
}