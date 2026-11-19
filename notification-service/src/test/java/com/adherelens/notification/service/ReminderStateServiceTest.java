package com.adherelens.notification.service;

import com.adherelens.notification.domain.Reminder;
import com.adherelens.notification.domain.ReminderChannel;
import com.adherelens.notification.domain.ReminderStatus;
import com.adherelens.notification.repository.ReminderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Claim / record steps around a send. Reference: now 08:30 IST, reminder window closes 09:00 IST. */
@ExtendWith(MockitoExtension.class)
class ReminderStateServiceTest {

    private static final Instant NOW = Instant.parse("2026-11-19T03:00:00Z");
    private static final Instant WINDOW_CLOSES = NOW.plusSeconds(1800);
    private static final long LEASE_SECONDS = 120;

    @Mock ReminderRepository reminderRepository;

    private ReminderStateService service;
    private final UUID id = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new ReminderStateService(reminderRepository, new ReminderRetryPolicy(3, 30),
                Clock.fixed(NOW, ZoneOffset.UTC), LEASE_SECONDS);
    }

    private Reminder reminder(Instant scheduledFor, Instant windowClosesAt) {
        Reminder r = new Reminder(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), 1,
                ReminderChannel.SMS, scheduledFor, windowClosesAt);
        ReflectionTestUtils.setField(r, "id", id);
        return r;
    }

    private Reminder sendingReminder() {
        Reminder r = reminder(NOW.minusSeconds(60), WINDOW_CLOSES);
        r.markSending(NOW.minusSeconds(5));
        return r;
    }

    // ---- claim --------------------------------------------------------------------------------

    @Test
    void claimMovesDueReminderToSending() {
        Reminder r = reminder(NOW.minusSeconds(10), WINDOW_CLOSES);
        when(reminderRepository.findByIdSkipLocked(id)).thenReturn(Optional.of(r));

        Reminder claimed = service.claim(id);

        assertThat(claimed).isSameAs(r);
        assertThat(r.getStatus()).isEqualTo(ReminderStatus.SENDING);
        assertThat(r.getClaimedAt()).isEqualTo(NOW);
    }

    @Test
    void claimReturnsNullWhenAnotherInstanceHoldsTheRow() {
        when(reminderRepository.findByIdSkipLocked(id)).thenReturn(Optional.empty());

        assertThat(service.claim(id)).isNull();
    }

    @Test
    void claimIgnoresReminderThatIsNoLongerPending() {
        for (ReminderStatus status : new ReminderStatus[]{ReminderStatus.SENDING, ReminderStatus.SENT,
                ReminderStatus.CANCELLED, ReminderStatus.FAILED}) {
            Reminder r = reminder(NOW.minusSeconds(10), WINDOW_CLOSES);
            ReflectionTestUtils.setField(r, "status", status);
            when(reminderRepository.findByIdSkipLocked(id)).thenReturn(Optional.of(r));

            assertThat(service.claim(id)).isNull();
            assertThat(r.getStatus()).isEqualTo(status);
        }
    }

    @Test
    void claimIgnoresReminderWhoseNextAttemptIsInTheFuture() {
        Reminder r = reminder(NOW.minusSeconds(100), WINDOW_CLOSES);
        r.scheduleRetry(NOW.plusSeconds(30), "IllegalStateException");
        when(reminderRepository.findByIdSkipLocked(id)).thenReturn(Optional.of(r));

        assertThat(service.claim(id)).isNull();
        assertThat(r.getStatus()).isEqualTo(ReminderStatus.PENDING);
    }

    @Test
    void claimCancelsReminderWhoseWindowHasClosed() {
        Reminder r = reminder(NOW.minusSeconds(3600), NOW); // window closes exactly now
        when(reminderRepository.findByIdSkipLocked(id)).thenReturn(Optional.of(r));

        assertThat(service.claim(id)).isNull();
        assertThat(r.getStatus()).isEqualTo(ReminderStatus.CANCELLED);
    }

    // ---- recordSent ---------------------------------------------------------------------------

    @Test
    void recordSentMarksSendingReminderAsSent() {
        Reminder r = sendingReminder();
        when(reminderRepository.findByIdForUpdate(id)).thenReturn(Optional.of(r));

        service.recordSent(id);

        assertThat(r.getStatus()).isEqualTo(ReminderStatus.SENT);
        assertThat(r.getSentAt()).isEqualTo(NOW);
        assertThat(r.getClaimedAt()).isNull();
    }

    @Test
    void recordSentDoesNotOverwriteACancellationThatArrivedWhileSending() {
        Reminder r = sendingReminder();
        r.markCancelled(); // the dose outcome event arrived while the message was in flight
        when(reminderRepository.findByIdForUpdate(id)).thenReturn(Optional.of(r));

        service.recordSent(id);

        assertThat(r.getStatus()).isEqualTo(ReminderStatus.CANCELLED);
    }

    // ---- recordFailure ------------------------------------------------------------------------

    @Test
    void firstFailureSchedulesARetryWithBackoff() {
        Reminder r = sendingReminder();
        when(reminderRepository.findByIdForUpdate(id)).thenReturn(Optional.of(r));

        service.recordFailure(id, "HttpServerErrorException");

        assertThat(r.getStatus()).isEqualTo(ReminderStatus.PENDING);
        assertThat(r.getSendTries()).isEqualTo(1);
        assertThat(r.getNextAttemptAt()).isEqualTo(NOW.plusSeconds(30));
        assertThat(r.getLastError()).isEqualTo("HttpServerErrorException");
        assertThat(r.getClaimedAt()).isNull();
    }

    @Test
    void secondFailureBacksOffLonger() {
        Reminder r = sendingReminder();
        r.scheduleRetry(NOW.minusSeconds(1), "X"); // one failed try already
        r.markSending(NOW.minusSeconds(2));
        when(reminderRepository.findByIdForUpdate(id)).thenReturn(Optional.of(r));

        service.recordFailure(id, "X");

        assertThat(r.getSendTries()).isEqualTo(2);
        assertThat(r.getNextAttemptAt()).isEqualTo(NOW.plusSeconds(60));
    }

    @Test
    void thirdFailureMarksFailed() {
        Reminder r = sendingReminder();
        r.scheduleRetry(NOW.minusSeconds(1), "X");
        r.markSending(NOW);
        r.scheduleRetry(NOW.minusSeconds(1), "X");
        r.markSending(NOW);
        when(reminderRepository.findByIdForUpdate(id)).thenReturn(Optional.of(r));

        service.recordFailure(id, "X");

        assertThat(r.getStatus()).isEqualTo(ReminderStatus.FAILED);
        assertThat(r.getSendTries()).isEqualTo(3);
    }

    @Test
    void failureNearTheEndOfTheWindowIsFailedNotRetriedLate() {
        Reminder r = reminder(NOW.minusSeconds(60), NOW.plusSeconds(20)); // retry at +30s would be too late
        r.markSending(NOW);
        when(reminderRepository.findByIdForUpdate(id)).thenReturn(Optional.of(r));

        service.recordFailure(id, "X");

        assertThat(r.getStatus()).isEqualTo(ReminderStatus.FAILED);
    }

    @Test
    void failureDoesNotReviveAReminderCancelledMeanwhile() {
        Reminder r = sendingReminder();
        r.markCancelled();
        when(reminderRepository.findByIdForUpdate(id)).thenReturn(Optional.of(r));

        service.recordFailure(id, "X");

        assertThat(r.getStatus()).isEqualTo(ReminderStatus.CANCELLED);
        assertThat(r.getSendTries()).isZero();
    }

    // ---- crash recovery -----------------------------------------------------------------------

    @Test
    void staleClaimsAreReleasedUsingTheLeaseCutoff() {
        when(reminderRepository.releaseStaleClaims(ReminderStatus.SENDING, ReminderStatus.PENDING,
                NOW.minusSeconds(LEASE_SECONDS))).thenReturn(2);

        assertThat(service.releaseStaleClaims()).isEqualTo(2);
        verify(reminderRepository).releaseStaleClaims(ReminderStatus.SENDING, ReminderStatus.PENDING,
                NOW.minusSeconds(LEASE_SECONDS));
    }
}
