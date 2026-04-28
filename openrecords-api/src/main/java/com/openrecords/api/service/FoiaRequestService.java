package com.openrecords.api.service;

import com.openrecords.api.domain.FoiaRequest;
import com.openrecords.api.domain.FoiaRequestStatus;
import com.openrecords.api.domain.FoiaRequestStatusHistory;
import com.openrecords.api.domain.User;
import com.openrecords.api.dto.CreateFoiaRequestDto;
import com.openrecords.api.dto.FoiaRequestDto;
import com.openrecords.api.dto.PageDto;
import com.openrecords.api.exception.InvalidStatusTransitionException;
import com.openrecords.api.mapper.FoiaRequestMapper;
import com.openrecords.api.repository.FoiaRequestRepository;
import com.openrecords.api.repository.FoiaRequestSpecifications;
import com.openrecords.api.repository.FoiaRequestStatusHistoryRepository;
import com.openrecords.api.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.NoSuchElementException;
import java.util.UUID;

/**
 * Orchestrates FOIA request operations.
 *
 * Responsibilities:
 *   - Create new requests, with side effects (tracking number, audit history)
 *   - Retrieve requests (by id, by user, all)
 *
 * Transaction semantics:
 *   - @Transactional on write methods means all DB changes in the method commit together
 *     or roll back together. If audit-history insert fails after the main insert, both revert.
 *   - @Transactional(readOnly = true) on read methods is a performance hint to Hibernate.
 */
@Service
@Transactional(readOnly = true)  // class-level default: read-only transactions
public class FoiaRequestService {

    private static final Logger log = LoggerFactory.getLogger(FoiaRequestService.class);

    private final FoiaRequestRepository requestRepository;
    private final FoiaRequestStatusHistoryRepository historyRepository;
    private final UserRepository userRepository;
    private final FoiaRequestMapper mapper;
    private final TrackingNumberService trackingNumberService;

    public FoiaRequestService(
        FoiaRequestRepository requestRepository,
        FoiaRequestStatusHistoryRepository historyRepository,
        UserRepository userRepository,
        FoiaRequestMapper mapper,
        TrackingNumberService trackingNumberService
    ) {
        this.requestRepository = requestRepository;
        this.historyRepository = historyRepository;
        this.userRepository = userRepository;
        this.mapper = mapper;
        this.trackingNumberService = trackingNumberService;
    }

    /**
     * Create a new FOIA request.
     *
     * For Phase 4 (before auth is built), we hard-code the requester to the seeded
     * test user. When JWT auth lands in Phase 6, this will accept the authenticated
     * user from the security context instead.
     *
     * @param dto incoming payload from the controller
     * @return DTO representation of the persisted request
     */
    @Transactional  // method-level override: this one WRITES
    public FoiaRequestDto createRequest(CreateFoiaRequestDto dto) {
        // TODO(Phase 6): replace with authenticated user from SecurityContext
        User requester = userRepository.findByEmail("testuser@example.com")
            .orElseThrow(() -> new IllegalStateException(
                "Seeded test user not found. Migration V3 may not have run."
            ));

        String trackingNumber = trackingNumberService.generate();

        FoiaRequest entity = mapper.toEntity(dto, requester, trackingNumber);
        FoiaRequest saved = requestRepository.save(entity);

        // Audit trail: first history row marks creation into DRAFT state
        FoiaRequestStatusHistory historyRow = new FoiaRequestStatusHistory(
            saved,
            null,                          // no "from" state — this is initial creation
            FoiaRequestStatus.DRAFT,
            requester,
            "Request created"
        );
        historyRepository.save(historyRow);

        log.info("Created FOIA request {} ({}) for user {}",
            saved.getTrackingNumber(), saved.getId(), requester.getEmail());

        return mapper.toDto(saved);
    }

    /**
     * Retrieve a single request by its id (UUID).
     * @throws NoSuchElementException if not found — handled by global exception handler as 404
     */
    public FoiaRequestDto getRequestById(UUID id) {
        FoiaRequest entity = requestRepository.findById(id)
            .orElseThrow(() -> new NoSuchElementException(
                "FOIA request not found: " + id
            ));
        return mapper.toDto(entity);
    }

    /**
     * Retrieve a single request by its human-readable tracking number.
     */
    public FoiaRequestDto getRequestByTrackingNumber(String trackingNumber) {
        FoiaRequest entity = requestRepository.findByTrackingNumber(trackingNumber)
            .orElseThrow(() -> new NoSuchElementException(
                "FOIA request not found: " + trackingNumber
            ));
        return mapper.toDto(entity);
    }

    /**
     * List requests with optional filters. All filter params are optional;
     * pass null to skip a filter dimension.
     */
    public PageDto<FoiaRequestDto> listRequests(
        FoiaRequestStatus status,
        Long assigneeId,
        Boolean unassignedOnly,
        Long requesterId,
        Integer dueWithinDays,
        String search,
        Pageable pageable
    ) {
        Specification<FoiaRequest> spec = Specification
            .where(FoiaRequestSpecifications.hasStatus(status))
            .and(FoiaRequestSpecifications.assignedTo(assigneeId))
            .and(FoiaRequestSpecifications.unassignedOnly(unassignedOnly))
            .and(FoiaRequestSpecifications.filedBy(requesterId))
            .and(FoiaRequestSpecifications.dueWithinDays(dueWithinDays))
            .and(FoiaRequestSpecifications.textSearch(search));

        return PageDto.from(
            requestRepository.findAll(spec, pageable),
            mapper::toDto
        );
    }

    /**
     * Transition a request to a new status.
     *
     * Validates the transition against the state machine, updates the entity,
     * writes an audit row, and returns the updated DTO. All within one transaction.
     *
     * @throws NoSuchElementException if the request doesn't exist
     * @throws InvalidStatusTransitionException if the transition is not allowed
     */
    @Transactional
    public FoiaRequestDto transitionStatus(UUID id, FoiaRequestStatus newStatus, String reason) {
        FoiaRequest request = requestRepository.findById(id)
            .orElseThrow(() -> new NoSuchElementException(
                "FOIA request not found: " + id
            ));

        FoiaRequestStatus currentStatus = request.getStatus();

        if (!currentStatus.canTransitionTo(newStatus)) {
            throw new InvalidStatusTransitionException(currentStatus, newStatus);
        }

        // TODO(Phase 6): replace with authenticated user from SecurityContext
        User actor = userRepository.findByEmail("testuser@example.com")
            .orElseThrow(() -> new IllegalStateException("Seeded test user not found"));

        // Apply the change to the entity
        request.applyStatusChange(newStatus);
        FoiaRequest saved = requestRepository.save(request);

        // Write audit history
        FoiaRequestStatusHistory history = new FoiaRequestStatusHistory(
            saved,
            currentStatus,
            newStatus,
            actor,
            reason
        );
        historyRepository.save(history);

        log.info("Transitioned {} from {} to {} (actor: {}, reason: {})",
            saved.getTrackingNumber(), currentStatus, newStatus,
            actor.getEmail(), reason);

        return mapper.toDto(saved);
    }

    /**
     * Assign or unassign a request.
     *
     * @param requestId the FOIA request UUID
     * @param assigneeUserId the staff user ID to assign to, or null to unassign
     * @return the updated request DTO
     * @throws NoSuchElementException if request or user doesn't exist
     * @throws IllegalArgumentException if the target user isn't STAFF or ADMIN
     */
    @Transactional
    public FoiaRequestDto assignRequest(UUID requestId, Long assigneeUserId) {
        FoiaRequest request = requestRepository.findById(requestId)
            .orElseThrow(() -> new NoSuchElementException(
                "FOIA request not found: " + requestId
            ));

        if (assigneeUserId == null) {
            request.unassign();
            FoiaRequest saved = requestRepository.save(request);
            log.info("Unassigned {}", saved.getTrackingNumber());
            return mapper.toDto(saved);
        }

        User staffUser = userRepository.findById(assigneeUserId)
            .orElseThrow(() -> new NoSuchElementException(
                "User not found: " + assigneeUserId
            ));

        if (staffUser.getRole() != User.Role.STAFF && staffUser.getRole() != User.Role.ADMIN) {
            throw new IllegalArgumentException(
                "Only STAFF or ADMIN users can be assigned to requests. " +
                "User " + staffUser.getEmail() + " has role " + staffUser.getRole()
            );
        }

        request.assignTo(staffUser);
        FoiaRequest saved = requestRepository.save(request);
        log.info("Assigned {} to {}", saved.getTrackingNumber(), staffUser.getEmail());
        return mapper.toDto(saved);
    }
}