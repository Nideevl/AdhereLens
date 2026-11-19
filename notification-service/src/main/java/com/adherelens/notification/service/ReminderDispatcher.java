package com.adherelens.notification.service;

import com.adherelens.notification.domain.Reminder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Sends one reminder in three steps: claim (short transaction), send (no transaction), record the
 * result (short transaction). If the process dies between send and record, the claim lease expires
 * and the reminder is sent again: delivery is at-least-once, never silently lost.
 */
@Service
public class ReminderDispatcher {

    private static final Logger log = LoggerFactory.getLogger(ReminderDispatcher.class);

    private final ReminderStateService state;
    private final ReminderSender sender;

    public ReminderDispatcher(ReminderStateService state, ReminderSender sender) {
        this.state = state;
        this.sender = sender;
    }

    public void dispatch(UUID reminderId) {
        Reminder claimed = state.claim(reminderId);
        if (claimed == null) {
            return;
        }
        try {
            sender.send(claimed);
        } catch (RuntimeException e) {
            // Class name only: exception messages from the phone lookup or Twilio can contain phone numbers.
            String error = e.getClass().getSimpleName();
            log.warn("Reminder {} send failed: {}", reminderId, error);
            state.recordFailure(reminderId, error);
            return;
        }
        state.recordSent(reminderId);
    }
}
