package com.adherelens.notification.service;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class ReminderRetryPolicyTest {

    private static final Instant NOW = Instant.parse("2026-11-19T03:00:00Z");
    private static final Instant WINDOW_CLOSES = NOW.plusSeconds(3600);

    private final ReminderRetryPolicy policy = new ReminderRetryPolicy(3, 30);

    @Test
    void backoffDoublesAfterEachFailure() {
        assertThat(policy.nextAttemptAt(1, NOW, WINDOW_CLOSES)).contains(NOW.plusSeconds(30));
        assertThat(policy.nextAttemptAt(2, NOW, WINDOW_CLOSES)).contains(NOW.plusSeconds(60));
    }

    @Test
    void stopsAfterMaxTries() {
        assertThat(policy.nextAttemptAt(3, NOW, WINDOW_CLOSES)).isEmpty();
        assertThat(policy.nextAttemptAt(4, NOW, WINDOW_CLOSES)).isEmpty();
    }

    @Test
    void neverRetriesAtOrAfterTheWindowClosing() {
        Instant closesIn30s = NOW.plusSeconds(30);
        // next try would be exactly at close time: too late
        assertThat(policy.nextAttemptAt(1, NOW, closesIn30s)).isEqualTo(Optional.empty());
        // one second earlier is still inside the window
        assertThat(policy.nextAttemptAt(1, NOW, closesIn30s.plusSeconds(1))).contains(NOW.plusSeconds(30));
    }
}
