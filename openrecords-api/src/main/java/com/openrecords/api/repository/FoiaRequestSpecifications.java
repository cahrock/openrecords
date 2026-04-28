package com.openrecords.api.repository;

import com.openrecords.api.domain.FoiaRequest;
import com.openrecords.api.domain.FoiaRequestStatus;
import com.openrecords.api.util.BusinessDayCalculator;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;

/**
 * Specification factory methods for filtering FOIA requests.
 *
 * Each public method returns a Specification predicate that can be combined
 * with .and() / .or() to build complex queries.
 *
 * Returning null from a specification is a Spring Data JPA convention:
 * it means "no filter applied" — equivalent to omitting the WHERE clause for that field.
 */
public final class FoiaRequestSpecifications {

    private FoiaRequestSpecifications() {}

    /**
     * Match requests with the given status, or any status if null.
     */
    public static Specification<FoiaRequest> hasStatus(FoiaRequestStatus status) {
        if (status == null) {
            return null;
        }
        return (root, query, cb) -> cb.equal(root.get("status"), status);
    }

    /**
     * Match requests assigned to the given user ID, or any if null.
     */
    public static Specification<FoiaRequest> assignedTo(Long assigneeId) {
        if (assigneeId == null) {
            return null;
        }
        return (root, query, cb) -> cb.equal(root.get("assignee").get("id"), assigneeId);
    }

    /**
     * Match requests with no assignee.
     */
    public static Specification<FoiaRequest> unassignedOnly(Boolean unassignedOnly) {
        if (unassignedOnly == null || !unassignedOnly) {
            return null;
        }
        return (root, query, cb) -> cb.isNull(root.get("assignee"));
    }

    /**
     * Match requests filed by the given user ID, or any if null.
     */
    public static Specification<FoiaRequest> filedBy(Long requesterId) {
        if (requesterId == null) {
            return null;
        }
        return (root, query, cb) -> cb.equal(root.get("requester").get("id"), requesterId);
    }

    /**
     * Match requests due within the next N business days from today.
     * Uses BusinessDayCalculator so weekends and federal holidays are excluded.
     *
     * @param dueWithinDays positive number of business days, or null for no filter
     */
    public static Specification<FoiaRequest> dueWithinDays(Integer dueWithinDays) {
        if (dueWithinDays == null || dueWithinDays < 0) {
            return null;
        }
        LocalDate today = LocalDate.now();
        LocalDate threshold = BusinessDayCalculator.addBusinessDays(today, dueWithinDays);
        return (root, query, cb) -> cb.between(root.get("dueDate"), today, threshold);
    }

    /**
     * Free-text search against subject and tracking number (case-insensitive).
     */
    public static Specification<FoiaRequest> textSearch(String search) {
        if (search == null || search.isBlank()) {
            return null;
        }
        String pattern = "%" + search.toLowerCase().trim() + "%";
        return (root, query, cb) -> cb.or(
            cb.like(cb.lower(root.get("subject")), pattern),
            cb.like(cb.lower(root.get("trackingNumber")), pattern)
        );
    }
}