package com.openrecords.api.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;

/**
 * Health check endpoint — verifies the API is running.
 * Not to be confused with Spring Boot Actuator's /actuator/health, which
 * we'll add later with more detail (database connectivity, disk space, etc.).
 */
@RestController
@RequestMapping("/api/v1/health")
public class HealthController {

    @GetMapping
    public Map<String, Object> health() {
        return Map.of(
            "status", "UP",
            "service", "openrecords-api",
            "timestamp", Instant.now().toString()
        );
    }
}