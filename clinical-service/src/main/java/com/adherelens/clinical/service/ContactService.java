package com.adherelens.clinical.service;

import com.adherelens.clinical.domain.Patient;
import com.adherelens.clinical.dto.ContactResponse;
import com.adherelens.clinical.repository.PatientRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/** Gives notification-service a patient's phone at the moment a reminder is sent. */
@Service
public class ContactService {

    private final PatientRepository patientRepository;

    public ContactService(PatientRepository patientRepository) {
        this.patientRepository = patientRepository;
    }

    @Transactional(readOnly = true)
    public ContactResponse contact(UUID hospitalId, UUID patientId) {
        Patient patient = patientRepository.findByIdAndHospitalId(patientId, hospitalId)
                .filter(Patient::isActive)
                .orElseThrow(() -> new NotFoundException("Patient not found"));
        boolean noOwnPhone = patient.getPhone() == null || patient.getPhone().isBlank();
        return new ContactResponse(patient.getId(), noOwnPhone ? patient.getGuardianPhone() : patient.getPhone());
    }
}
