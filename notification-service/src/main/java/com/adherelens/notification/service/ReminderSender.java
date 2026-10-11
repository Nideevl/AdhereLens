package com.adherelens.notification.service;

import com.adherelens.notification.domain.Reminder;

/**
 * Sends one reminder to an outpatient. A real implementation (SMS or call) will need the
 * patient's phone, which lives only in clinical-service, so it must fetch it from there at
 * send time. Throwing an exception marks the attempt FAILED.
 */
public interface ReminderSender {

    void send(Reminder reminder);
}
