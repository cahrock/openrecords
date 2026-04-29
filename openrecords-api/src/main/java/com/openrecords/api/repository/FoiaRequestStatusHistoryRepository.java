package com.openrecords.api.repository;

import com.openrecords.api.domain.FoiaRequest;
import com.openrecords.api.domain.FoiaRequestStatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Audit-trail access for FOIA request status changes.
 *
 * Append-only in normal operation — the service layer writes new rows but
 * never updates or deletes them.
 */
@Repository
public interface FoiaRequestStatusHistoryRepository extends JpaRepository<FoiaRequestStatusHistory, Long> {

    /**
     * Full status history for a request, oldest first.
     * Used to render the timeline on the request detail page.
     */
    List<FoiaRequestStatusHistory> findByRequestOrderByChangedAtAsc(FoiaRequest request);

    List<FoiaRequestStatusHistory> findByRequestIdOrderByChangedAtAsc(UUID requestId);
}