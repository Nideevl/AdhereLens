package com.adherelens.notification.service;

import com.adherelens.notification.domain.Reminder;
import com.adherelens.notification.domain.ReminderStatus;
import com.adherelens.notification.repository.ReminderRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

/**
 * The short database steps around a send: claim, record success, record failure. Each is its own
 * small transaction, so no network call (clinical-service, Twilio) ever runs while a row lock or a
 * database connection is held. It is a separate bean from ReminderDispatcher so @Transactional applies.
 */
@Service
public class ReminderStateService {

    private final ReminderRepository reminderRepository;
    private final ReminderRetryPolicy retryPolicy;
    private final Clock clock;
    private final long claimLeaseSeconds;

    public ReminderStateService(ReminderRepository reminderRepository,
                                ReminderRetryPolicy retryPolicy,
                                Clock clock,
                                @Value("${adherelens.notification.retry.claim-lease-seconds:120}") long claimLeaseSeconds) {
        this.reminderRepository = reminderRepository;
        this.retryPolicy = retryPolicy;
        this.clock = clock;
        this.claimLeaseSeconds = claimLeaseSeconds;
    }

    /**
     * Takes the reminder for sending: PENDING to SENDING. Returns null when there is nothing to send:
     * another instance holds it (SKIP LOCKED), it is no longer PENDING, it is not due yet, or its
     * window has closed (then it is cancelled, never sent late).
     */
    @Transactional
    public Reminder claim(UUID reminderId) {
        Reminder reminder = reminderRepository.findByIdSkipLocked(reminderId).orElse(null);
        if (reminder == null || reminder.getStatus() != ReminderStatus.PENDING) {
            return null;
        }
        Instant now = Instant.now(clock);
        if (reminder.getNextAttemptAt().isAfter(now)) {
            return null;
        }
        if (!now.isBefore(reminder.getWindowClosesAt())) {
            reminder.markCancelled();
            return null;
        }
        reminder.markSending(now);
        return reminder;
    }

    @Transactional
    public void recordSent(UUID reminderId) {
        Reminder reminder = reminderRepository.findByIdForUpdate(reminderId).orElse(null);
        if (reminder == null || reminder.getStatus() != ReminderStatus.SENDING) {
            return; // cancelled by an outcome while the message was in flight: leave it cancelled
        }
        reminder.markSent(Instant.now(clock));
    }

    /** Retry with backoff while tries and time remain, otherwise FAILED. error is an exception class name. */
    @Transactional
    public void recordFailure(UUID reminderId, String error) {
        Reminder reminder = reminderRepository.findByIdForUpdate(reminderId).orElse(null);
        if (reminder == null || reminder.getStatus() != ReminderStatus.SENDING) {
            return;
        }
        Instant now = Instant.now(clock);
        int failedTries = reminder.getSendTries() + 1;
        retryPolicy.nextAttemptAt(failedTries, now, reminder.getWindowClosesAt())
                .ifPresentOrElse(next -> reminder.scheduleRetry(next, error),
                        () -> reminder.markFailed(error));
    }

    /** Crash recovery: a claim older than the lease goes back to PENDING (a rare duplicate beats a lost reminder). */
    @Transactional
    public int releaseStaleClaims() {
        Instant cutoff = Instant.now(clock).minusSeconds(claimLeaseSeconds);
        return reminderRepository.releaseStaleClaims(ReminderStatus.SENDING, ReminderStatus.PENDING, cutoff);
    }
}
