package com.adherelens.notification.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Optional;

/**
 * When to retry a failed send. Exponential backoff (base, 2x base, 4x base ...), a maximum number
 * of tries, and never a retry that would land after the dose window has closed.
 */
@Component
public class ReminderRetryPolicy {

    private final int maxTries;
    private final long baseDelaySeconds;

    public ReminderRetryPolicy(@Value("${adherelens.notification.retry.max-tries:3}") int maxTries,
                               @Value("${adherelens.notification.retry.base-delay-seconds:30}") long baseDelaySeconds) {
        this.maxTries = maxTries;
        this.baseDelaySeconds = baseDelaySeconds;
    }

    /**
     * @param failedTries how many tries have failed so far, including the one that just failed (1 or more)
     * @return the time of the next try, or empty if the reminder should be marked FAILED
     */
    public Optional<Instant> nextAttemptAt(int failedTries, Instant now, Instant windowClosesAt) {
        if (failedTries >= maxTries) {
            return Optional.empty();
        }
        long multiplier = 1L << Math.min(failedTries - 1, 10);
        Instant next = now.plusSeconds(baseDelaySeconds * multiplier);
        return next.isBefore(windowClosesAt) ? Optional.of(next) : Optional.empty();
    }
}
