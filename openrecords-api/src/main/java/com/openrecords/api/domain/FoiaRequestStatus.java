package com.openrecords.api.domain;

import java.util.Set;

/**
 * State machine values for a FOIA request.
 *
 * The lifecycle is roughly:
 *   DRAFT → SUBMITTED → ACKNOWLEDGED → ASSIGNED → UNDER_REVIEW
 *      → (RESPONSIVE_RECORDS_FOUND → DOCUMENTS_RELEASED) or NO_RECORDS
 *      → CLOSED
 *
 * Side paths: ON_HOLD (pause), REJECTED (terminal rejection).
 *
 * The enum mirrors the CHECK constraint on foia_requests.status in V3 migration.
 * Keep them in sync — if you add a value here, update the migration and vice versa.
 */
public enum FoiaRequestStatus {

    /** Requester is still composing; not yet filed with the agency. */
    DRAFT,

    /** Requester has filed the request; awaiting agency acknowledgement. */
    SUBMITTED,

    /** Agency has formally acknowledged receipt; SLA clock starts. */
    ACKNOWLEDGED,

    /** Request has been assigned to a case officer but work has not begun. */
    ASSIGNED,

    /** Case officer is actively searching for responsive records. */
    UNDER_REVIEW,

    /** Work paused — typically awaiting clarification from requester. */
    ON_HOLD,

    /** Records were located and are being prepared for release or redaction. */
    RESPONSIVE_RECORDS_FOUND,

    /** No responsive records exist; closing out. */
    NO_RECORDS,

    /** Records have been released to requester (with redactions where applicable). */
    DOCUMENTS_RELEASED,

    /** Request denied (e.g., exempt under FOIA §552(b), improper form). */
    REJECTED,

    /** Terminal state — no further action. */
    CLOSED;

    /**
     * Terminal statuses — requests in these states don't have active SLAs.
     */
    public static final Set<FoiaRequestStatus> TERMINAL = Set.of(
        CLOSED, REJECTED, DOCUMENTS_RELEASED, NO_RECORDS
    );

    public boolean isTerminal() {
        return TERMINAL.contains(this);
    }
}