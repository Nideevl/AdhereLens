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

/** An inpatient dose the nurses should give. Holds ids only. */
@Entity
@Table(name = "nurse_alerts")
public class NurseAlert {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "dose_slot_id", nullable = false, updatable = false)
    private UUID doseSlotId;

    @Column(name = "hospital_id", nullable = false, updatable = false)
    private UUID hospitalId;

    @Column(name = "patient_id", nullable = false, updatable = false)
    private UUID patientId;

    @Column(name = "regimen_id", nullable = false, updatable = false)
    private UUID regimenId;

    @Column(name = "due_at", nullable = false, updatable = false)
    private Instant dueAt;

    @Column(name = "window_closes_at", nullable = false, updatable = false)
    private Instant windowClosesAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AlertStatus status = AlertStatus.OPEN;

    @Column(name = "resolved_status", length = 20)
    private String resolvedStatus;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    protected NurseAlert() {
    }

    public NurseAlert(UUID doseSlotId, UUID hospitalId, UUID patientId, UUID regimenId,
                      Instant dueAt, Instant windowClosesAt) {
        this.doseSlotId = doseSlotId;
        this.hospitalId = hospitalId;
        this.patientId = patientId;
        this.regimenId = regimenId;
        this.dueAt = dueAt;
        this.windowClosesAt = windowClosesAt;
    }

    public void resolve(String finalStatus, Instant at) {
        this.status = AlertStatus.RESOLVED;
        this.resolvedStatus = finalStatus;
        this.resolvedAt = at;
    }

    public UUID getId() { return id; }
    public UUID getDoseSlotId() { return doseSlotId; }
    public UUID getHospitalId() { return hospitalId; }
    public UUID getPatientId() { return patientId; }
    public UUID getRegimenId() { return regimenId; }
    public Instant getDueAt() { return dueAt; }
    public Instant getWindowClosesAt() { return windowClosesAt; }
    public AlertStatus getStatus() { return status; }
    public String getResolvedStatus() { return resolvedStatus; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getResolvedAt() { return resolvedAt; }
}
