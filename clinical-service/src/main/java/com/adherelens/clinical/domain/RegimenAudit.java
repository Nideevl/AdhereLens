package com.adherelens.clinical.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "regimen_audit")
public class RegimenAudit {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "change_id", nullable = false, updatable = false)
    private UUID changeId;

    @Column(name = "hospital_id", nullable = false, updatable = false)
    private UUID hospitalId;

    @Column(name = "regimen_id", nullable = false, updatable = false)
    private UUID regimenId;

    @Column(name = "changed_by", nullable = false, updatable = false)
    private UUID changedBy;

    @Column(name = "changed_by_name", updatable = false, length = 200)
    private String changedByName;

    @Column(name = "changed_by_role", nullable = false, updatable = false, length = 20)
    private String changedByRole;

    @Column(name = "changed_at", nullable = false, updatable = false)
    private Instant changedAt;

    @Column(nullable = false, updatable = false, length = 50)
    private String field;

    @Column(name = "old_value", updatable = false, length = 200)
    private String oldValue;

    @Column(name = "new_value", updatable = false, length = 200)
    private String newValue;

    @Column(updatable = false, length = 200)
    private String reason;

    public UUID getId() { return id; }
    public UUID getChangeId() { return changeId; }
    public void setChangeId(UUID changeId) { this.changeId = changeId; }
    public UUID getHospitalId() { return hospitalId; }
    public void setHospitalId(UUID hospitalId) { this.hospitalId = hospitalId; }
    public UUID getRegimenId() { return regimenId; }
    public void setRegimenId(UUID regimenId) { this.regimenId = regimenId; }
    public UUID getChangedBy() { return changedBy; }
    public void setChangedBy(UUID changedBy) { this.changedBy = changedBy; }
    public String getChangedByName() { return changedByName; }
    public void setChangedByName(String changedByName) { this.changedByName = changedByName; }
    public String getChangedByRole() { return changedByRole; }
    public void setChangedByRole(String changedByRole) { this.changedByRole = changedByRole; }
    public Instant getChangedAt() { return changedAt; }
    public void setChangedAt(Instant changedAt) { this.changedAt = changedAt; }
    public String getField() { return field; }
    public void setField(String field) { this.field = field; }
    public String getOldValue() { return oldValue; }
    public void setOldValue(String oldValue) { this.oldValue = oldValue; }
    public String getNewValue() { return newValue; }
    public void setNewValue(String newValue) { this.newValue = newValue; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}
