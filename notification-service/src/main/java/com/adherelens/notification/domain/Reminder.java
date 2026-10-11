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
    @Column(nullable = false, length = 20)
    private ReminderStatus status = ReminderStatus.PENDING;

    @Column(name = "sent_at")
    private Instant sentAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected Reminder() {
    }

    public Reminder(UUID doseSlotId, UUID hospitalId, UUID patientId, int attemptNo,
                    Instant scheduledFor, Instant windowClosesAt) {
        this.doseSlotId = doseSlotId;
        this.hospitalId = hospitalId;
        this.patientId = patientId;
        this.attemptNo = attemptNo;
        this.scheduledFor = scheduledFor;
        this.windowClosesAt = windowClosesAt;
    }

    public void markSent(Instant at) {
        this.status = ReminderStatus.SENT;
        this.sentAt = at;
    }

    public void markFailed() {
        this.status = ReminderStatus.FAILED;
    }

    public void markCancelled() {
        this.status = ReminderStatus.CANCELLED;
    }

    public UUID getId() { return id; }
    public UUID getDoseSlotId() { return doseSlotId; }
    public UUID getHospitalId() { return hospitalId; }
    public UUID getPatientId() { return patientId; }
    public int getAttemptNo() { return attemptNo; }
    public Instant getScheduledFor() { return scheduledFor; }
    public Instant getWindowClosesAt() { return windowClosesAt; }
    public ReminderStatus getStatus() { return status; }
    public Instant getSentAt() { return sentAt; }
    public Instant getCreatedAt() { return createdAt; }
}
