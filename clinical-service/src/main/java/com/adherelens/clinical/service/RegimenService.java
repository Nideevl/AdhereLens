package com.adherelens.clinical.service;

import com.adherelens.auth.AuthUser;
import com.adherelens.clinical.domain.Patient;
import com.adherelens.clinical.domain.Regimen;
import com.adherelens.clinical.domain.RegimenAudit;
import com.adherelens.clinical.domain.RegimenStatus;
import com.adherelens.clinical.domain.SlotStatus;
import com.adherelens.clinical.dto.RegimenChangeResponse;
import com.adherelens.clinical.dto.RegimenRequest;
import com.adherelens.clinical.dto.RegimenResponse;
import com.adherelens.clinical.dto.RegimenUpdateRequest;
import com.adherelens.clinical.repository.DoseSlotRepository;
import com.adherelens.clinical.repository.PatientRepository;
import com.adherelens.clinical.repository.RegimenAuditRepository;
import com.adherelens.clinical.repository.RegimenRepository;
import com.adherelens.common.model.DoseUnit;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Every method is scoped by the caller's hospitalId from the token. Every change to a regimen
 * writes history rows in the same transaction, so a change and its record cannot come apart.
 */
@Service
public class RegimenService {

    private static final int DEFAULT_GRACE_MINUTES = 60;

    private final RegimenRepository regimenRepository;
    private final PatientRepository patientRepository;
    private final DoseSlotRepository slotRepository;
    private final RegimenAuditRepository auditRepository;
    private final Clock clock;

    public RegimenService(RegimenRepository regimenRepository,
                          PatientRepository patientRepository,
                          DoseSlotRepository slotRepository,
                          RegimenAuditRepository auditRepository,
                          Clock clock) {
        this.regimenRepository = regimenRepository;
        this.patientRepository = patientRepository;
        this.slotRepository = slotRepository;
        this.auditRepository = auditRepository;
        this.clock = clock;
    }

    @Transactional
    public RegimenResponse create(AuthUser caller, UUID patientId, RegimenRequest request) {
        Patient patient = patientRepository.findByIdAndHospitalId(patientId, caller.hospitalId())
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

        Instant now = Instant.now(clock);
        Regimen regimen = new Regimen();
        regimen.setHospitalId(caller.hospitalId());
        regimen.setPatientId(patientId);
        regimen.setPrescribedBy(caller.staffId());
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
        regimen.setScheduleEffectiveFrom(now);
        Regimen saved = regimenRepository.save(regimen);

        auditRepository.save(row(caller, saved, UUID.randomUUID(), now, "REGIMEN", null, "created", null));
        return toResponse(saved);
    }

    /**
     * Changes only the fields sent. If the dose times change, doses not yet due are replaced by
     * the new times. Doses already due keep their normal lifecycle.
     */
    @Transactional
    public RegimenResponse update(AuthUser caller, UUID regimenId, RegimenUpdateRequest request) {
        Regimen regimen = find(caller.hospitalId(), regimenId);
        if (regimen.getStatus() != RegimenStatus.ACTIVE) {
            throw new IllegalArgumentException("Only active regimens can be edited");
        }

        Instant now = Instant.now(clock);
        UUID changeId = UUID.randomUUID();
        String reason = (request.reason() == null || request.reason().isBlank()) ? null : request.reason().trim();
        List<RegimenAudit> rows = new ArrayList<>();
        boolean timesChanged = false;

        if (request.doseTimes() != null) {
            TreeSet<LocalTime> newTimes = new TreeSet<>(request.doseTimes());
            if (newTimes.size() != request.doseTimes().size()) {
                throw new IllegalArgumentException("Dose times must be distinct");
            }
            TreeSet<LocalTime> oldTimes = new TreeSet<>(regimen.getDoseTimes());
            if (!newTimes.equals(oldTimes)) {
                rows.add(row(caller, regimen, changeId, now, "DOSE_TIMES", join(oldTimes), join(newTimes), reason));
                regimen.setDoseTimes(newTimes);
                timesChanged = true;
            }
        }

        if (request.doseAmount() != null || request.doseUnit() != null) {
            BigDecimal newAmount = request.doseAmount() != null ? request.doseAmount() : regimen.getDoseAmount();
            DoseUnit newUnit = request.doseUnit() != null ? request.doseUnit() : regimen.getDoseUnit();
            if (newAmount.compareTo(regimen.getDoseAmount()) != 0 || newUnit != regimen.getDoseUnit()) {
                rows.add(row(caller, regimen, changeId, now, "DOSE",
                        formatDose(regimen.getDoseAmount(), regimen.getDoseUnit()),
                        formatDose(newAmount, newUnit), reason));
                regimen.setDoseAmount(newAmount);
                regimen.setDoseUnit(newUnit);
            }
        }

        if (request.graceWindowMinutes() != null
                && request.graceWindowMinutes() != regimen.getGraceWindowMinutes()) {
            rows.add(row(caller, regimen, changeId, now, "GRACE_WINDOW_MINUTES",
                    String.valueOf(regimen.getGraceWindowMinutes()),
                    String.valueOf(request.graceWindowMinutes()), reason));
            regimen.setGraceWindowMinutes(request.graceWindowMinutes());
        }

        if (request.endDate() != null && !request.endDate().equals(regimen.getEndDate())) {
            if (request.endDate().isBefore(regimen.getStartDate())) {
                throw new IllegalArgumentException("endDate must not be before startDate");
            }
            rows.add(row(caller, regimen, changeId, now, "END_DATE",
                    regimen.getEndDate() == null ? "none" : regimen.getEndDate().toString(),
                    request.endDate().toString(), reason));
            regimen.setEndDate(request.endDate());
        }

        if (rows.isEmpty()) {
            throw new IllegalArgumentException("Nothing to change");
        }

        if (timesChanged) {
            regimen.setScheduleEffectiveFrom(now);
            slotRepository.deleteFuturePending(regimen.getId(), SlotStatus.PENDING, now);
        }
        Regimen saved = regimenRepository.save(regimen);
        auditRepository.saveAll(rows);
        return toResponse(saved);
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

    @Transactional(readOnly = true)
    public List<RegimenChangeResponse> history(UUID hospitalId, UUID regimenId) {
        find(hospitalId, regimenId);
        return auditRepository.findByRegimenIdAndHospitalIdOrderByChangedAtDesc(regimenId, hospitalId)
                .stream()
                .map(a -> new RegimenChangeResponse(a.getChangeId(), a.getChangedAt(), a.getChangedBy(),
                        a.getChangedByName(), a.getChangedByRole(), a.getField(),
                        a.getOldValue(), a.getNewValue(), a.getReason()))
                .toList();
    }

    /** Doses already due keep their normal lifecycle. Doses not yet due are cancelled. */
    @Transactional
    public RegimenResponse discontinue(AuthUser caller, UUID regimenId) {
        Regimen regimen = find(caller.hospitalId(), regimenId);
        if (regimen.getStatus() != RegimenStatus.ACTIVE) {
            throw new IllegalArgumentException("Regimen is not active");
        }
        Instant now = Instant.now(clock);
        regimen.setStatus(RegimenStatus.DISCONTINUED);
        regimen.setDiscontinuedOn(LocalDate.now(clock));
        Regimen saved = regimenRepository.save(regimen);
        slotRepository.cancelFuturePending(regimenId, SlotStatus.PENDING, SlotStatus.CANCELLED, now);
        auditRepository.save(row(caller, saved, UUID.randomUUID(), now, "STATUS", "ACTIVE", "DISCONTINUED", null));
        return toResponse(saved);
    }

    private Regimen find(UUID hospitalId, UUID regimenId) {
        return regimenRepository.findByIdAndHospitalId(regimenId, hospitalId)
                .orElseThrow(() -> new NotFoundException("Regimen not found"));
    }

    private RegimenAudit row(AuthUser caller, Regimen regimen, UUID changeId, Instant at,
                             String field, String oldValue, String newValue, String reason) {
        RegimenAudit a = new RegimenAudit();
        a.setChangeId(changeId);
        a.setHospitalId(regimen.getHospitalId());
        a.setRegimenId(regimen.getId());
        a.setChangedBy(caller.staffId());
        a.setChangedByName(caller.fullName());
        a.setChangedByRole(caller.role().name());
        a.setChangedAt(at);
        a.setField(field);
        a.setOldValue(oldValue);
        a.setNewValue(newValue);
        a.setReason(reason);
        return a;
    }

    private String join(TreeSet<LocalTime> times) {
        return times.stream().map(t -> t.toString()).collect(Collectors.joining(","));
    }

    private String formatDose(BigDecimal amount, DoseUnit unit) {
        return amount.stripTrailingZeros().toPlainString() + " " + unit;
    }

    private RegimenResponse toResponse(Regimen r) {
        List<LocalTime> times = new TreeSet<>(r.getDoseTimes()).stream().toList();
        return new RegimenResponse(r.getId(), r.getPatientId(), r.getPrescribedBy(), r.getDrugName(),
                r.getFormulation(), r.getDoseAmount(), r.getDoseUnit(), times, times.size(),
                r.getStartDate(), r.getEndDate(), r.getGraceWindowMinutes(), r.getStatus(), r.getDiscontinuedOn());
    }
}
