package com.adherelens.notification.service;

import com.adherelens.notification.domain.Reminder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/** Stand-in for SMS/call: only writes a log line with ids, never personal data. */
@Component
public class LogReminderSender implements ReminderSender {

    private static final Logger log = LoggerFactory.getLogger(LogReminderSender.class);

    @Override
    public void send(Reminder reminder) {
        log.info("[SMS-STUB] reminder attempt {} for dose slot {} (patient {}, hospital {})",
                reminder.getAttemptNo(), reminder.getDoseSlotId(), reminder.getPatientId(), reminder.getHospitalId());
    }
}
