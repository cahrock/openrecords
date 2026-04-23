package com.openrecords.api.controller;

import com.openrecords.api.dto.CreateFoiaRequestDto;
import com.openrecords.api.dto.FoiaRequestDto;
import com.openrecords.api.dto.PageDto;
import com.openrecords.api.service.FoiaRequestService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.UUID;

/**
 * HTTP endpoints for FOIA requests.
 *
 * Routes:
 *   POST   /api/v1/requests                — Create a new request
 *   GET    /api/v1/requests                — List requests (paginated)
 *   GET    /api/v1/requests/{id}           — Fetch request by UUID
 *   GET    /api/v1/requests/tracking/{num} — Fetch request by tracking number
 *
 * All endpoints return JSON. Errors are handled by the global exception handler
 * (Step 9) — this controller should not contain try/catch blocks for expected failures.
 */
@RestController
@RequestMapping("/api/v1/requests")
public class FoiaRequestController {

    private final FoiaRequestService service;

    public FoiaRequestController(FoiaRequestService service) {
        this.service = service;
    }

    /**
     * Create a new FOIA request.
     *
     * @param dto the request payload (validated)
     * @return 201 Created with Location header pointing to the new resource
     */
    @PostMapping
    public ResponseEntity<FoiaRequestDto> createRequest(
        @Valid @RequestBody CreateFoiaRequestDto dto
    ) {
        FoiaRequestDto created = service.createRequest(dto);

        // Build the Location header per HTTP convention for 201 responses
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
            .path("/{id}")
            .buildAndExpand(created.id())
            .toUri();

        return ResponseEntity.created(location).body(created);
    }

    /**
     * List all requests, paginated.
     * Query params: ?page=0&size=20&sort=createdAt,desc
     */
    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public PageDto<FoiaRequestDto> listRequests(
        @PageableDefault(size = 20, sort = "createdAt") Pageable pageable
    ) {
        return service.listAllRequests(pageable);
    }

    /**
     * Fetch a single request by UUID.
     */
    @GetMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    public FoiaRequestDto getRequestById(@PathVariable UUID id) {
        return service.getRequestById(id);
    }

    /**
     * Fetch a single request by its human-readable tracking number.
     */
    @GetMapping("/tracking/{trackingNumber}")
    @ResponseStatus(HttpStatus.OK)
    public FoiaRequestDto getByTrackingNumber(@PathVariable String trackingNumber) {
        return service.getRequestByTrackingNumber(trackingNumber);
    }
}