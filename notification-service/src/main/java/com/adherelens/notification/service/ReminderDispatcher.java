package com.adherelens.notification.service;

import com.adherelens.notification.domain.Reminder;
import com.adherelens.notification.domain.ReminderStatus;
import com.adherelens.notification.repository.ReminderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

/**
 * Sends one reminder. It is a separate bean from ReminderScheduler on purpose: a
 * @Transactional method called from inside the same class would not get a transaction.
 */
@Service
public class ReminderDispatcher {

    private static final Logger log = LoggerFactory.getLogger(ReminderDispatcher.class);

    private final ReminderRepository reminderRepository;
    private final ReminderSender sender;
    private final Clock clock;

    public ReminderDispatcher(ReminderRepository reminderRepository, ReminderSender sender, Clock clock) {
        this.reminderRepository = reminderRepository;
        this.sender = sender;
        this.clock = clock;
    }

    @Transactional
    public void dispatch(UUID reminderId) {
        Reminder reminder = reminderRepository.findById(reminderId).orElse(null);
        if (reminder == null || reminder.getStatus() != ReminderStatus.PENDING) {
            return; // already sent, or cancelled by an outcome in the meantime
        }
        Instant now = Instant.now(clock);
        if (!now.isBefore(reminder.getWindowClosesAt())) {
            reminder.markCancelled(); // window closed while this was waiting, never send late
            return;
        }
        try {
            sender.send(reminder);
            reminder.markSent(now);
        } catch (RuntimeException e) {
            log.warn("Reminder {} failed: {}", reminderId, e.getMessage());
            reminder.markFailed();
        }
    }
}
