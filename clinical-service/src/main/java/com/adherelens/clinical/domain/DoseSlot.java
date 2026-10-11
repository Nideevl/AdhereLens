package com.adherelens.clinical.domain;

import com.adherelens.common.model.CareSetting;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "dose_slots")
public class DoseSlot {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "hospital_id", nullable = false, updatable = false)
    private UUID hospitalId;

    @Column(name = "patient_id", nullable = false, updatable = false)
    private UUID patientId;

    @Column(name = "regimen_id", nullable = false, updatable = false)
    private UUID regimenId;

    @Enumerated(EnumType.STRING)
    @Column(name = "care_setting", nullable = false, updatable = false, length = 20)
    private CareSetting careSetting;

    @Column(name = "due_at", nullable = false, updatable = false)
    private Instant dueAt;

    @Column(name = "window_closes_at", nullable = false, updatable = false)
    private Instant windowClosesAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SlotStatus status;

    @Column(name = "responded_at")
    private Instant respondedAt;

    @Column(name = "recorded_by")
    private UUID recordedBy;

    @Column(name = "reported_by_role", length = 20)
    private String reportedByRole;

    @Column(name = "due_event_published", nullable = false)
    private boolean dueEventPublished;

    @Column(name = "outcome_published", nullable = false)
    private boolean outcomePublished;

    // If the scheduler and a nurse change the same slot at the same moment, the second
    // save fails instead of silently overwriting the first.
    @Version
    private Long version;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public UUID getId() { return id; }
    public UUID getHospitalId() { return hospitalId; }
    public void setHospitalId(UUID hospitalId) { this.hospitalId = hospitalId; }
    public UUID getPatientId() { return patientId; }
    public void setPatientId(UUID patientId) { this.patientId = patientId; }
    public UUID getRegimenId() { return regimenId; }
    public void setRegimenId(UUID regimenId) { this.regimenId = regimenId; }
    public CareSetting getCareSetting() { return careSetting; }
    public void setCareSetting(CareSetting careSetting) { this.careSetting = careSetting; }
    public Instant getDueAt() { return dueAt; }
    public void setDueAt(Instant dueAt) { this.dueAt = dueAt; }
    public Instant getWindowClosesAt() { return windowClosesAt; }
    public void setWindowClosesAt(Instant windowClosesAt) { this.windowClosesAt = windowClosesAt; }
    public SlotStatus getStatus() { return status; }
    public void setStatus(SlotStatus status) { this.status = status; }
    public Instant getRespondedAt() { return respondedAt; }
    public void setRespondedAt(Instant respondedAt) { this.respondedAt = respondedAt; }
    public UUID getRecordedBy() { return recordedBy; }
    public void setRecordedBy(UUID recordedBy) { this.recordedBy = recordedBy; }
    public String getReportedByRole() { return reportedByRole; }
    public void setReportedByRole(String reportedByRole) { this.reportedByRole = reportedByRole; }
    public boolean isDueEventPublished() { return dueEventPublished; }
    public void setDueEventPublished(boolean dueEventPublished) { this.dueEventPublished = dueEventPublished; }
    public boolean isOutcomePublished() { return outcomePublished; }
    public void setOutcomePublished(boolean outcomePublished) { this.outcomePublished = outcomePublished; }
    public Long getVersion() { return version; }
    public Instant getCreatedAt() { return createdAt; }
}
