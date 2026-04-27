package com.openrecords.api.dto;

/**
 * Payload for PATCH /api/v1/requests/{id}/assignment.
 *
 * @param assigneeUserId the staff user ID to assign to (null to unassign)
 */
public record AssignmentDto(Long assigneeUserId) {}