package com.openrecords.api.dto;

/**
 * Minimal public-safe view of a user, nested inside request DTOs.
 *
 * Intentionally omits:
 *   - passwordHash (NEVER expose)
 *   - enabled (internal concern)
 *   - role (may be sensitive depending on context)
 *   - audit timestamps (not useful in this context)
 */
public record RequesterSummaryDto(
    Long id,
    String email,
    String fullName
) {}