package com.openrecords.api.repository;

import com.openrecords.api.domain.FoiaRequest;
import com.openrecords.api.domain.FoiaRequestStatus;
import com.openrecords.api.domain.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Data access for the foia_requests table.
 *
 * Paginated queries return Page<FoiaRequest>. Controllers accept a Pageable
 * parameter that Spring MVC auto-binds from query parameters like
 *   ?page=0&size=20&sort=createdAt,desc
 */
@Repository
public interface FoiaRequestRepository extends JpaRepository<FoiaRequest, UUID> {

    /**
     * Look up a request by its human-readable tracking number
     * (e.g., "FOIA-2026-000001").
     */
    Optional<FoiaRequest> findByTrackingNumber(String trackingNumber);

    /**
     * All requests filed by a specific user, newest first.
     * Used for the "My Requests" page on the requester dashboard.
     */
    Page<FoiaRequest> findByRequesterOrderByCreatedAtDesc(User requester, Pageable pageable);

    /**
     * All requests in a particular workflow state.
     * Used for the staff triage queue (e.g., "show me all SUBMITTED requests").
     */
    Page<FoiaRequest> findByStatus(FoiaRequestStatus status, Pageable pageable);

    /**
     * Check if a tracking number is already taken.
     * Used by the tracking number generator to retry on collision.
     */
    boolean existsByTrackingNumber(String trackingNumber);
}