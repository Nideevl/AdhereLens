package com.adherelens.notification.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/** Marks a dose slot that already reached a final status. The id is the dose slot id. */
@Entity
@Table(name = "slot_outcomes")
public class SlotOutcome {

    @Id
    @Column(name = "dose_slot_id")
    private UUID doseSlotId;

    @Column(nullable = false, length = 20)
    private String status;

    @Column(name = "received_at", nullable = false)
    private Instant receivedAt;

    protected SlotOutcome() {
    }

    public SlotOutcome(UUID doseSlotId, String status, Instant receivedAt) {
        this.doseSlotId = doseSlotId;
        this.status = status;
        this.receivedAt = receivedAt;
    }

    public UUID getDoseSlotId() { return doseSlotId; }
    public String getStatus() { return status; }
    public Instant getReceivedAt() { return receivedAt; }
}
