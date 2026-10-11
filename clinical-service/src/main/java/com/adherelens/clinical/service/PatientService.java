package com.adherelens.clinical.service;

import com.adherelens.clinical.domain.Patient;
import com.adherelens.clinical.dto.PatientRequest;
import com.adherelens.clinical.dto.PatientResponse;
import com.adherelens.clinical.repository.PatientRepository;
import com.adherelens.common.model.CareSetting;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Every method takes the caller's hospitalId (from the verified token) and uses it
 * in every query, so one hospital can never read or change another hospital's patients.
 */
@Service
public class PatientService {

    private final PatientRepository patientRepository;

    public PatientService(PatientRepository patientRepository) {
        this.patientRepository = patientRepository;
    }

    @Transactional
    public PatientResponse create(UUID hospitalId, PatientRequest request) {
        validateContact(request);
        Patient patient = new Patient();
        patient.setHospitalId(hospitalId);
        apply(patient, request);
        return toResponse(patientRepository.save(patient));
    }

    @Transactional(readOnly = true)
    public List<PatientResponse> list(UUID hospitalId, CareSetting careSetting) {
        List<Patient> patients = careSetting == null
                ? patientRepository.findByHospitalIdOrderByFullName(hospitalId)
                : patientRepository.findByHospitalIdAndCareSettingOrderByFullName(hospitalId, careSetting);
        return patients.stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public PatientResponse get(UUID hospitalId, UUID patientId) {
        return toResponse(find(hospitalId, patientId));
    }

    @Transactional
    public PatientResponse update(UUID hospitalId, UUID patientId, PatientRequest request) {
        validateContact(request);
        Patient patient = find(hospitalId, patientId);
        apply(patient, request);
        return toResponse(patientRepository.save(patient));
    }

    private Patient find(UUID hospitalId, UUID patientId) {
        return patientRepository.findByIdAndHospitalId(patientId, hospitalId)
                .orElseThrow(() -> new NotFoundException("Patient not found"));
    }

    private void validateContact(PatientRequest request) {
        boolean noPhone = isBlank(request.phone()) && isBlank(request.guardianPhone());
        if (request.careSetting() == CareSetting.OUTPATIENT && noPhone) {
            throw new IllegalArgumentException("Outpatients need a phone or a guardian phone");
        }
    }

    private boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    private void apply(Patient patient, PatientRequest r) {
        patient.setFullName(r.fullName().trim());
        patient.setPhone(isBlank(r.phone()) ? null : r.phone().trim());
        patient.setGuardianPhone(isBlank(r.guardianPhone()) ? null : r.guardianPhone().trim());
        patient.setDateOfBirth(r.dateOfBirth());
        patient.setGender(r.gender());
        patient.setCareSetting(r.careSetting());
        patient.setConditionCode(r.conditionCode());
    }

    private PatientResponse toResponse(Patient p) {
        return new PatientResponse(p.getId(), p.getFullName(), p.getPhone(), p.getGuardianPhone(),
                p.getDateOfBirth(), p.getGender(), p.getCareSetting(), p.getConditionCode(), p.isActive());
    }
}
