package com.openrecords.api.domain;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * State machine values for a FOIA request.
 *
 * Lifecycle (happy path):
 *   DRAFT → SUBMITTED → ACKNOWLEDGED → ASSIGNED → UNDER_REVIEW
 *      → (RESPONSIVE_RECORDS_FOUND → DOCUMENTS_RELEASED) or NO_RECORDS
 *      → CLOSED
 *
 * Side paths: ON_HOLD (pause), REJECTED (terminal rejection).
 *
 * The enum mirrors the CHECK constraint on foia_requests.status in V3 migration.
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

    // ============================================================
    // Terminal states
    // ============================================================

    /**
     * Statuses where a request is "done" — no active SLA, no further transitions.
     */
    public static final Set<FoiaRequestStatus> TERMINAL = Set.of(
        CLOSED, REJECTED, DOCUMENTS_RELEASED, NO_RECORDS
    );

    public boolean isTerminal() {
        return TERMINAL.contains(this);
    }

    // ============================================================
    // Allowed transitions
    // ============================================================
    /**
     * For each status, the set of statuses that this state can transition INTO.
     * If a target isn't in this set for the current status, the transition is rejected.
     *
     * Note: terminal states have an empty set — once closed, a request stays closed.
     */
    private static final Map<FoiaRequestStatus, Set<FoiaRequestStatus>> ALLOWED_TRANSITIONS;

    static {
        Map<FoiaRequestStatus, Set<FoiaRequestStatus>> map = new EnumMap<>(FoiaRequestStatus.class);

        // Initial draft — requester is still composing
        map.put(DRAFT, EnumSet.of(SUBMITTED));

        // After submission, agency must acknowledge or reject
        map.put(SUBMITTED, EnumSet.of(ACKNOWLEDGED, REJECTED));

        // Acknowledged — get assigned (or reject if invalid)
        map.put(ACKNOWLEDGED, EnumSet.of(ASSIGNED, REJECTED, ON_HOLD));

        // Assigned — case officer starts review (or pause for clarification)
        map.put(ASSIGNED, EnumSet.of(UNDER_REVIEW, ON_HOLD, REJECTED));

        // Under review — find records, find none, or pause
        map.put(UNDER_REVIEW, EnumSet.of(RESPONSIVE_RECORDS_FOUND, NO_RECORDS, ON_HOLD, REJECTED));

        // On hold — can resume to any active state, or terminate
        map.put(ON_HOLD, EnumSet.of(ASSIGNED, UNDER_REVIEW, REJECTED, CLOSED));

        // Records found — release to requester
        map.put(RESPONSIVE_RECORDS_FOUND, EnumSet.of(DOCUMENTS_RELEASED, ON_HOLD));

        // Almost-terminal states: just close out
        map.put(DOCUMENTS_RELEASED, EnumSet.of(CLOSED));
        map.put(NO_RECORDS, EnumSet.of(CLOSED));

        // Terminal states — no further transitions
        map.put(REJECTED, EnumSet.noneOf(FoiaRequestStatus.class));
        map.put(CLOSED, EnumSet.noneOf(FoiaRequestStatus.class));

        ALLOWED_TRANSITIONS = Map.copyOf(map);
    }

    /**
     * @return true if this status can transition to the target status.
     */
    public boolean canTransitionTo(FoiaRequestStatus target) {
        return ALLOWED_TRANSITIONS.get(this).contains(target);
    }

    /**
     * @return immutable set of statuses this state can transition to (may be empty).
     */
    public Set<FoiaRequestStatus> allowedNextStatuses() {
        return ALLOWED_TRANSITIONS.get(this);
    }
}