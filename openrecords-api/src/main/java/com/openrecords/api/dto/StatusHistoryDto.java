package com.openrecords.api.dto;

import com.openrecords.api.domain.FoiaRequestStatus;

import java.time.OffsetDateTime;

/**
 * One row of the status-history audit trail.
 * Returned by GET /api/v1/requests/{id}/history.
 */
public record StatusHistoryDto(
    Long id,
    FoiaRequestStatus fromStatus,
    FoiaRequestStatus toStatus,
    UserSummaryDto changedBy,
    String reason,
    OffsetDateTime changedAt
) {}