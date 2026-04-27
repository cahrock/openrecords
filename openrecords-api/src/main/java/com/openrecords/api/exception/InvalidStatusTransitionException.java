package com.openrecords.api.exception;

import com.openrecords.api.domain.FoiaRequestStatus;

/**
 * Thrown when an attempted status transition violates the state machine rules.
 *
 * Caught by GlobalExceptionHandler and translated to HTTP 422 Unprocessable Entity
 * (the right status code for a request that's syntactically valid but semantically
 * invalid for the current resource state).
 */
public class InvalidStatusTransitionException extends RuntimeException {

    private final FoiaRequestStatus from;
    private final FoiaRequestStatus to;

    public InvalidStatusTransitionException(FoiaRequestStatus from, FoiaRequestStatus to) {
        super("Cannot transition from " + from + " to " + to);
        this.from = from;
        this.to = to;
    }

    public FoiaRequestStatus getFrom() { return from; }
    public FoiaRequestStatus getTo() { return to; }
}