package com.adherelens.clinical.service;

import com.adherelens.clinical.domain.Patient;
import com.adherelens.clinical.domain.Regimen;
import com.adherelens.clinical.dto.AnalyticsContextResponse;
import com.adherelens.clinical.repository.PatientRepository;
import com.adherelens.clinical.repository.RegimenRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Period;
import java.util.UUID;

/** Decides what leaves the hospital's private data for the analytics layer. */
@Service
public class AnalyticsContextService {

    private final RegimenRepository regimenRepository;
    private final PatientRepository patientRepository;

    public AnalyticsContextService(RegimenRepository regimenRepository, PatientRepository patientRepository) {
        this.regimenRepository = regimenRepository;
        this.patientRepository = patientRepository;
    }

    @Transactional(readOnly = true)
    public AnalyticsContextResponse context(UUID hospitalId, UUID regimenId) {
        Regimen regimen = regimenRepository.findByIdAndHospitalId(regimenId, hospitalId)
                .orElseThrow(() -> new NotFoundException("Regimen not found"));
        Patient patient = patientRepository.findByIdAndHospitalId(regimen.getPatientId(), hospitalId)
                .orElseThrow(() -> new NotFoundException("Patient not found"));
        int age = Math.max(0, Period.between(patient.getDateOfBirth(), regimen.getStartDate()).getYears());
        return new AnalyticsContextResponse(regimen.getId(), regimen.getDrugName(), regimen.getFormulation().name(),
                regimen.getDoseAmount(), regimen.getDoseUnit().name(), regimen.getStartDate(),
                ageBucket(age), patient.getGender().name(), patient.getConditionCode());
    }

    /** 0-9, 10-19 ... 80-89, then 90+. */
    static String ageBucket(int age) {
        if (age >= 90) {
            return "90+";
        }
        int low = age / 10 * 10;
        return low + "-" + (low + 9);
    }
}
