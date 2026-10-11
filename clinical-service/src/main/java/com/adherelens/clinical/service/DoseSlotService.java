package com.adherelens.clinical.service;

import com.adherelens.auth.AuthUser;
import com.adherelens.clinical.domain.DoseSlot;
import com.adherelens.clinical.domain.Patient;
import com.adherelens.clinical.domain.Regimen;
import com.adherelens.clinical.domain.SlotStatus;
import com.adherelens.clinical.dto.DoseSlotResponse;
import com.adherelens.clinical.dto.RecordOutcome;
import com.adherelens.clinical.repository.DoseSlotRepository;
import com.adherelens.clinical.repository.PatientRepository;
import com.adherelens.clinical.repository.RegimenRepository;
import com.adherelens.common.model.CareSetting;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class DoseSlotService {

    private static final int MAX_ROWS = 200;

    private final DoseSlotRepository slotRepository;
    private final PatientRepository patientRepository;
    private final RegimenRepository regimenRepository;
    private final Clock clock;
    private final long onTimeMinutes;

    public DoseSlotService(DoseSlotRepository slotRepository,
                           PatientRepository patientRepository,
                           RegimenRepository regimenRepository,
                           Clock clock,
                           @Value("${adherelens.dose.on-time-minutes}") long onTimeMinutes) {
        this.slotRepository = slotRepository;
        this.patientRepository = patientRepository;
        this.regimenRepository = regimenRepository;
        this.clock = clock;
        this.onTimeMinutes = onTimeMinutes;
    }

    /** Slots that are already due, oldest first. The default status PENDING is the nurse worklist. */
    @Transactional(readOnly = true)
    public List<DoseSlotResponse> list(UUID hospitalId, SlotStatus status, CareSetting careSetting) {
        List<DoseSlot> slots = slotRepository.findByHospitalIdAndStatusAndDueAtLessThanEqualOrderByDueAt(
                hospitalId, status, Instant.now(clock), PageRequest.of(0, MAX_ROWS));
        if (careSetting != null) {
            slots = slots.stream().filter(s -> s.getCareSetting() == careSetting).toList();
        }
        return toResponses(slots);
    }

    @Transactional
    public DoseSlotResponse record(AuthUser caller, UUID slotId, RecordOutcome outcome) {
        DoseSlot slot = slotRepository.findByIdAndHospitalId(slotId, caller.hospitalId())
                .orElseThrow(() -> new NotFoundException("Dose not found"));
        if (slot.getStatus() != SlotStatus.PENDING) {
            throw new IllegalArgumentException("This dose already has an outcome");
        }
        Instant now = Instant.now(clock);
        if (now.isBefore(slot.getDueAt())) {
            throw new IllegalArgumentException("This dose is not due yet");
        }
        if (!now.isBefore(slot.getWindowClosesAt())) {
            throw new IllegalArgumentException("The window for this dose has closed");
        }

        SlotStatus status;
        if (outcome == RecordOutcome.REFUSED) {
            status = SlotStatus.REFUSED;
        } else if (now.isAfter(slot.getDueAt().plus(onTimeMinutes, ChronoUnit.MINUTES))) {
            status = SlotStatus.DELAYED;
        } else {
            status = SlotStatus.HIT;
        }
        slot.setStatus(status);
        slot.setRespondedAt(now);
        slot.setRecordedBy(caller.staffId());
        slot.setReportedByRole(caller.role().name());
        return toResponses(List.of(slotRepository.save(slot))).get(0);
    }

    private List<DoseSlotResponse> toResponses(List<DoseSlot> slots) {
        Map<UUID, Patient> patients = patientRepository
                .findAllById(slots.stream().map(DoseSlot::getPatientId).distinct().toList())
                .stream().collect(Collectors.toMap(Patient::getId, Function.identity()));
        Map<UUID, Regimen> regimens = regimenRepository
                .findAllById(slots.stream().map(DoseSlot::getRegimenId).distinct().toList())
                .stream().collect(Collectors.toMap(Regimen::getId, Function.identity()));

        return slots.stream().map(s -> {
            Patient p = patients.get(s.getPatientId());
            Regimen r = regimens.get(s.getRegimenId());
            return new DoseSlotResponse(s.getId(), s.getPatientId(), p.getFullName(), r.getDrugName(),
                    r.getFormulation(), r.getDoseAmount(), r.getDoseUnit(), s.getCareSetting(),
                    s.getDueAt(), s.getWindowClosesAt(), s.getStatus(), s.getRespondedAt());
        }).toList();
    }
}
