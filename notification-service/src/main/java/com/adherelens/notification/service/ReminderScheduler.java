package com.adherelens.notification.service;

import com.adherelens.notification.domain.Reminder;
import com.adherelens.notification.domain.ReminderStatus;
import com.adherelens.notification.repository.ReminderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

/**
 * Every 15 seconds, sends the reminders whose time has come (first tries and retries alike).
 * Safe on several instances: each reminder is claimed with FOR UPDATE SKIP LOCKED.
 */
@Component
public class ReminderScheduler {

    private static final Logger log = LoggerFactory.getLogger(ReminderScheduler.class);

    private final ReminderRepository reminderRepository;
    private final ReminderDispatcher dispatcher;
    private final ReminderStateService state;
    private final Clock clock;

    public ReminderScheduler(ReminderRepository reminderRepository,
                             ReminderDispatcher dispatcher,
                             ReminderStateService state,
                             Clock clock) {
        this.reminderRepository = reminderRepository;
        this.dispatcher = dispatcher;
        this.state = state;
        this.clock = clock;
    }

    @Scheduled(fixedDelay = 15_000)
    public void sendDueReminders() {
        try {
            int released = state.releaseStaleClaims();
            if (released > 0) {
                log.warn("Released {} stale reminder claim(s) back to PENDING", released);
            }
        } catch (RuntimeException e) {
            log.warn("Releasing stale claims failed: {}", e.toString());
        }

        List<Reminder> due = reminderRepository
                .findTop100ByStatusAndNextAttemptAtLessThanEqualOrderByNextAttemptAtAsc(
                        ReminderStatus.PENDING, Instant.now(clock));
        for (Reminder reminder : due) {
            try {
                dispatcher.dispatch(reminder.getId());
            } catch (RuntimeException e) {
                log.warn("Dispatch failed for reminder {}: {}", reminder.getId(), e.toString());
            }
        }
    }
}
