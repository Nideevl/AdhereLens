package com.adherelens.clinical.service;

import com.adherelens.clinical.domain.DoseSlot;
import com.adherelens.clinical.domain.Patient;
import com.adherelens.clinical.domain.Regimen;
import com.adherelens.clinical.domain.RegimenStatus;
import com.adherelens.clinical.domain.SlotStatus;
import com.adherelens.clinical.repository.DoseSlotRepository;
import com.adherelens.clinical.repository.PatientRepository;
import com.adherelens.clinical.repository.RegimenRepository;
import com.adherelens.common.event.DoseDueEvent;
import com.adherelens.common.event.DoseOutcomeEvent;
import com.adherelens.common.event.Topics;
import com.adherelens.common.model.DoseStatus;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Each public method handles ONE regimen or slot in its own transaction, so one failure never
 * blocks the others. It is a separate bean from DoseScheduler on purpose: @Transactional works
 * through a proxy, and a method calling another method on the same object skips the proxy.
 */
@Service
public class DoseSlotProcessor {

    private final DoseSlotRepository slotRepository;
    private final RegimenRepository regimenRepository;
    private final PatientRepository patientRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final Clock clock;

    public DoseSlotProcessor(DoseSlotRepository slotRepository,
                             RegimenRepository regimenRepository,
                             PatientRepository patientRepository,
                             KafkaTemplate<String, Object> kafkaTemplate,
                             Clock clock) {
        this.slotRepository = slotRepository;
        this.regimenRepository = regimenRepository;
        this.patientRepository = patientRepository;
        this.kafkaTemplate = kafkaTemplate;
        this.clock = clock;
    }

    /** Creates today's slots for one regimen. Safe to call repeatedly. */
    @Transactional
    public void createSlotsIfMissing(UUID regimenId) {
        Regimen regimen = regimenRepository.findById(regimenId).orElse(null);
        if (regimen == null || regimen.getStatus() != RegimenStatus.ACTIVE) {
            return;
        }
        Patient patient = patientRepository.findById(regimen.getPatientId()).orElse(null);
        if (patient == null || !patient.isActive()) {
            return;
        }
        LocalDate today = LocalDate.now(clock);
        if (today.isBefore(regimen.getStartDate())
                || (regimen.getEndDate() != null && today.isAfter(regimen.getEndDate()))) {
            return;
        }
        for (LocalTime time : regimen.getDoseTimes()) {
            Instant dueAt = today.atTime(time).atZone(clock.getZone()).toInstant();
            // A dose time that had already passed when the regimen was prescribed is not a missed dose.
            if (dueAt.isBefore(regimen.getCreatedAt())) {
                continue;
            }
            if (slotRepository.existsByRegimenIdAndDueAt(regimen.getId(), dueAt)) {
                continue;
            }
            DoseSlot slot = new DoseSlot();
            slot.setHospitalId(regimen.getHospitalId());
            slot.setPatientId(regimen.getPatientId());
            slot.setRegimenId(regimen.getId());
            slot.setCareSetting(patient.getCareSetting());
            slot.setDueAt(dueAt);
            slot.setWindowClosesAt(dueAt.plusSeconds(regimen.getGraceWindowMinutes() * 60L));
            slot.setStatus(SlotStatus.PENDING);
            slotRepository.save(slot);
        }
    }

    @Transactional
    public void publishDue(UUID slotId) {
        DoseSlot slot = slotRepository.findById(slotId).orElse(null);
        if (slot == null || slot.isDueEventPublished() || slot.getStatus() == SlotStatus.CANCELLED) {
            return;
        }
        DoseDueEvent event = new DoseDueEvent(slot.getId(), slot.getHospitalId(), slot.getPatientId(),
                slot.getRegimenId(), slot.getCareSetting(), slot.getDueAt(), slot.getWindowClosesAt());
        send(Topics.DOSE_DUE, slot.getPatientId().toString(), event);
        slot.setDueEventPublished(true);
        slotRepository.save(slot);
    }

    /** Turns a PENDING slot into MISSED once its window has closed with no response. */
    @Transactional
    public void markMissedIfExpired(UUID slotId) {
        DoseSlot slot = slotRepository.findById(slotId).orElse(null);
        if (slot == null || slot.getStatus() != SlotStatus.PENDING
                || slot.getWindowClosesAt().isAfter(Instant.now(clock))) {
            return;
        }
        slot.setStatus(SlotStatus.MISSED);
        slotRepository.save(slot);
    }

    @Transactional
    public void publishOutcome(UUID slotId) {
        DoseSlot slot = slotRepository.findById(slotId).orElse(null);
        if (slot == null || slot.isOutcomePublished()) {
            return;
        }
        DoseOutcomeEvent event = new DoseOutcomeEvent(slot.getId(), slot.getHospitalId(), slot.getPatientId(),
                slot.getRegimenId(), slot.getCareSetting(), toDoseStatus(slot.getStatus()),
                slot.getDueAt(), slot.getRespondedAt(), null);
        send(Topics.DOSE_OUTCOME, slot.getPatientId().toString(), event);
        slot.setOutcomePublished(true);
        slotRepository.save(slot);
    }

    private DoseStatus toDoseStatus(SlotStatus status) {
        return switch (status) {
            case HIT -> DoseStatus.HIT;
            case DELAYED -> DoseStatus.DELAYED;
            case MISSED -> DoseStatus.MISSED;
            case REFUSED -> DoseStatus.REFUSED;
            default -> throw new IllegalStateException("No outcome for status " + status);
        };
    }

    /** Waits for the broker to acknowledge, so a slot is only marked published after a real success. */
    private void send(String topic, String key, Object payload) {
        try {
            kafkaTemplate.send(topic, key, payload).get(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while publishing to " + topic, e);
        } catch (ExecutionException | TimeoutException e) {
            throw new IllegalStateException("Kafka publish to " + topic + " failed", e);
        }
    }
}
