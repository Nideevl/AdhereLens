package com.adherelens.clinical.domain;

import com.adherelens.common.model.DoseUnit;
import com.adherelens.common.model.Formulation;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;

@Entity
@Table(name = "regimens")
public class Regimen {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "hospital_id", nullable = false, updatable = false)
    private UUID hospitalId;

    @Column(name = "patient_id", nullable = false, updatable = false)
    private UUID patientId;

    @Column(name = "prescribed_by", nullable = false, updatable = false)
    private UUID prescribedBy;

    @Column(name = "drug_name", nullable = false, length = 100)
    private String drugName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Formulation formulation;

    @Column(name = "dose_amount", nullable = false, precision = 10, scale = 3)
    private BigDecimal doseAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "dose_unit", nullable = false, length = 10)
    private DoseUnit doseUnit;

    // Small, always needed with the regimen, so loaded together with it (EAGER).
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "regimen_dose_times", joinColumns = @JoinColumn(name = "regimen_id"))
    @Column(name = "dose_time", nullable = false)
    private Set<LocalTime> doseTimes = new TreeSet<>();

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "grace_window_minutes", nullable = false)
    private int graceWindowMinutes;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RegimenStatus status;

    @Column(name = "discontinued_on")
    private LocalDate discontinuedOn;

    // Doses are only generated for times at or after this instant, so editing a dose time to
    // one that already passed today never creates an instantly-missed dose.
    @Column(name = "schedule_effective_from", nullable = false)
    private Instant scheduleEffectiveFrom;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public UUID getId() { return id; }
    public UUID getHospitalId() { return hospitalId; }
    public void setHospitalId(UUID hospitalId) { this.hospitalId = hospitalId; }
    public UUID getPatientId() { return patientId; }
    public void setPatientId(UUID patientId) { this.patientId = patientId; }
    public UUID getPrescribedBy() { return prescribedBy; }
    public void setPrescribedBy(UUID prescribedBy) { this.prescribedBy = prescribedBy; }
    public String getDrugName() { return drugName; }
    public void setDrugName(String drugName) { this.drugName = drugName; }
    public Formulation getFormulation() { return formulation; }
    public void setFormulation(Formulation formulation) { this.formulation = formulation; }
    public BigDecimal getDoseAmount() { return doseAmount; }
    public void setDoseAmount(BigDecimal doseAmount) { this.doseAmount = doseAmount; }
    public DoseUnit getDoseUnit() { return doseUnit; }
    public void setDoseUnit(DoseUnit doseUnit) { this.doseUnit = doseUnit; }
    public Set<LocalTime> getDoseTimes() { return doseTimes; }
    public void setDoseTimes(Set<LocalTime> doseTimes) { this.doseTimes = doseTimes; }
    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }
    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }
    public int getGraceWindowMinutes() { return graceWindowMinutes; }
    public void setGraceWindowMinutes(int graceWindowMinutes) { this.graceWindowMinutes = graceWindowMinutes; }
    public RegimenStatus getStatus() { return status; }
    public void setStatus(RegimenStatus status) { this.status = status; }
    public LocalDate getDiscontinuedOn() { return discontinuedOn; }
    public void setDiscontinuedOn(LocalDate discontinuedOn) { this.discontinuedOn = discontinuedOn; }
    public Instant getScheduleEffectiveFrom() { return scheduleEffectiveFrom; }
    public void setScheduleEffectiveFrom(Instant scheduleEffectiveFrom) { this.scheduleEffectiveFrom = scheduleEffectiveFrom; }
    public Instant getCreatedAt() { return createdAt; }
}
