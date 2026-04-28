package com.openrecords.api.exception;

/**
 * Thrown when an action requires authentication but no current user is set.
 *
 * Translated to HTTP 401 Unauthorized by GlobalExceptionHandler.
 */
public class UnauthenticatedException extends RuntimeException {
    public UnauthenticatedException(String message) {
        super(message);
    }
}