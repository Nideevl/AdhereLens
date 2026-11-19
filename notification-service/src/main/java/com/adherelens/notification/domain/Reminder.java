package com.adherelens.notification.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

/** One reminder attempt for an outpatient dose. Holds ids only. */
@Entity
@Table(name = "reminders")
public class Reminder {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "dose_slot_id", nullable = false, updatable = false)
    private UUID doseSlotId;

    @Column(name = "hospital_id", nullable = false, updatable = false)
    private UUID hospitalId;

    @Column(name = "patient_id", nullable = false, updatable = false)
    private UUID patientId;

    @Column(name = "attempt_no", nullable = false, updatable = false)
    private int attemptNo;

    @Column(name = "scheduled_for", nullable = false, updatable = false)
    private Instant scheduledFor;

    @Column(name = "window_closes_at", nullable = false, updatable = false)
    private Instant windowClosesAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false, length = 10)
    private ReminderChannel channel;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ReminderStatus status = ReminderStatus.PENDING;

    @Column(name = "sent_at")
    private Instant sentAt;

    @Column(name = "send_tries", nullable = false)
    private int sendTries;

    @Column(name = "next_attempt_at", nullable = false)
    private Instant nextAttemptAt;

    @Column(name = "claimed_at")
    private Instant claimedAt;

    // Exception class name only. Never a message: those can contain phone numbers.
    @Column(name = "last_error", length = 200)
    private String lastError;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected Reminder() {
    }

    public Reminder(UUID doseSlotId, UUID hospitalId, UUID patientId, int attemptNo,
                    ReminderChannel channel, Instant scheduledFor, Instant windowClosesAt) {
        this.doseSlotId = doseSlotId;
        this.hospitalId = hospitalId;
        this.patientId = patientId;
        this.attemptNo = attemptNo;
        this.channel = channel;
        this.scheduledFor = scheduledFor;
        this.windowClosesAt = windowClosesAt;
        this.nextAttemptAt = scheduledFor;
    }

    /** An instance has claimed this reminder and is about to send it. */
    public void markSending(Instant at) {
        this.status = ReminderStatus.SENDING;
        this.claimedAt = at;
    }

    public void markSent(Instant at) {
        this.status = ReminderStatus.SENT;
        this.sentAt = at;
        this.claimedAt = null;
        this.lastError = null;
    }

    /** The send failed but there is time left: back to PENDING, to be tried again at nextAttemptAt. */
    public void scheduleRetry(Instant nextAttemptAt, String error) {
        this.sendTries++;
        this.status = ReminderStatus.PENDING;
        this.nextAttemptAt = nextAttemptAt;
        this.claimedAt = null;
        this.lastError = error;
    }

    /** The send failed and no more tries are allowed. */
    public void markFailed(String error) {
        this.sendTries++;
        this.status = ReminderStatus.FAILED;
        this.claimedAt = null;
        this.lastError = error;
    }

    public void markCancelled() {
        this.status = ReminderStatus.CANCELLED;
        this.claimedAt = null;
    }

    public UUID getId() { return id; }
    public UUID getDoseSlotId() { return doseSlotId; }
    public UUID getHospitalId() { return hospitalId; }
    public UUID getPatientId() { return patientId; }
    public int getAttemptNo() { return attemptNo; }
    public ReminderChannel getChannel() { return channel; }
    public Instant getScheduledFor() { return scheduledFor; }
    public Instant getWindowClosesAt() { return windowClosesAt; }
    public ReminderStatus getStatus() { return status; }
    public Instant getSentAt() { return sentAt; }
    public int getSendTries() { return sendTries; }
    public Instant getNextAttemptAt() { return nextAttemptAt; }
    public Instant getClaimedAt() { return claimedAt; }
    public String getLastError() { return lastError; }
    public Instant getCreatedAt() { return createdAt; }
}
