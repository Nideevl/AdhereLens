package com.adherelens.clinical.service;

import com.adherelens.auth.AuthUser;
import com.adherelens.clinical.domain.DrugRule;
import com.adherelens.clinical.domain.Patient;
import com.adherelens.clinical.dto.DoseSuggestionResponse;
import com.adherelens.clinical.dto.DrugRuleRequest;
import com.adherelens.clinical.dto.DrugRuleResponse;
import com.adherelens.clinical.repository.DrugRuleRepository;
import com.adherelens.clinical.repository.PatientRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.Period;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;
import java.util.UUID;

/**
 * Rules belong to one hospital and every query is scoped by the caller's hospitalId.
 * Rules are only used to suggest. The regimen stores whatever the doctor finally prescribes.
 */
@Service
public class DrugRuleService {

    private final DrugRuleRepository ruleRepository;
    private final PatientRepository patientRepository;
    private final Clock clock;

    public DrugRuleService(DrugRuleRepository ruleRepository, PatientRepository patientRepository, Clock clock) {
        this.ruleRepository = ruleRepository;
        this.patientRepository = patientRepository;
        this.clock = clock;
    }

    @Transactional
    public DrugRuleResponse create(AuthUser caller, DrugRuleRequest r) {
        if (r.minAgeYears() > r.maxAgeYears()) {
            throw new IllegalArgumentException("minAgeYears must not be greater than maxAgeYears");
        }
        if (r.minWeightKg() != null && r.maxWeightKg() != null && r.minWeightKg().compareTo(r.maxWeightKg()) > 0) {
            throw new IllegalArgumentException("minWeightKg must not be greater than maxWeightKg");
        }
        TreeSet<LocalTime> times = new TreeSet<>(r.doseTimes());
        if (times.size() != r.doseTimes().size()) {
            throw new IllegalArgumentException("Dose times must be distinct");
        }

        DrugRule rule = new DrugRule();
        rule.setHospitalId(caller.hospitalId());
        rule.setCreatedBy(caller.staffId());
        rule.setDrugName(r.drugName());
        rule.setFormulation(r.formulation());
        rule.setMinAgeYears(r.minAgeYears());
        rule.setMaxAgeYears(r.maxAgeYears());
        rule.setGender(r.gender());
        rule.setMinWeightKg(r.minWeightKg());
        rule.setMaxWeightKg(r.maxWeightKg());
        rule.setDoseAmount(r.doseAmount());
        rule.setDoseUnit(r.doseUnit());
        rule.setStrengthPerUnit(r.strengthPerUnit());
        rule.setDoseTimes(times);
        return toResponse(ruleRepository.save(rule));
    }

    @Transactional(readOnly = true)
    public List<DrugRuleResponse> list(UUID hospitalId, String drugName) {
        List<DrugRule> rules = (drugName == null || drugName.isBlank())
                ? ruleRepository.findByHospitalIdAndActiveTrueOrderByDrugNameAscMinAgeYearsAsc(hospitalId)
                : ruleRepository.findByHospitalIdAndDrugNameAndActiveTrueOrderByMinAgeYearsAsc(hospitalId, drugName.trim());
        return rules.stream().map(this::toResponse).toList();
    }

    @Transactional
    public DrugRuleResponse deactivate(UUID hospitalId, UUID ruleId) {
        DrugRule rule = ruleRepository.findByIdAndHospitalId(ruleId, hospitalId)
                .orElseThrow(() -> new NotFoundException("Drug rule not found"));
        if (!rule.isActive()) {
            throw new IllegalArgumentException("Rule is already inactive");
        }
        rule.setActive(false);
        return toResponse(ruleRepository.save(rule));
    }

    @Transactional(readOnly = true)
    public DoseSuggestionResponse suggest(UUID hospitalId, UUID patientId, String drugName) {
        Patient patient = patientRepository.findByIdAndHospitalId(patientId, hospitalId)
                .orElseThrow(() -> new NotFoundException("Patient not found"));
        String drug = drugName.trim();
        int age = Period.between(patient.getDateOfBirth(), LocalDate.now(clock)).getYears();
        BigDecimal weight = patient.getWeightKg();

        List<DoseSuggestionResponse.Suggestion> suggestions = new ArrayList<>();
        List<String> notes = new ArrayList<>();
        int skippedForWeight = 0;

        for (DrugRule rule : ruleRepository.findByHospitalIdAndDrugNameAndActiveTrueOrderByMinAgeYearsAsc(hospitalId, drug)) {
            if (age < rule.getMinAgeYears() || age > rule.getMaxAgeYears()) {
                continue;
            }
            if (rule.getGender() != null && rule.getGender() != patient.getGender()) {
                continue;
            }
            boolean needsWeight = rule.getMinWeightKg() != null || rule.getMaxWeightKg() != null;
            if (needsWeight) {
                if (weight == null) {
                    skippedForWeight++;
                    continue;
                }
                if (rule.getMinWeightKg() != null && weight.compareTo(rule.getMinWeightKg()) < 0) {
                    continue;
                }
                if (rule.getMaxWeightKg() != null && weight.compareTo(rule.getMaxWeightKg()) > 0) {
                    continue;
                }
            }
            suggestions.add(toSuggestion(rule));
        }

        if (skippedForWeight > 0) {
            notes.add("Weight missing: " + skippedForWeight
                    + " rule(s) for this drug need the patient's weight, so no suggestion was made from them");
        }
        if (suggestions.isEmpty()) {
            notes.add("No matching rule for this patient. Prescribe manually.");
        } else if (suggestions.size() > 1) {
            notes.add("More than one rule matches. Check the rules and choose one.");
        }
        return new DoseSuggestionResponse(patient.getId(), drug, age, weight, suggestions, notes);
    }

    private DoseSuggestionResponse.Suggestion toSuggestion(DrugRule rule) {
        BigDecimal units = null;
        String label = null;
        if (rule.getStrengthPerUnit() != null) {
            units = rule.getDoseAmount().divide(rule.getStrengthPerUnit(), 3, RoundingMode.HALF_UP);
            label = switch (rule.getFormulation()) {
                case TABLET -> "tablet(s)";
                case CAPSULE -> "capsule(s)";
                case SYRUP -> "ml";
                default -> "unit(s)";
            };
        }
        return new DoseSuggestionResponse.Suggestion(rule.getId(), rule.getFormulation(), rule.getDoseAmount(),
                rule.getDoseUnit(), new ArrayList<>(rule.getDoseTimes()), units, label);
    }

    private DrugRuleResponse toResponse(DrugRule r) {
        return new DrugRuleResponse(r.getId(), r.getDrugName(), r.getFormulation(), r.getMinAgeYears(),
                r.getMaxAgeYears(), r.getGender(), r.getMinWeightKg(), r.getMaxWeightKg(), r.getDoseAmount(),
                r.getDoseUnit(), r.getStrengthPerUnit(), new ArrayList<>(r.getDoseTimes()), r.isActive());
    }
}
