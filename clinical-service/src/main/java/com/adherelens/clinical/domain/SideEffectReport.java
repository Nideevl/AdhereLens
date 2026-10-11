package com.adherelens.clinical.domain;

import com.adherelens.common.model.SideEffect;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/** One side effect reported by a doctor or nurse for a regimen. */
@Entity
@Table(name = "side_effect_reports")
public class SideEffectReport {

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
    @Column(name = "side_effect", nullable = false, updatable = false, length = 40)
    private SideEffect sideEffect;

    @Column(name = "reported_by", nullable = false, updatable = false)
    private UUID reportedBy;

    @Column(name = "reported_by_name", length = 200, updatable = false)
    private String reportedByName;

    @Column(name = "reported_by_role", nullable = false, length = 20, updatable = false)
    private String reportedByRole;

    @Column(name = "reported_at", nullable = false, updatable = false)
    private Instant reportedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SideEffectStatus status = SideEffectStatus.ACTIVE;

    @Column(name = "retracted_by")
    private UUID retractedBy;

    @Column(name = "retracted_by_name", length = 200)
    private String retractedByName;

    @Column(name = "retracted_at")
    private Instant retractedAt;

    @Column(name = "retract_reason", length = 200)
    private String retractReason;

    public UUID getId() { return id; }
    public UUID getHospitalId() { return hospitalId; }
    public void setHospitalId(UUID hospitalId) { this.hospitalId = hospitalId; }
    public UUID getPatientId() { return patientId; }
    public void setPatientId(UUID patientId) { this.patientId = patientId; }
    public UUID getRegimenId() { return regimenId; }
    public void setRegimenId(UUID regimenId) { this.regimenId = regimenId; }
    public SideEffect getSideEffect() { return sideEffect; }
    public void setSideEffect(SideEffect sideEffect) { this.sideEffect = sideEffect; }
    public UUID getReportedBy() { return reportedBy; }
    public void setReportedBy(UUID reportedBy) { this.reportedBy = reportedBy; }
    public String getReportedByName() { return reportedByName; }
    public void setReportedByName(String reportedByName) { this.reportedByName = reportedByName; }
    public String getReportedByRole() { return reportedByRole; }
    public void setReportedByRole(String reportedByRole) { this.reportedByRole = reportedByRole; }
    public Instant getReportedAt() { return reportedAt; }
    public void setReportedAt(Instant reportedAt) { this.reportedAt = reportedAt; }
    public SideEffectStatus getStatus() { return status; }
    public void setStatus(SideEffectStatus status) { this.status = status; }
    public UUID getRetractedBy() { return retractedBy; }
    public void setRetractedBy(UUID retractedBy) { this.retractedBy = retractedBy; }
    public String getRetractedByName() { return retractedByName; }
    public void setRetractedByName(String retractedByName) { this.retractedByName = retractedByName; }
    public Instant getRetractedAt() { return retractedAt; }
    public void setRetractedAt(Instant retractedAt) { this.retractedAt = retractedAt; }
    public String getRetractReason() { return retractReason; }
    public void setRetractReason(String retractReason) { this.retractReason = retractReason; }
}
