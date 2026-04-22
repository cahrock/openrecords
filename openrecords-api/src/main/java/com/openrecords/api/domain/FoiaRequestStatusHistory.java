package com.openrecords.api.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;

/**
 * Append-only audit record of a status change on a FOIA request.
 *
 * One row is inserted every time a request's status changes. This table is never
 * updated or deleted in normal operation — federal FOIA compliance requires a
 * complete audit trail of state transitions.
 */
@Entity
@Table(name = "foia_request_status_history")
public class FoiaRequestStatusHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "request_id", nullable = false)
    private FoiaRequest request;

    @Enumerated(EnumType.STRING)
    @Column(name = "from_status", length = 50)
    private FoiaRequestStatus fromStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "to_status", nullable = false, length = 50)
    private FoiaRequestStatus toStatus;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "changed_by_id")
    private User changedBy;

    @Column(columnDefinition = "text")
    private String reason;

    @CreationTimestamp
    @Column(name = "changed_at", nullable = false, updatable = false)
    private OffsetDateTime changedAt;

    // ==============================
    // Constructors
    // ==============================
    protected FoiaRequestStatusHistory() {}

    public FoiaRequestStatusHistory(FoiaRequest request, FoiaRequestStatus fromStatus,
                                     FoiaRequestStatus toStatus, User changedBy, String reason) {
        this.request = request;
        this.fromStatus = fromStatus;
        this.toStatus = toStatus;
        this.changedBy = changedBy;
        this.reason = reason;
    }

    // ==============================
    // Getters only (truly immutable)
    // ==============================
    public Long getId() { return id; }
    public FoiaRequest getRequest() { return request; }
    public FoiaRequestStatus getFromStatus() { return fromStatus; }
    public FoiaRequestStatus getToStatus() { return toStatus; }
    public User getChangedBy() { return changedBy; }
    public String getReason() { return reason; }
    public OffsetDateTime getChangedAt() { return changedAt; }
}