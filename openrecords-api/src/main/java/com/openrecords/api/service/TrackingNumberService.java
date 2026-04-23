package com.openrecords.api.service;

import com.openrecords.api.repository.FoiaRequestRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Year;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Generates unique, human-readable tracking numbers for FOIA requests.
 *
 * Format: {PREFIX}-{YEAR}-{NUMBER}, for example:
 *     FOIA-2026-000001
 *     FOIA-2026-000002
 *     ...
 *
 * Strategy:
 *   - Random 6-digit number within the current year
 *   - On the rare collision, retry a few times
 *   - After N failed attempts, throw — at that point the year has enough
 *     requests that we should switch to a real sequence anyway
 *
 * A production system would typically back this with a Postgres SEQUENCE for
 * guaranteed uniqueness without retries. Random-with-retry is fine for a
 * portfolio project and is deliberately simple to understand.
 */
@Service
public class TrackingNumberService {

    private static final int MAX_RETRIES = 10;
    private static final int NUMBER_RANGE = 1_000_000;  // 6-digit numbers

    private final FoiaRequestRepository repository;

    @Value("${openrecords.tracking-number.prefix:FOIA}")
    private String prefix;

    public TrackingNumberService(FoiaRequestRepository repository) {
        this.repository = repository;
    }

    @PostConstruct
    void logConfig() {
        // Small startup log so we know the prefix was loaded from config correctly
        System.out.println("TrackingNumberService initialized with prefix: " + prefix);
    }

    /**
     * Generate a new unique tracking number. Thread-safe.
     *
     * @return formatted string like "FOIA-2026-000001"
     * @throws IllegalStateException if unable to find an unused number after MAX_RETRIES
     */
    public String generate() {
        int year = Year.now().getValue();

        for (int attempt = 0; attempt < MAX_RETRIES; attempt++) {
            int number = ThreadLocalRandom.current().nextInt(1, NUMBER_RANGE);
            String candidate = String.format("%s-%d-%06d", prefix, year, number);

            if (!repository.existsByTrackingNumber(candidate)) {
                return candidate;
            }
        }

        throw new IllegalStateException(
            "Unable to generate unique tracking number after " + MAX_RETRIES + " attempts. " +
            "Consider switching to a database sequence."
        );
    }
}