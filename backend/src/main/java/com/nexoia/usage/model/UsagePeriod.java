package com.nexoia.usage.model;

import java.time.Duration;
import java.time.Instant;

/**
 * The reporting window for a usage query. {@code ALL_TIME} has no lower bound and is represented by
 * {@link Instant#EPOCH}, which predates any recorded request.
 */
public enum UsagePeriod {
    LAST_24_HOURS(Duration.ofDays(1)),
    LAST_7_DAYS(Duration.ofDays(7)),
    LAST_30_DAYS(Duration.ofDays(30)),
    ALL_TIME(null);

    private final Duration window;

    UsagePeriod(Duration window) {
        this.window = window;
    }

    public Instant startingFrom(Instant now) {
        return window == null ? Instant.EPOCH : now.minus(window);
    }
}
