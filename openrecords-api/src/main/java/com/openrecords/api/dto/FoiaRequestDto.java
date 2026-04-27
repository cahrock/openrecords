package com.openrecords.api.dto;

import com.openrecords.api.domain.FoiaRequestStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Outgoing representation of a FOIA request — what the API returns to clients.
 *
 * Notably omits:
 *   - requester entity (replaced with nested RequesterSummaryDto)
 *   - audit columns that aren't useful to clients (version, updatedAt)
 *
 * Includes timestamps in ISO-8601 with timezone (OffsetDateTime), which Jackson
 * serializes as strings like "2026-04-22T16:30:45.123Z".
 */
public record FoiaRequestDto(
    UUID id,
    String trackingNumber,
    UserSummaryDto requester,
    UserSummaryDto assignee,
    String subject,
    String description,
    String recordsRequested,
    LocalDate dateRangeStart,
    LocalDate dateRangeEnd,
    FoiaRequestStatus status,
    boolean feeWaiverRequested,
    BigDecimal maxFeeWilling,
    OffsetDateTime submittedAt,
    OffsetDateTime acknowledgedAt,
    LocalDate dueDate,
    OffsetDateTime closedAt,
    OffsetDateTime createdAt
) {}