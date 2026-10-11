package com.adherelens.notification.service;

import com.adherelens.notification.domain.Reminder;
import com.adherelens.notification.domain.ReminderStatus;
import com.adherelens.notification.repository.ReminderRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

/** Every 15 seconds, sends the reminders whose time has come. Assumes one running instance. */
@Component
public class ReminderScheduler {

    private final ReminderRepository reminderRepository;
    private final ReminderDispatcher dispatcher;
    private final Clock clock;

    public ReminderScheduler(ReminderRepository reminderRepository, ReminderDispatcher dispatcher, Clock clock) {
        this.reminderRepository = reminderRepository;
        this.dispatcher = dispatcher;
        this.clock = clock;
    }

    @Scheduled(fixedDelay = 15_000)
    public void sendDueReminders() {
        List<Reminder> due = reminderRepository
                .findTop100ByStatusAndScheduledForLessThanEqualOrderByScheduledForAsc(
                        ReminderStatus.PENDING, Instant.now(clock));
        for (Reminder reminder : due) {
            dispatcher.dispatch(reminder.getId());
        }
    }
}
