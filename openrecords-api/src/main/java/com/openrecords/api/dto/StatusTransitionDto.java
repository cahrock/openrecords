package com.openrecords.api.dto;

import com.openrecords.api.domain.FoiaRequestStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Payload for PATCH /api/v1/requests/{id}/status.
 *
 * Reason is optional but encouraged for audit-trail clarity.
 */
public record StatusTransitionDto(

    @NotNull(message = "Target status is required")
    FoiaRequestStatus targetStatus,

    @Size(max = 1000, message = "Reason must be 1000 characters or fewer")
    String reason
) {}