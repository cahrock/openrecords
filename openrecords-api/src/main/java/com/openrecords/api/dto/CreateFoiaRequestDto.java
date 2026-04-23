package com.openrecords.api.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Payload for creating a new FOIA request (the "file this request" API call).
 *
 * Fields NOT on this DTO are either:
 *   - Server-generated: id, trackingNumber, status, submittedAt, dueDate, audit columns
 *   - Owner-derived: requester (taken from the authenticated user, not the payload)
 *
 * Validation annotations run automatically when the controller method
 * parameter is annotated with @Valid. If validation fails, Spring throws
 * MethodArgumentNotValidException — our global exception handler turns that
 * into a clean 400 Bad Request response.
 */
public record CreateFoiaRequestDto(

    @NotBlank(message = "Subject is required")
    @Size(max = 255, message = "Subject must be 255 characters or fewer")
    String subject,

    @NotBlank(message = "Description is required")
    @Size(max = 10_000, message = "Description must be 10,000 characters or fewer")
    String description,

    @NotBlank(message = "Records requested is required")
    @Size(max = 10_000, message = "Records requested must be 10,000 characters or fewer")
    String recordsRequested,

    LocalDate dateRangeStart,
    LocalDate dateRangeEnd,

    Boolean feeWaiverRequested,

    @DecimalMin(value = "0.0", message = "Max fee must be non-negative")
    @Digits(integer = 8, fraction = 2, message = "Max fee must have at most 8 digits before the decimal and 2 after")
    BigDecimal maxFeeWilling
) {}