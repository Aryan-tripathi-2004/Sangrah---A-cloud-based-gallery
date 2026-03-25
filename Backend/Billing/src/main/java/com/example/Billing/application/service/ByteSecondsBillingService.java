package com.example.Billing.application.service;

import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

@Service
public class ByteSecondsBillingService {

    public long bytesSeconds(long sizeBytes, Instant startAt, Instant endAtExclusive) {
        Objects.requireNonNull(startAt, "startAt is required");
        Objects.requireNonNull(endAtExclusive, "endAtExclusive is required");
        if (endAtExclusive.isBefore(startAt)) {
            throw new IllegalArgumentException("endAtExclusive must be after startAt");
        }
        long seconds = Duration.between(startAt, endAtExclusive).toSeconds();
        return Math.max(0, seconds) * sizeBytes;
    }
}
