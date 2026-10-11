package com.adherelens.clinical.domain;

import com.adherelens.common.model.DoseUnit;
import com.adherelens.common.model.Formulation;
import com.adherelens.common.model.Gender;
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
import java.time.LocalTime;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;

/** A hospital-owned dosing rule. Only ever used to suggest; it never changes a regimen by itself. */
@Entity
@Table(name = "drug_rules")
public class DrugRule {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "hospital_id", nullable = false, updatable = false)
    private UUID hospitalId;

    @Column(name = "drug_name", nullable = false, length = 100)
    private String drugName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Formulation formulation;

    @Column(name = "min_age_years", nullable = false)
    private int minAgeYears;

    @Column(name = "max_age_years", nullable = false)
    private int maxAgeYears;

    @Enumerated(EnumType.STRING)
    @Column(length = 10)
    private Gender gender;

    @Column(name = "min_weight_kg", precision = 5, scale = 2)
    private BigDecimal minWeightKg;

    @Column(name = "max_weight_kg", precision = 5, scale = 2)
    private BigDecimal maxWeightKg;

    @Column(name = "dose_amount", nullable = false, precision = 10, scale = 3)
    private BigDecimal doseAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "dose_unit", nullable = false, length = 10)
    private DoseUnit doseUnit;

    @Column(name = "strength_per_unit", precision = 10, scale = 3)
    private BigDecimal strengthPerUnit;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "drug_rule_dose_times", joinColumns = @JoinColumn(name = "rule_id"))
    @Column(name = "dose_time", nullable = false)
    private Set<LocalTime> doseTimes = new TreeSet<>();

    @Column(name = "created_by", nullable = false, updatable = false)
    private UUID createdBy;

    @Column(nullable = false)
    private boolean active = true;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public UUID getId() { return id; }
    public UUID getHospitalId() { return hospitalId; }
    public void setHospitalId(UUID hospitalId) { this.hospitalId = hospitalId; }
    public String getDrugName() { return drugName; }
    public void setDrugName(String drugName) { this.drugName = drugName; }
    public Formulation getFormulation() { return formulation; }
    public void setFormulation(Formulation formulation) { this.formulation = formulation; }
    public int getMinAgeYears() { return minAgeYears; }
    public void setMinAgeYears(int minAgeYears) { this.minAgeYears = minAgeYears; }
    public int getMaxAgeYears() { return maxAgeYears; }
    public void setMaxAgeYears(int maxAgeYears) { this.maxAgeYears = maxAgeYears; }
    public Gender getGender() { return gender; }
    public void setGender(Gender gender) { this.gender = gender; }
    public BigDecimal getMinWeightKg() { return minWeightKg; }
    public void setMinWeightKg(BigDecimal minWeightKg) { this.minWeightKg = minWeightKg; }
    public BigDecimal getMaxWeightKg() { return maxWeightKg; }
    public void setMaxWeightKg(BigDecimal maxWeightKg) { this.maxWeightKg = maxWeightKg; }
    public BigDecimal getDoseAmount() { return doseAmount; }
    public void setDoseAmount(BigDecimal doseAmount) { this.doseAmount = doseAmount; }
    public DoseUnit getDoseUnit() { return doseUnit; }
    public void setDoseUnit(DoseUnit doseUnit) { this.doseUnit = doseUnit; }
    public BigDecimal getStrengthPerUnit() { return strengthPerUnit; }
    public void setStrengthPerUnit(BigDecimal strengthPerUnit) { this.strengthPerUnit = strengthPerUnit; }
    public Set<LocalTime> getDoseTimes() { return doseTimes; }
    public void setDoseTimes(Set<LocalTime> doseTimes) { this.doseTimes = doseTimes; }
    public UUID getCreatedBy() { return createdBy; }
    public void setCreatedBy(UUID createdBy) { this.createdBy = createdBy; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public Instant getCreatedAt() { return createdAt; }
}
