package com.adherelens.clinical.service;

import com.adherelens.clinical.domain.Patient;
import com.adherelens.clinical.domain.Regimen;
import com.adherelens.clinical.domain.RegimenStatus;
import com.adherelens.clinical.domain.SlotStatus;
import com.adherelens.clinical.dto.RegimenRequest;
import com.adherelens.clinical.dto.RegimenResponse;
import com.adherelens.clinical.repository.DoseSlotRepository;
import com.adherelens.clinical.repository.PatientRepository;
import com.adherelens.clinical.repository.RegimenRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.TreeSet;
import java.util.UUID;

/** Like PatientService, every method is scoped by the caller's hospitalId from the token. */
@Service
public class RegimenService {

    private static final int DEFAULT_GRACE_MINUTES = 60;

    private final RegimenRepository regimenRepository;
    private final PatientRepository patientRepository;
    private final DoseSlotRepository slotRepository;
    private final Clock clock;

    public RegimenService(RegimenRepository regimenRepository,
                          PatientRepository patientRepository,
                          DoseSlotRepository slotRepository,
                          Clock clock) {
        this.regimenRepository = regimenRepository;
        this.patientRepository = patientRepository;
        this.slotRepository = slotRepository;
        this.clock = clock;
    }

    @Transactional
    public RegimenResponse create(UUID hospitalId, UUID doctorStaffId, UUID patientId, RegimenRequest request) {
        Patient patient = patientRepository.findByIdAndHospitalId(patientId, hospitalId)
                .orElseThrow(() -> new NotFoundException("Patient not found"));
        if (!patient.isActive()) {
            throw new IllegalArgumentException("Patient is not active");
        }
        if (request.endDate() != null && request.endDate().isBefore(request.startDate())) {
            throw new IllegalArgumentException("endDate must not be before startDate");
        }
        TreeSet<LocalTime> times = new TreeSet<>(request.doseTimes());
        if (times.size() != request.doseTimes().size()) {
            throw new IllegalArgumentException("Dose times must be distinct");
        }

        Regimen regimen = new Regimen();
        regimen.setHospitalId(hospitalId);
        regimen.setPatientId(patientId);
        regimen.setPrescribedBy(doctorStaffId);
        regimen.setDrugName(request.drugName());
        regimen.setFormulation(request.formulation());
        regimen.setDoseAmount(request.doseAmount());
        regimen.setDoseUnit(request.doseUnit());
        regimen.setDoseTimes(times);
        regimen.setStartDate(request.startDate());
        regimen.setEndDate(request.endDate());
        regimen.setGraceWindowMinutes(
                request.graceWindowMinutes() == null ? DEFAULT_GRACE_MINUTES : request.graceWindowMinutes());
        regimen.setStatus(RegimenStatus.ACTIVE);
        return toResponse(regimenRepository.save(regimen));
    }

    @Transactional(readOnly = true)
    public List<RegimenResponse> listForPatient(UUID hospitalId, UUID patientId) {
        patientRepository.findByIdAndHospitalId(patientId, hospitalId)
                .orElseThrow(() -> new NotFoundException("Patient not found"));
        return regimenRepository.findByPatientIdAndHospitalIdOrderByStartDateDesc(patientId, hospitalId)
                .stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public RegimenResponse get(UUID hospitalId, UUID regimenId) {
        return toResponse(find(hospitalId, regimenId));
    }

    /** Doses already due keep their normal lifecycle. Doses not yet due are cancelled. */
    @Transactional
    public RegimenResponse discontinue(UUID hospitalId, UUID regimenId) {
        Regimen regimen = find(hospitalId, regimenId);
        if (regimen.getStatus() != RegimenStatus.ACTIVE) {
            throw new IllegalArgumentException("Regimen is not active");
        }
        regimen.setStatus(RegimenStatus.DISCONTINUED);
        regimen.setDiscontinuedOn(LocalDate.now(clock));
        Regimen saved = regimenRepository.save(regimen);
        slotRepository.cancelFuturePending(regimenId, SlotStatus.PENDING, SlotStatus.CANCELLED, Instant.now(clock));
        return toResponse(saved);
    }

    private Regimen find(UUID hospitalId, UUID regimenId) {
        return regimenRepository.findByIdAndHospitalId(regimenId, hospitalId)
                .orElseThrow(() -> new NotFoundException("Regimen not found"));
    }

    private RegimenResponse toResponse(Regimen r) {
        List<LocalTime> times = new TreeSet<>(r.getDoseTimes()).stream().toList();
        return new RegimenResponse(r.getId(), r.getPatientId(), r.getPrescribedBy(), r.getDrugName(),
                r.getFormulation(), r.getDoseAmount(), r.getDoseUnit(), times, times.size(),
                r.getStartDate(), r.getEndDate(), r.getGraceWindowMinutes(), r.getStatus(), r.getDiscontinuedOn());
    }
}
