package com.openrecords.api.mapper;

import com.openrecords.api.domain.FoiaRequest;
import com.openrecords.api.domain.User;
import com.openrecords.api.dto.CreateFoiaRequestDto;
import com.openrecords.api.dto.FoiaRequestDto;
import com.openrecords.api.dto.RequesterSummaryDto;
import org.springframework.stereotype.Component;

/**
 * Converts between FOIA request entities and DTOs.
 *
 * This class is a Spring-managed @Component so services can inject it.
 * It is stateless and thread-safe — no instance fields, no mutable state.
 */
@Component
public class FoiaRequestMapper {

    /**
     * Build a new FoiaRequest entity from a create-DTO and the owning user.
     *
     * Notes:
     *   - id / trackingNumber are NOT set here. Those belong to the service layer
     *     (tracking number generation needs database access for uniqueness).
     *   - status defaults to DRAFT via the entity constructor.
     *   - Optional fields (dateRange, fee prefs) are copied as-is.
     */
    public FoiaRequest toEntity(CreateFoiaRequestDto dto, User requester, String trackingNumber) {
        FoiaRequest entity = new FoiaRequest(
            requester,
            trackingNumber,
            dto.subject(),
            dto.description(),
            dto.recordsRequested()
        );

        entity.setDateRangeStart(dto.dateRangeStart());
        entity.setDateRangeEnd(dto.dateRangeEnd());
        entity.setFeeWaiverRequested(
            dto.feeWaiverRequested() != null && dto.feeWaiverRequested()
        );
        entity.setMaxFeeWilling(dto.maxFeeWilling());

        return entity;
    }

    /**
     * Convert a FoiaRequest entity into its API DTO representation.
     */
    public FoiaRequestDto toDto(FoiaRequest entity) {
        return new FoiaRequestDto(
            entity.getId(),
            entity.getTrackingNumber(),
            toRequesterSummary(entity.getRequester()),
            entity.getSubject(),
            entity.getDescription(),
            entity.getRecordsRequested(),
            entity.getDateRangeStart(),
            entity.getDateRangeEnd(),
            entity.getStatus(),
            entity.isFeeWaiverRequested(),
            entity.getMaxFeeWilling(),
            entity.getSubmittedAt(),
            entity.getAcknowledgedAt(),
            entity.getDueDate(),
            entity.getClosedAt(),
            entity.getCreatedAt()
        );
    }

    /**
     * Build a minimal user view for embedding in request DTOs.
     * Does NOT expose sensitive fields like password hash.
     */
    public RequesterSummaryDto toRequesterSummary(User user) {
        if (user == null) {
            return null;
        }
        return new RequesterSummaryDto(
            user.getId(),
            user.getEmail(),
            user.getFullName()
        );
    }
}