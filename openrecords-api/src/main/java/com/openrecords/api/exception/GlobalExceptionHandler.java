package com.openrecords.api.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.net.URI;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

/**
 * Central exception handler for the entire API.
 *
 * Converts Java exceptions into RFC 7807 Problem Detail responses. This gives
 * clients a consistent, structured error shape and — critically — prevents
 * stack traces from leaking to HTTP responses.
 *
 * Every handler:
 *   - Logs the exception at an appropriate level (INFO for expected, ERROR for bugs)
 *   - Returns a ProblemDetail with status, title, and safe detail message
 *   - Includes the request path under "instance"
 *   - Adds a UTC timestamp for log correlation
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // ============================================================
    // 404 — Entity not found
    // ============================================================
    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<ProblemDetail> handleNotFound(
        NoSuchElementException ex, HttpServletRequest request
    ) {
        log.info("Not found: {}", ex.getMessage());
        ProblemDetail problem = problem(
            HttpStatus.NOT_FOUND,
            "Resource not found",
            ex.getMessage(),
            request
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(problem);
    }

    // ============================================================
    // 422 — Invalid status transition
    // ============================================================
    @ExceptionHandler(InvalidStatusTransitionException.class)
    public ResponseEntity<ProblemDetail> handleInvalidTransition(
        InvalidStatusTransitionException ex, HttpServletRequest request
    ) {
        log.info("Invalid status transition: {} -> {}", ex.getFrom(), ex.getTo());

        ProblemDetail problem = problem(
            HttpStatus.UNPROCESSABLE_ENTITY,
            "Invalid status transition",
            ex.getMessage(),
            request
        );
        problem.setProperty("from", ex.getFrom().toString());
        problem.setProperty("to", ex.getTo().toString());
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(problem);
    }

    // ============================================================
    // 400 — Invalid request body (e.g. missing required field)
    // ============================================================
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetail> handleValidation(
        MethodArgumentNotValidException ex, HttpServletRequest request
    ) {
        // Collect all field errors into a list the client can display
        List<Map<String, String>> errors = ex.getBindingResult().getFieldErrors().stream()
            .map(err -> {
                Map<String, String> entry = new LinkedHashMap<>();
                entry.put("field", err.getField());
                entry.put("message", err.getDefaultMessage());
                return entry;
            })
            .toList();

        log.info("Validation failed: {}", errors);

        ProblemDetail problem = problem(
            HttpStatus.BAD_REQUEST,
            "Validation failed",
            "One or more fields failed validation",
            request
        );
        problem.setProperty("errors", errors);

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(problem);
    }

    // ============================================================
    // 400 — Malformed JSON body (unparseable, truncated, etc.)
    // ============================================================
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ProblemDetail> handleMalformedJson(
        HttpMessageNotReadableException ex, HttpServletRequest request
    ) {
        log.info("Malformed request body: {}", ex.getMostSpecificCause().getMessage());
        ProblemDetail problem = problem(
            HttpStatus.BAD_REQUEST,
            "Malformed request body",
            "The request body could not be parsed as valid JSON",
            request
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(problem);
    }

    // ============================================================
    // 400 — Path variable type mismatch (e.g. /requests/not-a-uuid)
    // ============================================================
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ProblemDetail> handleTypeMismatch(
        MethodArgumentTypeMismatchException ex, HttpServletRequest request
    ) {
        String expectedType = ex.getRequiredType() != null
            ? ex.getRequiredType().getSimpleName()
            : "unknown";
        String detail = "Parameter '%s' has an invalid value. Expected type: %s"
            .formatted(ex.getName(), expectedType);

        log.info("Type mismatch for parameter {}: {}", ex.getName(), ex.getValue());
        ProblemDetail problem = problem(
            HttpStatus.BAD_REQUEST,
            "Invalid parameter",
            detail,
            request
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(problem);
    }

    // ============================================================
    // 500 — Fallback for any unhandled exception (bugs)
    // ============================================================
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> handleUnexpected(
        Exception ex, HttpServletRequest request
    ) {
        // Log at ERROR with full stack trace so we can diagnose later
        log.error("Unhandled exception on {}: {}",
            request.getRequestURI(), ex.getMessage(), ex);

        ProblemDetail problem = problem(
            HttpStatus.INTERNAL_SERVER_ERROR,
            "Internal server error",
            // Don't leak the real message to clients
            "An unexpected error occurred. Please try again or contact support.",
            request
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(problem);
    }

    // ============================================================
    // Shared builder
    // ============================================================
    private ProblemDetail problem(HttpStatus status, String title, String detail,
                                   HttpServletRequest request) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(title);
        problem.setType(URI.create("about:blank"));
        problem.setInstance(URI.create(request.getRequestURI()));
        problem.setProperty("timestamp", OffsetDateTime.now().toString());
        return problem;
    }
}