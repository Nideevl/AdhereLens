package com.adherelens.notification.domain;

public enum ReminderStatus {
    PENDING,
    SENDING,    // claimed by one instance, the message is being sent right now
    SENT,
    CANCELLED,
    FAILED      // out of tries, or no time left in the dose window
}
